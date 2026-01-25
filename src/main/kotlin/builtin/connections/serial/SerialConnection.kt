package com.labeq.builtin.connections.serial

import com.fazecast.jSerialComm.SerialPort
import com.labeq.transport.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

class SerialConnection(connectionConfiguration: ConnectionConfiguration) : DeviceConnection(
    connectionConfiguration,
    DeviceConnectionType.SERIAL
) {
    private val log = LoggerFactory.getLogger("SerialConnection@${this.hashCode()}")
    override var status = DeviceConnectionStatus.DISCONNECTED
    private val requestCounter = AtomicInteger(0)
    private val requestLock = Mutex()

    init {
        requireNotNull(configuration) { "Serial configuration cannot be null" }
    }

    val serialConfiguration = configuration as SerialConfiguration
    var port: SerialPort? = null

    override suspend fun open(): Boolean = withContext(Dispatchers.IO) {
        if (port?.isOpen == true) return@withContext true

        try {
            port = SerialPort.getCommPort(serialConfiguration.port).apply {
                if (!openPort()) {
                    log.error("Failed to open port ${serialConfiguration.port}")
                    return@withContext false
                }

                // Clear buffers immediately after opening
                flushIOBuffers()

                setFlowControl(
                    when(serialConfiguration.flowControl) {
                        SerialFlowControl.NONE -> SerialPort.FLOW_CONTROL_DISABLED
                        SerialFlowControl.SOFTWARE -> SerialPort.FLOW_CONTROL_RTS_ENABLED or SerialPort.FLOW_CONTROL_CTS_ENABLED
                        SerialFlowControl.HARDWARE -> SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED or SerialPort.FLOW_CONTROL_XONXOFF_OUT_ENABLED
                    }
                )
                setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 10000, 10000)
                setComPortParameters(
                    serialConfiguration.baudRate,
                    serialConfiguration.dataBits,
                    when (serialConfiguration.stopBits) {
                        SerialStopBits.ONE -> SerialPort.ONE_STOP_BIT
                        SerialStopBits.ONE_POINT_FIVE -> SerialPort.ONE_POINT_FIVE_STOP_BITS
                        SerialStopBits.TWO -> SerialPort.TWO_STOP_BITS
                    },
                    when (serialConfiguration.parity) {
                        SerialParity.NONE -> SerialPort.NO_PARITY
                        SerialParity.ODD -> SerialPort.ODD_PARITY
                        SerialParity.EVEN -> SerialPort.EVEN_PARITY
                        SerialParity.MARK -> SerialPort.MARK_PARITY
                        SerialParity.SPACE -> SerialPort.SPACE_PARITY
                    }
                )
            }

            // Give the port a moment to stabilize
            delay(50)

            status = DeviceConnectionStatus.CONNECTED
            true
        } catch (e: Exception) {
            log.error("Error opening serial port: ${e.message}")
            false
        }
    }

    override suspend fun close(): Boolean = withContext(Dispatchers.IO) {
        try {
            port?.takeIf { it.isOpen }?.closePort() ?: true.also {
                status = DeviceConnectionStatus.DISCONNECTED
            }
        } catch (e: Exception) {
            log.error("Error closing serial port: ${e.message}")
            false
        }
    }

    override suspend fun send(message: String): MessageResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val port = port ?: return@withContext MessageResult.Error("Port not open")
            val data = message.toByteArray()
            port.outputStream.write(data)
            port.outputStream.flush()
            MessageResult.Result(Unit)
        } catch (e: Exception) {
            MessageResult.Error("Failed to send string: ${e.message}")
        }
    }

    override suspend fun send(message: ByteArray): MessageResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val port = port ?: return@withContext MessageResult.Error("Port not open")
            port.outputStream.write(message)
            port.outputStream.flush()
            MessageResult.Result(Unit)
        } catch (e: Exception) {
            MessageResult.Error("Failed to send bytes: ${e.message}")
        }
    }

    override suspend fun query(
        message: String,
        expectedBytes: Int,
        returnType: ReturnType
    ): MessageResult<ParameterValue> = withContext(Dispatchers.IO) {
        processRequest(message.toByteArray(), expectedBytes, returnType, true)
    }

    override suspend fun query(
        message: ByteArray,
        expectedBytes: Int,
        returnType: ReturnType
    ): MessageResult<ParameterValue> = withContext(Dispatchers.IO) {
        processRequest(message, expectedBytes, returnType, false)
    }

    private suspend fun processRequest(
        data: ByteArray,
        expectedBytes: Int,
        returnType: ReturnType,
        inputWasString: Boolean
    ): MessageResult<ParameterValue> {
        val requestId = requestCounter.incrementAndGet()

        // Use mutex to prevent concurrent access
        return requestLock.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val port = port ?: return@withContext MessageResult.Error("Port not open")

                    // Flush buffers BEFORE sending to clear any stale data
                    port.flushIOBuffers()
                    delay(10) // Brief delay to ensure flush completes

                    log.debug("[Request $requestId] Sending ${data.size} bytes")
                    log.debug("[Request $requestId] Sending message: ${data.decodeToString()}")

                    // Write directly - don't use 'use' block as we need to keep stream open
                    port.outputStream.write(data)
                    port.outputStream.flush()

                    val response = if (expectedBytes == -1) {
                        readResponseUntilStopSequence(port, requestId)
                    } else {
                        readResponse(port, expectedBytes, requestId)
                    }

                    val transformed = transformResponse(response, returnType, inputWasString)
                    MessageResult.Result(transformed)
                } catch (e: Exception) {
                    log.error("[Request $requestId] Error: ${e.message}")
                    MessageResult.Error("Communication error: ${e.message}")
                }
            }
        }
    }

    private suspend fun readResponse(
        port: SerialPort,
        expectedBytes: Int,
        requestId: Int
    ): ByteArray = withContext(Dispatchers.IO) {
        val buffer = ByteArray(expectedBytes)
        var totalRead = 0
        val startTime = System.currentTimeMillis()
        val timeoutMs = calculateTimeoutMs(expectedBytes)

        log.debug("[Request $requestId] Expecting $expectedBytes bytes with timeout $timeoutMs ms")

        while (totalRead < expectedBytes) {
            val remainingTime = timeoutMs - (System.currentTimeMillis() - startTime)
            if (remainingTime <= 0) {
                throw Exception("Read timeout after $timeoutMs ms (got $totalRead/$expectedBytes bytes)")
            }

            val available = port.bytesAvailable()
            if (available > 0) {
                val toRead = available.coerceAtMost(expectedBytes - totalRead)
                val read = port.inputStream.read(buffer, totalRead, toRead)
                if (read > 0) {
                    totalRead += read
                    log.debug("[Request $requestId] Read $read bytes (total $totalRead/$expectedBytes)")
                }
            } else {
                delay(10)
            }
        }

        // Flush any extra data that might have arrived
        val remaining = port.bytesAvailable()
        if (remaining > 0) {
            log.warn("[Request $requestId] Flushing $remaining unexpected extra bytes")
            port.flushIOBuffers()
        }

        buffer
    }

    private suspend fun readResponseUntilStopSequence(
        port: SerialPort,
        requestId: Int
    ): ByteArray = withContext(Dispatchers.IO) {
        val buffer = ByteArray(1024) // Enough to hold small replies
        var totalRead = 0
        val startTime = System.currentTimeMillis()
        val timeoutMs = 3000L // up to 3 seconds for full response

        log.debug("[Request $requestId] Reading until ${serialConfiguration.stopSequence.decodeToString()} with timeout $timeoutMs ms")

        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed > timeoutMs) {
                throw Exception("Timeout waiting for ${serialConfiguration.stopSequence.decodeToString()} (after $timeoutMs ms)")
            }

            val available = port.bytesAvailable()
            if (available > 0) {
                // Read byte-by-byte to detect stop sequence
                val read = port.inputStream.read(buffer, totalRead, 1)
                if (read > 0) {
                    totalRead += read
                    if (totalRead >= serialConfiguration.stopSequence.size) {
                        var matches = true
                        for (i in serialConfiguration.stopSequence.indices) {
                            if (buffer[totalRead - serialConfiguration.stopSequence.size + i] != serialConfiguration.stopSequence[i]) {
                                matches = false
                                break
                            }
                        }
                        if (matches) {
                            break // Stop sequence reached
                        }
                    }
                }
            } else {
                delay(10)
            }
        }

        // Flush any remaining bytes after stop sequence
        val remaining = port.bytesAvailable()
        if (remaining > 0) {
            log.debug("[Request $requestId] Flushing $remaining remaining bytes after stop sequence")
            port.flushIOBuffers()
        }

        buffer.copyOf(totalRead)
    }


    private fun transformResponse(
        data: ByteArray,
        returnType: ReturnType,
        inputWasString: Boolean
    ): ParameterValue {
        return when (returnType) {
            ReturnType.SameAsInput -> if (inputWasString) {
                ParameterValue.StringValue(data.decodeToString())
            } else {
                ParameterValue.BytesValue(data)
            }

            ReturnType.String -> ParameterValue.StringValue(data.decodeToString())
            ReturnType.Bytes -> ParameterValue.BytesValue(data)
            ReturnType.Integer -> ParameterValue.IntegerValue(ByteBuffer.wrap(data).int)
            else -> ParameterValue.StringValue(data.decodeToString()) // TODO: Handle arrays
        }
    }

    private fun calculateTimeoutMs(expectedBytes: Int): Int {
        val bitsPerByte = serialConfiguration.dataBits +
                (if (serialConfiguration.parity != SerialParity.NONE) 1 else 0) +
                when (serialConfiguration.stopBits) {
                    SerialStopBits.ONE -> 1
                    SerialStopBits.ONE_POINT_FIVE -> 2
                    SerialStopBits.TWO -> 2
                }

        val transmissionTime = (bitsPerByte * expectedBytes * 1000L) / serialConfiguration.baudRate
        return (transmissionTime + 100L).coerceIn(1000L, 3000L).toInt()
    }
}
