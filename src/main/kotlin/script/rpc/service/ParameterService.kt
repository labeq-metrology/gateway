package com.labeq.script.rpc.service

import Parameter
import ParameterServiceGrpcKt
import com.google.protobuf.ByteString
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
        val device = getDeviceOrThrow(request.device)

        val readResult = device.readParameter(request.parameter, ReturnType.SameAsInput)

        readResult.onSuccess { paramValue ->
            return Parameter.ParameterReadResponse.newBuilder()
                .setValue(paramValue.toProto())
                .build()
        }.onFailure { errorMsg ->
            return Parameter.ParameterReadResponse.newBuilder()
                .setError(errorMsg)
                .build()
        }

        throw Status.INTERNAL.withDescription("Unknown error").asException()
    }

    override suspend fun writeParameter(request: Parameter.ParameterWriteRequest): Parameter.ParameterWriteResponse {
        val device = getDeviceOrThrow(request.device)

        val kotlinValue = request.value.toModel()

        device.setParameter(request.parameter, kotlinValue)
            .onSuccess { writtenValue ->
                return Parameter.ParameterWriteResponse.newBuilder()
                    .setValue(writtenValue.toProto())
                    .setMessage("")
                    .setSuccess(true)
                    .build()
            }
            .onFailure { errorMsg ->
                return Parameter.ParameterWriteResponse.newBuilder()
                    .setMessage(errorMsg)
                    .setSuccess(false)
                    .build()
            }

        throw Status.INTERNAL.withDescription("Unknown error").asException()
    }

    private fun getDeviceOrThrow(deviceUuid: String): GenericDevice {
        val uuid = try {
            Uuid.parse(deviceUuid)
        } catch (e: IllegalArgumentException) {
            throw Status.INVALID_ARGUMENT
                .withDescription("Malformed UUID format: $deviceUuid")
                .asException()
        }

        return devices[uuid] ?: throw Status.INVALID_ARGUMENT
            .withDescription("The device does not exist")
            .asException()
    }
}

/**
 * Extension to map Kotlin ParameterValue to Protobuf ParameterValue
 */
fun ParameterValue.toProto(): Parameter.ParameterValue {
    val builder = Parameter.ParameterValue.newBuilder()
    when (this) {
        is ParameterValue.StringValue -> builder.stringValue = this.value
        is ParameterValue.IntegerValue -> builder.integerValue = this.value
        is ParameterValue.DoubleValue -> builder.doubleValue = this.value
        is ParameterValue.BytesValue -> builder.bytesValue = ByteString.copyFrom(this.value)
        is ParameterValue.IntegerArrayValue -> {
            builder.integerArrayValue = Parameter.IntegerArray.newBuilder()
                .addAllValues(this.value.toList())
                .build()
        }
        is ParameterValue.DoubleArrayValue -> {
            builder.doubleArrayValue = Parameter.DoubleArray.newBuilder()
                .addAllValues(this.value.toList())
                .build()
        }
    }
    return builder.build()
}

/**
 * Extension to map Protobuf ParameterValue to Kotlin ParameterValue
 */
fun Parameter.ParameterValue.toModel(): ParameterValue {
    return when (this.valueTypeCase) {
        Parameter.ParameterValue.ValueTypeCase.STRING_VALUE -> ParameterValue.StringValue(this.stringValue)
        Parameter.ParameterValue.ValueTypeCase.INTEGER_VALUE -> ParameterValue.IntegerValue(this.integerValue)
        Parameter.ParameterValue.ValueTypeCase.DOUBLE_VALUE -> ParameterValue.DoubleValue(this.doubleValue)
        Parameter.ParameterValue.ValueTypeCase.BYTES_VALUE -> ParameterValue.BytesValue(this.bytesValue.toByteArray())
        Parameter.ParameterValue.ValueTypeCase.INTEGER_ARRAY_VALUE -> ParameterValue.IntegerArrayValue(this.integerArrayValue.valuesList.toIntArray())
        Parameter.ParameterValue.ValueTypeCase.DOUBLE_ARRAY_VALUE -> ParameterValue.DoubleArrayValue(this.doubleArrayValue.valuesList.toDoubleArray())
        Parameter.ParameterValue.ValueTypeCase.VALUETYPE_NOT_SET, null -> throw Status.INVALID_ARGUMENT
            .withDescription("Parameter value is missing or unknown type")
            .asException()
    }
}