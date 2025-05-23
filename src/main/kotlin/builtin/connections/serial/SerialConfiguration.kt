package com.labeq.builtin.connections.serial

import com.labeq.transport.ConnectionConfiguration
import com.labeq.transport.DeviceConnectionType

data class SerialConfiguration(
    override val connectionType: DeviceConnectionType = DeviceConnectionType.SERIAL,
    val port: String = "COM1",
    val baudRate: Int = 9600,
    val dataBits: Int = 8,
    val stopBits: SerialStopBits = SerialStopBits.TWO,
    val parity: SerialParity = SerialParity.NONE,
    val flowControl: SerialFlowControl = SerialFlowControl.SOFTWARE,
    val stopSequence: ByteArray = "\r\n".toByteArray()
) : ConnectionConfiguration {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SerialConfiguration

        if (baudRate != other.baudRate) return false
        if (dataBits != other.dataBits) return false
        if (connectionType != other.connectionType) return false
        if (port != other.port) return false
        if (stopBits != other.stopBits) return false
        if (parity != other.parity) return false
        if (flowControl != other.flowControl) return false
        if (!stopSequence.contentEquals(other.stopSequence)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = baudRate
        result = 31 * result + dataBits
        result = 31 * result + connectionType.hashCode()
        result = 31 * result + port.hashCode()
        result = 31 * result + stopBits.hashCode()
        result = 31 * result + parity.hashCode()
        result = 31 * result + flowControl.hashCode()
        result = 31 * result + stopSequence.contentHashCode()
        return result
    }
}
