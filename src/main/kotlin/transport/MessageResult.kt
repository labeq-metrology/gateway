package com.labeq.transport

/**
 * Represents the result of an operation, encapsulating either a successful value of type `T`
 * or an error message.
 *
 * This sealed interface is designed to standardize how results and errors are handled
 * across various operations, ensuring a consistent approach to managing both success and failure scenarios.
 *
 * @param T The type of the successful result, if applicable.
 */
sealed interface MessageResult<out T> {
    /**
     * A data class representing a successful result within the context of a [MessageResult].
     *
     * The [Result] class encapsulates a value of type `T`, indicating that an operation
     * has been completed successfully and has produced a result.
     *
     * @param T The type of the successful result encapsulated by this instance.
     * @property result The value of type `T` representing the successful outcome.
     */
    data class Result<out T>(val result: T) : MessageResult<T>

    /**
     * Represents an error result within the [MessageResult] context.
     *
     * This class encapsulates an error message as a [String], indicating that an
     * operation has failed. It is used to standardize error reporting within the
     * result-handling system and provides a consistent way to access error details.
     *
     * @property error The error message describing the nature of the failure.
     */
    data class Error(val error: String) : MessageResult<Nothing>
}

/**
 * Determines whether the current [MessageResult] instance represents a successful result.
 *
 * @return `true` if the instance is of type [MessageResult.Result], indicating a successful result;
 *         `false` if the instance is of type [MessageResult.Error], indicating an error.
 */
fun <T> MessageResult<T>.isSuccess(): Boolean = this is MessageResult.Result

/**
 * Checks if the current `MessageResult` instance represents a failure.
 *
 * This method determines whether the result is of type `MessageResult.Error`,
 * indicating that an error occurred during an operation.
 *
 * @return `true` if the result is a failure (i.e., `MessageResult.Error`), otherwise `false`.
 */
fun <T> MessageResult<T>.isFailure(): Boolean = this is MessageResult.Error

/**
 * Retrieves the result from a [MessageResult], or returns `null` if the result represents an error.
 *
 * @return The result value of type `T` if this is a [MessageResult.Result], or `null` if this is a [MessageResult.Error].
 */
fun <T> MessageResult<T>.getOrNull(): T? = when (this) {
    is MessageResult.Result -> result
    is MessageResult.Error -> null
}

/**
 * Returns the error message contained in the [MessageResult] if it is of type [MessageResult.Error],
 * or `null` if it is of type [MessageResult.Result].
 *
 * @return A nullable [String] containing the error message if the result is an error, or `null` otherwise.
 */
fun <T> MessageResult<T>.errorOrNull(): String? = when (this) {
    is MessageResult.Result -> null
    is MessageResult.Error -> error
}

/**
 * Executes the provided action if the [MessageResult] instance represents a successful result.
 *
 * This function checks if the current [MessageResult] is of type [MessageResult.Result].
 * If so, the provided `action` is invoked with the encapsulated successful result.
 * The original [MessageResult] is returned to allow for chaining operations.
 *
 * @param action A lambda function to be invoked with the value of the successful result.
 * @return The original [MessageResult] instance for chaining operations.
 */
suspend inline fun <T> MessageResult<T>.onSuccessAsync(action: suspend (T) -> Unit): MessageResult<T> {
    if (this is MessageResult.Result) action(result)
    return this
}

/**
 * Executes the given action if the current instance is of type [MessageResult.Error],
 * passing the error message as a parameter to the action. This can be used to handle
 * error scenarios in a streamlined manner.
 *
 * @param action A lambda function that receives an error message as a [String] and performs some processing.
 * @return The same [MessageResult] instance, allowing for method chaining.
 */
suspend inline fun <T> MessageResult<T>.onFailureAsync(action: suspend (String) -> Unit): MessageResult<T> {
    if (this is MessageResult.Error) action(error)
    return this
}

/**
 * Executes the provided action if the [MessageResult] instance represents a successful result.
 *
 * This function checks if the current [MessageResult] is of type [MessageResult.Result].
 * If so, the provided `action` is invoked with the encapsulated successful result.
 * The original [MessageResult] is returned to allow for chaining operations.
 *
 * @param action A lambda function to be invoked with the value of the successful result.
 * @return The original [MessageResult] instance for chaining operations.
 */
inline fun <T> MessageResult<T>.onSuccess(action: (T) -> Unit): MessageResult<T> {
    if (this is MessageResult.Result) action(result)
    return this
}

/**
 * Executes the given action if the current instance is of type [MessageResult.Error],
 * passing the error message as a parameter to the action. This can be used to handle
 * error scenarios in a streamlined manner.
 *
 * @param action A lambda function that receives an error message as a [String] and performs some processing.
 * @return The same [MessageResult] instance, allowing for method chaining.
 */
inline fun <T> MessageResult<T>.onFailure(action: (String) -> Unit): MessageResult<T> {
    if (this is MessageResult.Error) action(error)
    return this
}

/**
 * Represents the desired return format for operations in the device driver, such as querying or writing data.
 * This sealed class defines the possible return types that can be configured for these operations.
 *
 * Types:
 * - `SAME_AS_INPUT`: Specifies that the result should have the same format/type as the input provided to the operation.
 * - `BYTES`: Specifies that the result should be returned as a `ByteArray`.
 * - `STRING`: Specifies that the result should be returned as a `String`.
 */
sealed class ReturnType {
    object SameAsInput : ReturnType()
    object Bytes : ReturnType()
    object String : ReturnType()
    object Integer : ReturnType()
    object Double : ReturnType()
    object IntegerArray : ReturnType()
    object DoubleArray : ReturnType()
}

/**
 * Represents a parameter value that can be used in interactions with a device driver.
 * This sealed class serves as a base type for concrete implementations of parameter values.
 */
sealed class ParameterValue {
    data class StringValue(val value: String) : ParameterValue()
    data class IntegerValue(val value: Int) : ParameterValue()
    data class DoubleValue(val value: Double) : ParameterValue()
    data class BytesValue(val value: ByteArray) : ParameterValue() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as BytesValue

            return value.contentEquals(other.value)
        }

        override fun hashCode(): Int {
            return value.contentHashCode()
        }
    }
    data class IntegerArrayValue(val value: IntArray) : ParameterValue() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as IntegerArrayValue
            return true
        }

        override fun hashCode(): Int {
            return value.contentHashCode()
        }
    }

    data class DoubleArrayValue(val value: DoubleArray) : ParameterValue() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as DoubleArrayValue
            return true
        }

        override fun hashCode(): Int {
            return value.contentHashCode()
        }
    }
}