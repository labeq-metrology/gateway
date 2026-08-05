package com.labeq.script.rpc.service

import Parameter
import ParameterServiceGrpcKt
import com.labeq.communication.RequestHandler
import com.labeq.device.GenericDevice
import com.labeq.transport.ParameterValue
import com.labeq.transport.ReturnType
import com.labeq.transport.onFailure
import com.labeq.transport.onSuccess
import io.grpc.Status
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ParameterService(val devices: MutableMap<Uuid, GenericDevice>) :
    ParameterServiceGrpcKt.ParameterServiceCoroutineImplBase() {
    override suspend fun readParameter(request: Parameter.ParameterReadRequest): Parameter.ParameterReadResponse {
        val uuid = try {
            Uuid.parse(request.device)
        } catch (e: IllegalArgumentException) {
            throw Status.INVALID_ARGUMENT
                .withDescription("Malformed UUID format: ${request.device}")
                .asException()
        }

        val device = try {
            checkNotNull(devices[uuid])
        } catch (e: Exception) {
            throw Status.INVALID_ARGUMENT
                .withDescription("The device does not exist")
                .asException()
        }

        val readRequest = device.readParameter(request.parameter, ReturnType.String)

        readRequest.onSuccess {
            it as ParameterValue.StringValue
            return Parameter.ParameterReadResponse.newBuilder()
                .setValue(it.value)
                .build()
        }.onFailure {
            throw Status.INTERNAL
                .withDescription(it)
                .asException()
        }

        throw Status.INTERNAL
            .withDescription("Unknown error")
            .asException()
    }

    override suspend fun writeParameter(request: Parameter.ParameterWriteRequest): Parameter.ParameterWriteResponse {
        val uuid = try {
            Uuid.parse(request.device)
        } catch (e: IllegalArgumentException) {
            throw Status.INVALID_ARGUMENT
                .withDescription("Malformed UUID format: ${request.device}")
                .asException()
        }

        val device = try {
            checkNotNull(devices[uuid])
        } catch (e: Exception) {
            throw Status.INVALID_ARGUMENT
                .withDescription("The device does not exist")
                .asException()
        }

        device.setParameter(request.parameter, ParameterValue.StringValue(request.value))
            .onSuccess {
                it as ParameterValue.StringValue
                return Parameter.ParameterWriteResponse.newBuilder()
                    .setValue(it.value)
                    .setMessage("")
                    .setSuccess(true)
                    .build()
            }
            .onFailure {
                return Parameter.ParameterWriteResponse.newBuilder()
                    .setValue("")
                    .setMessage(it)
                    .setSuccess(false)
                    .build()
            }

        throw Status.INTERNAL
            .withDescription("Unknown error")
            .asException()
    }
}