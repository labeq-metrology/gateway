package com.labeq.communication

import com.labeq.device.generic.GenericDevice
import com.labeq.transport.*
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTPublish
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
sealed class DeviceRequest(val requestId: Uuid) {
    data class ParameterReadRequest(
        val id: Uuid,
        val requestedParameter: String,
        val returnType: ReturnType = ReturnType.String
    ) : DeviceRequest(id)

    data class ParameterWriteRequest(val id: Uuid, val requestedParameter: String, val value: ParameterValue) :
        DeviceRequest(id)
}

@Serializable
@OptIn(ExperimentalUuidApi::class)
sealed class RequestDTO {
    @Serializable
    data class ReadRequestDTO(val requestId: Uuid, val returnType: String) : RequestDTO()

    @Serializable
    data class WriteRequestDTO(val requestId: Uuid, val value: String, val contentType: String) : RequestDTO()
}

@OptIn(ExperimentalUuidApi::class, ExperimentalUnsignedTypes::class)
class RequestHandler @OptIn(DelicateCoroutinesApi::class) constructor(
    var devices: MutableMap<Uuid, GenericDevice>,
    private val eventBus: MQTTEventBus,
    private val scope: CoroutineScope = CoroutineScope(Executors.newFixedThreadPool(10).asCoroutineDispatcher())
) {
    private var deviceQueues: HashMap<Uuid, LinkedBlockingQueue<DeviceRequest>> = hashMapOf()

    private fun beginDeviceQueueProcessing(device: GenericDevice) {
        if (!deviceQueues.containsKey(device.uuid)) return
        val queue = deviceQueues[device.uuid]!!
        scope.launch {
            while (true) {
                when (val request = queue.take()) {
                    is DeviceRequest.ParameterReadRequest -> {
                        val readResult = if (request.requestedParameter == "*") {
                            if (device.readState()) MessageResult.Result(ParameterValue.StringValue("OK")) else MessageResult.Error(
                                "Error while reading device state"
                            )
                        } else {
                            device.readParameter(request.requestedParameter, request.returnType)
                        }
                        readResult
                            .onFailure {
                                eventBus.publishResolveError(
                                    request.requestId,
                                    device,
                                    request.requestedParameter,
                                    "Failed to read parameter"
                                )
                            }
                            .onSuccess {
                                device.parameters[request.requestedParameter] = it
                                eventBus.publishParameterResolve(
                                    request.requestId,
                                    device,
                                    request.requestedParameter,
                                    it
                                )
                                eventBus.publishParameterUpdate(
                                    device,
                                    request.requestedParameter,
                                    it
                                )
                            }
                    }

                    is DeviceRequest.ParameterWriteRequest -> {
                        when (val result = device.setParameter(request.requestedParameter, request.value)) {
                            is MessageResult.Result -> continue
                            is MessageResult.Error -> eventBus.publishResolveError(
                                request.requestId,
                                device,
                                request.requestedParameter,
                                result.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun add(deviceId: Uuid, request: DeviceRequest) {
        if (!deviceQueues.containsKey(deviceId)) {
            deviceQueues[deviceId] = LinkedBlockingQueue<DeviceRequest>().apply { add(request) }
            devices[deviceId]?.let { beginDeviceQueueProcessing(it) }
        } else {
            deviceQueues[deviceId]?.add(request)
        }
    }

    fun cancelRequest(deviceId: Uuid, requestId: Uuid): Boolean {
        return deviceQueues[deviceId]?.removeIf { it.requestId == requestId } ?: false
    }

    fun handlePublish(packet: MQTTPublish) {
        val readRequestRegex = MQTTEventBus.DEVICE_PARAMETER_READ_TOPIC
            .replace("{uuid}", "(?<uuid>[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})")
            .replace("{parameter}", "(?<parameter>[^/]+)")
            .toRegex()

        val writeRequestRegex = MQTTEventBus.DEVICE_PARAMETER_WRITE_TOPIC
            .replace("{uuid}", "(?<uuid>[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})")
            .replace("{parameter}", "(?<parameter>[^/]+)")
            .toRegex()

        if (readRequestRegex matches packet.topicName) {
            val matched = readRequestRegex.matchEntire(packet.topicName) ?: return

            val deviceUuid = Uuid.parse(matched.groupValues[1])
            val parameter = matched.groupValues[2]
            if (!devices.containsKey(deviceUuid)) return
            Json.decodeFromString<RequestDTO.ReadRequestDTO>(
                String(
                    packet.payload?.toByteArray() ?: byteArrayOf()
                )
            )
            val requestId = try {
                Json.decodeFromString<RequestDTO.ReadRequestDTO>(
                    String(
                        packet.payload?.toByteArray() ?: byteArrayOf()
                    )
                ).requestId
            } catch (_: SerializationException) {
                Uuid.random()
            }

            add(
                deviceUuid,
                DeviceRequest.ParameterReadRequest(requestId, parameter)
            )
        } else if (writeRequestRegex matches packet.topicName) {
            val matched = writeRequestRegex.matchEntire(packet.topicName) ?: return
            val deviceUuid = Uuid.parse(matched.groupValues[1])
            val parameter = matched.groupValues[2]
            if (!devices.containsKey(deviceUuid)) return

            val payloadString = String(packet.payload?.toByteArray() ?: byteArrayOf());

            val dto = try {
                Json.decodeFromString<RequestDTO.WriteRequestDTO>(payloadString)
            } catch (_: Exception) {
                return
            }

            val parameterValueResult = parseWriteRequestDTO(
                payloadString,
                dto.contentType
            )

            parameterValueResult.onSuccess { paramValue ->
                add(deviceUuid, DeviceRequest.ParameterWriteRequest(dto.requestId, parameter, paramValue))
            }.onFailure {
                eventBus.publishResolveError(dto.requestId, devices[deviceUuid]!!, parameter, it)
            }
        }
    }

    private fun parseWriteRequestDTO(payload: String, contentType: String?): MessageResult<ParameterValue> {
        return try {
            val json = Json.parseToJsonElement(payload).jsonObject
            val valueString = json["value"]?.jsonPrimitive?.content
                ?: return MessageResult.Error("Missing 'value' field in write request")

            when (contentType) {
                "labeq/string" -> MessageResult.Result(ParameterValue.StringValue(valueString))

                "labeq/integer" -> {
                    valueString.toIntOrNull()?.let { intVal ->
                        MessageResult.Result(ParameterValue.IntegerValue(intVal))
                    } ?: MessageResult.Error("Invalid integer value: $valueString")
                }

                "labeq/bytes" -> {
                    try {
                        val decoded = java.util.Base64.getDecoder().decode(valueString)
                        MessageResult.Result(ParameterValue.BytesValue(decoded))
                    } catch (e: IllegalArgumentException) {
                        MessageResult.Error("Invalid base64 string: ${e.message}")
                    }
                }

                else -> MessageResult.Error("Unsupported content type: $contentType")
            }
        } catch (e: Exception) {
            MessageResult.Error("Failed to parse payload: ${e.message}")
        }
    }
}
