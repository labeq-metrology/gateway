package com.labeq.communication

import com.labeq.device.generic.GenericDevice
import com.labeq.transport.ParameterValue
import com.labeq.transport.ReturnType
import com.labeq.transport.onFailure
import com.labeq.transport.onSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
sealed class DeviceRequest(val requestId: Uuid) {
    data class ParameterReadRequest(
        val id: Uuid,
        val requestedParameter: String,
        val returnType: ReturnType = ReturnType.STRING
    ) : DeviceRequest(id)

    data class ParameterWriteRequest(val id: Uuid, val requestedParameter: String, val value: ParameterValue) :
        DeviceRequest(id)
}

@OptIn(ExperimentalUuidApi::class)
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
                val request = queue.take()
                when (request) {
                    is DeviceRequest.ParameterReadRequest -> {
                        val readResult = device.readParameter(request.requestedParameter, request.returnType)

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
                                eventBus.publishParameterResolve(
                                    request.requestId,
                                    device,
                                    request.requestedParameter,
                                    it
                                )
                            }
                    }

                    is DeviceRequest.ParameterWriteRequest -> {
                        device.setParameter(request.requestedParameter, request.value)
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
}
