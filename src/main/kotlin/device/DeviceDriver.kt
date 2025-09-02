package com.labeq.device

import com.labeq.transport.*

/**
 * Represents an abstract driver for managing communication and operations with a device.
 * This class serves as a foundation for implementing specific device drivers, providing
 * methods for querying, writing parameters, sending data, and managing device connections.
 *
 * @property connection The underlying connection used to communicate with the device.
 */
abstract class DeviceDriver(val connection: DeviceConnection) {

    /**
     * Queries a specific parameter on the device and returns its value in the desired format.
     *
     * @param parameter The name or identifier of the parameter to query.
     * @param returnType Specifies the desired return format for the parameter value. Defaults to `ReturnType.STRING`.
     * @return A [MessageResult] containing the queried parameter value as a [ParameterValue] if successful,
     *         or an error message if the operation fails.
     */
    abstract suspend fun queryParameter(
        parameter: String,
        returnType: ReturnType = ReturnType.String
    ): MessageResult<ParameterValue>

    /**
     * Writes a parameter with the specified value to the connected device and optionally returns the result in a desired format.
     *
     * @param parameter The name or identifier of the parameter to write.
     * @param value The value of the parameter to be written, provided as a [ParameterValue].
     * @param returnType Specifies the desired return format for the operation's result. Defaults to [ReturnType.SameAsInput].
     * @return A [MessageResult] containing the parameter value as a [ParameterValue] in the requested format if the operation is successful, or an error message if the operation
     *  fails.
     */
    abstract suspend fun writeParameter(
        parameter: String,
        value: ParameterValue,
        returnType: ReturnType = ReturnType.SameAsInput
    ): MessageResult<ParameterValue>

    /**
     * Writes the specified data to the connected device and optionally returns the result in a desired format.
     *
     * @param data The data to be written, provided as a [ByteArray].
     * @param returnType Specifies the desired return format for the operation's result. Defaults to [ReturnType.SameAsInput].
     * @return A [MessageResult] containing the result as a [ParameterValue] in the requested format if the operation is successful,
     * or an error message if the operation fails.
     */
    abstract suspend fun writeData(
        data: ByteArray,
        returnType: ReturnType = ReturnType.SameAsInput
    ): MessageResult<ParameterValue>

    /**
     * Writes the specified string data to the connected device and optionally returns the result in a desired format.
     *
     * @param data The data to be written, provided as a String.
     * @param returnType Specifies the desired return format for the operation's result. Defaults to [ReturnType.SameAsInput].
     * @return A [MessageResult] containing the result as a [ParameterValue] in the requested format if the operation is successful,
     *         or an error message if the operation fails.
     */
    abstract suspend fun writeData(
        data: String,
        returnType: ReturnType = ReturnType.SameAsInput
    ): MessageResult<ParameterValue>

    /**
     * Establishes a connection to the device using the implementing driver's connection mechanism.
     *
     * @return `true` if the connection is successfully established, `false` otherwise.
     */
    abstract fun connect(): Boolean

    /**
     * Terminates the connection to the device.
     *
     * @return `true` if the disconnection is successful, `false` otherwise.
     */
    abstract fun disconnect(): Boolean
}
