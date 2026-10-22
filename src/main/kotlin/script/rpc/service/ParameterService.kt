package com.labeq.script.rpc.service

import Parameter
import ParameterServiceGrpcKt
import com.labeq.device.GenericDevice
import com.labeq.transport.ReturnType
import com.labeq.transport.onFailure
import com.labeq.transport.onSuccess
import io.grpc.Status
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ParameterService(val devices: MutableMap<Uuid, GenericDevice>) :
    ParameterServiceGrpcKt.ParameterServiceCoroutineImplBase() {

    override suspend fun readParameter(request: Parameter.ParameterReadRequest): Parameter.ParameterReadResponse =
        respond {
                val device = getDeviceOrThrow(request.device)
                val parameter = requireNonBlank(request.parameter, "parameter")
                device.readParameter(parameter, ReturnType.SameAsInput)
                    .onSuccess { value ->
                        return@respond Parameter.ParameterReadResponse.newBuilder()
                            .setValue(value.toProto())
                            .build()
                    }
                    .onFailure { error ->
                        return@respond Parameter.ParameterReadResponse.newBuilder()
                            .setError(error)
                            .build()
                    }
                    throw Status.INTERNAL.withDescription("Unknown error").asException()
            }

    override suspend fun writeParameter(request: Parameter.ParameterWriteRequest): Parameter.ParameterWriteResponse =
        respond {
                val device = getDeviceOrThrow(request.device)
                val parameter = requireNonBlank(request.parameter, "parameter")
                device.setParameter(parameter, request.value.toModel())
                    .onSuccess { value ->
                        return@respond Parameter.ParameterWriteResponse.newBuilder()
                            .setValue(value.toProto())
                            .setMessage("")
                            .setSuccess(true)
                            .build()
                    }
                    .onFailure { error ->
                        return@respond Parameter.ParameterWriteResponse.newBuilder()
                            .setMessage(error)
                            .setSuccess(false)
                            .build()
                    }
                    throw Status.INTERNAL.withDescription("Unknown error").asException()
            }

    private fun getDeviceOrThrow(deviceUuid: String): GenericDevice {
        val uuid = parseUuid(deviceUuid, "device")

        return devices[uuid] ?: throw Status.NOT_FOUND
            .withDescription("The device does not exist")
            .asException()
    }
}