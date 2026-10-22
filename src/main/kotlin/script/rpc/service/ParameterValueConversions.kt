package com.labeq.script.rpc.service

import com.google.protobuf.ByteString
import com.labeq.transport.ParameterValue
import io.grpc.Status

internal fun ParameterValue.toProto(): Parameter.ParameterValue {
    val builder = Parameter.ParameterValue.newBuilder()
    when (this) {
        is ParameterValue.StringValue -> builder.stringValue = value
        is ParameterValue.IntegerValue -> builder.integerValue = value
        is ParameterValue.DoubleValue -> builder.doubleValue = value
        is ParameterValue.BytesValue -> builder.bytesValue = ByteString.copyFrom(value)
        is ParameterValue.IntegerArrayValue -> builder.integerArrayValue =
            Parameter.IntegerArray.newBuilder().addAllValues(value.toList()).build()
        is ParameterValue.DoubleArrayValue -> builder.doubleArrayValue =
            Parameter.DoubleArray.newBuilder().addAllValues(value.toList()).build()
    }
    return builder.build()
}

internal fun Parameter.ParameterValue.toModel(): ParameterValue =
    when (valueTypeCase) {
        Parameter.ParameterValue.ValueTypeCase.STRING_VALUE -> ParameterValue.StringValue(stringValue)
        Parameter.ParameterValue.ValueTypeCase.INTEGER_VALUE -> ParameterValue.IntegerValue(integerValue)
        Parameter.ParameterValue.ValueTypeCase.DOUBLE_VALUE -> ParameterValue.DoubleValue(doubleValue)
        Parameter.ParameterValue.ValueTypeCase.BYTES_VALUE -> ParameterValue.BytesValue(bytesValue.toByteArray())
        Parameter.ParameterValue.ValueTypeCase.INTEGER_ARRAY_VALUE ->
            ParameterValue.IntegerArrayValue(integerArrayValue.valuesList.toIntArray())
        Parameter.ParameterValue.ValueTypeCase.DOUBLE_ARRAY_VALUE ->
            ParameterValue.DoubleArrayValue(doubleArrayValue.valuesList.toDoubleArray())
        Parameter.ParameterValue.ValueTypeCase.VALUETYPE_NOT_SET, null ->
            throw Status.INVALID_ARGUMENT
                .withDescription("Parameter value is missing or unknown type")
                .asException()
    }
