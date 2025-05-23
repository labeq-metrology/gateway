package com.labeq.device.generic

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class ParameterSetter(val identifier: String)

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class ParameterGetter(val identifier: String)

sealed class DeviceParameter<T>(open val value: T)

/**
 * Represents a device parameter that holds raw binary data.
 */
data class BinaryParameter(override val value: ByteArray) : DeviceParameter<ByteArray>(value) {
    override fun equals(other: Any?): Boolean {
        return other is BinaryParameter && value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }
}

/**
 * Represents a device parameter that holds an integer value.
 */
data class IntegerParameter(override val value: Int) : DeviceParameter<Int>(value)

/**
 * Represents a device parameter that holds a textual value.
 */
data class StringParameter(override var value: String) : DeviceParameter<String>(value)
