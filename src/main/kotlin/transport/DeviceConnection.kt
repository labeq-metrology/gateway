package com.labeq.transport

import com.labeq.error.ConnectionTypeMismatchError

/**
 * Represents a base class for managing and interacting with various types of device connections.
 *
 * This abstract class provides a framework for establishing, managing, and terminating connections
 * with devices using different communication protocols. It encapsulates the common behavior and
 * requirements of device connections while allowing specific implementations to define protocol-specific
 * functionality.
 *
 * @property configuration The configuration parameters for establishing the connection.
 * @property connectionType The type of device connection, such as network, serial, USB, etc.
 *
 * @throws ConnectionTypeMismatchError Thrown if the provided connection type in the configuration
 * does not match the declared connection type of the device connection.
 */
abstract class DeviceConnection(val configuration: ConnectionConfiguration, val connectionType: DeviceConnectionType) {

    /**
     * Represents the current state of the device connection. This property helps to monitor
     * and manage the connection's lifecycle by indicating statuses such as `DISCONNECTED`,
     * `CONNECTED`, `CONNECTING`, `TIMEOUT`, `ERROR`, or `UNKNOWN`.
     *
     * The status provides valuable information about the device connection's operational
     * state and can be used by implementations or consumers to handle connection logic
     * appropriately.
     */
    abstract var status: DeviceConnectionStatus

    init {
        if (configuration.connectionType != connectionType) throw ConnectionTypeMismatchError()
    }

    /**
     * Establishes and initializes a connection to a device.
     *
     * This method is responsible for configuring and opening a connection
     * to a device based on its specific type and settings. It performs any
     * necessary setup actions required to ensure that the device connection
     * is ready for communication.
     *
     * @return Boolean indicating the success or failure of the connection setup.
     *         Returns `true` if the connection is successfully opened, otherwise `false`.
     */
    abstract suspend fun open(): Boolean

    /**
     * Closes the active connection to the device.
     *
     * This method is responsible for terminating the current connection and releasing
     * any resources associated with it. Implementations should ensure that the connection
     * is properly closed to prevent resource leaks or unintended behavior.
     *
     * @return Boolean indicating the outcome of the closing operation.
     *         Returns `true` if the connection is successfully closed, otherwise `false`.
     */
    abstract suspend fun close(): Boolean

    /**
     * Sends a message to the connected device.
     *
     * This method transmits a string message to the target device over the established connection.
     * It ensures the data is sent successfully and captures any resulting status or errors.
     *
     * @param message The message to send as a string.
     * @return A [MessageResult] containing either the success result (`Unit`) or an error message.
     */
    abstract suspend fun send(message: String): MessageResult<Unit>

    /**
     * Sends a message to the connected device using its byte representation.
     *
     * This method is responsible for transmitting binary data to the device
     * over the established connection. It ensures that the data is sent successfully
     * and returns a result indicating the operation's outcome.
     *
     * @param message The binary data to send as a `ByteArray`.
     * @return A [MessageResult] containing either the success result (`Unit`) if the message was sent
     *         successfully, or an error message detailing what went wrong.
     */
    abstract suspend fun send(message: ByteArray): MessageResult<Unit>

    /**
     * Sends a message to the connected device and retrieves the resulting response.
     *
     * This method transmits a string message to the target device over the established connection.
     * It waits for the response based on the specified expected byte size and formats the result
     * according to the desired return type.
     *
     * @param message The message to send as a string.
     * @param expectedBytes The number of bytes expected in the response from the device.
     * @param returnType Specifies the desired return format of the response.
     *        Defaults to [ReturnType.SAME_AS_INPUT], where the response format matches the input.
     * @return A [MessageResult] containing the response as a [ParameterValue] if successful,
     *         or an error message if the operation fails.
     */
    abstract suspend fun query(
        message: String,
        expectedBytes: Int,
        returnType: ReturnType = ReturnType.SameAsInput
    ): MessageResult<ParameterValue>

    /**
     * Sends a binary message to the connected device and retrieves the resulting response.
     *
     * This method transmits a binary message to the target device over an established connection.
     * It waits for the device's response based on the specified expected byte size and formats the
     * result according to the desired return type.
     *
     * @param message The binary data to send as a `ByteArray`.
     * @param expectedBytes The number of bytes expected in the response from the device.
     * @param returnType Specifies the desired return format of the response.
     *        Defaults to [ReturnType.SAME_AS_INPUT], where the response format matches the input type.
     * @return A [MessageResult] containing the response as a [ParameterValue] if successful,
     *         or an error indicating the failure reason.
     */
    abstract suspend fun query(
        message: ByteArray,
        expectedBytes: Int,
        returnType: ReturnType = ReturnType.SameAsInput
    ): MessageResult<ParameterValue>
}
