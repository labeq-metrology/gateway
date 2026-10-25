package com.labeq.communication

import com.labeq.device.GenericDevice
import com.labeq.transport.*
import com.labeq.transport.MessageResult.*
import com.labeq.transport.ParameterValue.*
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTPublish
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
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
    data class WriteRequestDTO(val requestId: Uuid, val value: ParameterValue) : RequestDTO()
}

@OptIn(ExperimentalUuidApi::class, ExperimentalUnsignedTypes::class)
class RequestHandler @OptIn(DelicateCoroutinesApi::class) constructor(
    var devices: MutableMap<Uuid, GenericDevice>,
    private val eventBus: MQTTEventBus,
    private val scope: CoroutineScope = CoroutineScope(Executors.newFixedThreadPool(10).asCoroutineDispatcher())
) {
    private var deviceQueues: HashMap<Uuid, LinkedBlockingQueue<DeviceRequest>> = hashMapOf()

    private fun beginDeviceQueueProcessing(device: GenericDevice) {
        if (!deviceQueues.containsKey(device.metadata.uuid)) return
        val queue = deviceQueues[device.metadata.uuid]
        scope.launch {
            while (true) {
                when (val request = queue?.take()) {
                    is DeviceRequest.ParameterReadRequest -> {
                        val readResult = if (request.requestedParameter == "*") {
                            if (device.readState()) Result(StringValue("OK")) else Error(
                                "Error while reading device state"
                            )
                        } else {
                            device.readParameter(request.requestedParameter, request.returnType)
                        }
                        notifyParameterChange(device, request, readResult)
                    }

                    is DeviceRequest.ParameterWriteRequest -> {
                        notifyParameterChange(device, request, device.setParameter(request.requestedParameter, request.value))
                    }

                    null -> break
                }
            }
        }
    }

    private fun notifyParameterChange(device: GenericDevice, request: DeviceRequest, result: MessageResult<ParameterValue>) {
        val parameter = when (request) {
            is DeviceRequest.ParameterReadRequest -> request.requestedParameter
            is DeviceRequest.ParameterWriteRequest -> request.requestedParameter
            else -> return // Future proofing
        }

        result
            .onFailure {
                eventBus.publishResolveError(
                    request.requestId,
                    device,
                    parameter,
                    it
                )
            }
            .onSuccess {
                device.parameters[parameter] = it
                eventBus.publishParameterResolve(
                    request.requestId,
                    device,
                    parameter,
                    it
                )
                eventBus.publishParameterUpdate(
                    device,
                    parameter,
                    device.parameters[parameter]!!
                )
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

    private val writeJson = Json { classDiscriminator = "paramType" }
    fun handlePublish(packet: MQTTPublish) {
        val readRequestRegex = MQTTEventBus.DEVICE_PARAMETER_READ_TOPIC
            .replace("{uuid}", "(?<uuid>[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})")
            .replace("{parameter}", "(?<parameter>[^/]+)")
            .toRegex()

        val writeRequestRegex = MQTTEventBus.DEVICE_PARAMETER_WRITE_TOPIC
            .replace("{uuid}", "(?<uuid>[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})")
            .replace("{parameter}", "(?<parameter>[^/]+)")
            .toRegex()

        println(writeRequestRegex matches packet.topicName)
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
        }

        if (writeRequestRegex matches packet.topicName) {
            val matched = writeRequestRegex.matchEntire(packet.topicName) ?: return
            val deviceUuid = Uuid.parse(matched.groupValues[1])
            val parameter = matched.groupValues[2]
            if (!devices.containsKey(deviceUuid)) return

            val payloadString = String(packet.payload?.toByteArray() ?: byteArrayOf())

            val dto = try {
                writeJson.decodeFromString<RequestDTO.WriteRequestDTO>(payloadString)
            } catch (_: Exception) {
                return
            }

            add(deviceUuid, DeviceRequest.ParameterWriteRequest(dto.requestId, parameter, dto.value))
        }
    }
}
