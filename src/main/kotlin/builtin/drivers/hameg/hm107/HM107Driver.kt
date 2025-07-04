package com.labeq.builtin.drivers.hameg.hm107

import com.labeq.device.DeviceDriver
import com.labeq.transport.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.math.pow

class HM107Driver(deviceConnection: DeviceConnection) : DeviceDriver(deviceConnection) {
    private companion object {
        const val CRLF = "\r\n"

        // Remote control 
        const val REMOTE_ENABLE = " \r"
        const val REMOTE_DISABLE = "RM0$CRLF"

        // Autoset
        const val AUTOSET = "AUTOSET$CRLF"

        // Average acquisitions
        const val AVG_ACQUISITIONS = "AVRNM"
        const val AVG_ACQUISITIONS_SET = "$AVG_ACQUISITIONS="
        const val AVG_ACQUISITIONS_GET = "$AVG_ACQUISITIONS?$CRLF"
        const val AVG_ACQUISITIONS_SWITCH_GET = "${AVG_ACQUISITIONS}SW?"
        const val AVG_ACQUISITIONS_SWITCH_SET = "${AVG_ACQUISITIONS}SW="

        // Channels
        const val CHANNEL_1 = "CH1"
        const val CHANNEL_1_SETTINGS_GET = "$CHANNEL_1?"
        const val CHANNEL_1_SETTINGS_SET = "$CHANNEL_1="

        const val CHANNEL_2 = "CH2"
        const val CHANNEL_2_SETTINGS_GET = "$CHANNEL_2?"
        const val CHANNEL_2_SETTINGS_SET = "$CHANNEL_2="

        // Waveform
        const val WAVEFORM_PREAMBLE = "WFMPRE"
        const val READ_WAVEFORM = "RDWFM"

    }

    override suspend fun queryParameter(
        parameter: String,
        returnType: ReturnType
    ): MessageResult<ParameterValue> {
        return when (parameter) {
            "scope.acquisition.avg" -> queryAvgAcquisitions()
            HM107Device.PARAMETER_AVG_NUMBER_OF_ACQUISITIONS_SWITCH -> queryAvgAcquisitionsSwitch()
            HM107Device.PARAMETER_WAVEFORM_PREAMBLE -> queryWaveformPreamble()
            HM107Device.PARAMETER_CHANNEL_1_WAVEFORM -> queryWaveform(1)
            HM107Device.PARAMETER_CHANNEL_2_WAVEFORM -> queryWaveform(2)
            HM107Device.PARAMETER_CHANNEL_1_SETTINGS -> queryChannelSettings(1)
            HM107Device.PARAMETER_CHANNEL_2_SETTINGS -> queryChannelSettings(2)
            else -> MessageResult.Error("Invalid parameter: $parameter")
        }
    }

    override suspend fun writeParameter(
        parameter: String,
        value: ParameterValue,
        returnType: ReturnType
    ): MessageResult<ParameterValue> {
        return when (parameter) {
            "scope.autoset" -> autoset()
            "scope.acquisition.avg" -> setAvgAcquisitions(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_CHANNEL_1_WAVEFORM -> MessageResult.Error("Read-only parameter")
            HM107Device.PARAMETER_CHANNEL_2_WAVEFORM -> MessageResult.Error("Read-only parameter")
            else -> MessageResult.Error("Invalid parameter: $parameter")
        }
    }

    override suspend fun writeData(
        data: ByteArray,
        returnType: ReturnType
    ): MessageResult<ParameterValue> = connection.query(data, -1, returnType)

    override suspend fun writeData(
        data: String,
        returnType: ReturnType
    ): MessageResult<ParameterValue> = connection.query(data, -1, returnType)

    override fun connect(): Boolean {
        return runBlocking {
            if (!connection.open()) return@runBlocking false
            return@runBlocking resetRemote()
        }
    }

    override fun disconnect(): Boolean {
        return runBlocking {
            resetRemote()
            delay(5)
            connection.query(REMOTE_DISABLE, -1).onSuccess {
                connection.close()
                return@runBlocking true
            }
            return@runBlocking false
        }
    }

    /**
     * Tries enabling the remote operation mode of the device.
     *
     * @return Boolean indicating whether remote mode was successfully configured
     */
    private suspend fun resetRemote(): Boolean {
        val result = connection.query(REMOTE_ENABLE, -1)
        return when (result) {
            is MessageResult.Error -> {
                connection.query(REMOTE_DISABLE, -1)
                delay(5)
                return when (connection.query(REMOTE_ENABLE, -1)) {
                    is MessageResult.Error -> false
                    is MessageResult.Result<*> -> true
                }
            }

            is MessageResult.Result -> true
        }
    }

    /**
     * Executes the AUTOSET command.
     *
     * AUTOSET automatically configures:
     * - Vertical sensitivity
     * - Time base setting
     * - Trigger level
     * To optimize signal display based on input signal characteristics.
     *
     * @return 1, if the operation succeeded
     */
    private suspend fun autoset(): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query(AUTOSET, -1)
            .onSuccessAsync {
                return MessageResult.Result(ParameterValue.IntegerValue(1))
            }
        return MessageResult.Error("Autoset failed")
    }

    /**
     * Queries the current average acquisitions setting (AVRNM command).
     *
     * This command controls signal averaging:
     * - Values 0-9 set number of averages to 2^n (where n is 0-9)
     * - Returns byte value indicating current averaging exponent
     *
     * Query format: AVRNM?<CR><LF>
     * Response: Single byte containing exponent value
     *
     * @return average acquisitions
     */
    private suspend fun queryAvgAcquisitions(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query(AVG_ACQUISITIONS_GET, 7, ReturnType.Bytes)
            .onSuccessAsync {
                when (it) {
                    is ParameterValue.BytesValue -> {
                        if (it.value.size != 7) return MessageResult.Error("Invalid response")
                        return MessageResult.Result(
                            ParameterValue.IntegerValue(2.0.pow(it.value[6].toInt()).toInt())
                        )
                    }

                    else -> {}
                }
            }
        return MessageResult.Error("Invalid response")
    }

    /**
     * Sets the average acquisitions count using AVRNM command.
     *
     * According to HM1507-3 manual:
     * - Values must be between 1-9
     * - Sets number of averages to 2^n where n is the input value
     * - Validates input range before sending
     *
     * Command format: AVRNM=n where n is an averaging exponent
     * @param parameterValue the value to be written (1 <= x <= 9)
     * @return the average acquisitions parameter read **AFTER** trying to write the desired value
     */
    private suspend fun setAvgAcquisitions(
        parameterValue: ParameterValue.IntegerValue
    ): MessageResult<ParameterValue.IntegerValue> {
        if (parameterValue.value !in 1..9) return MessageResult.Error("Invalid value: AVRNM must be 1 <= x <= 9")
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.send(AVG_ACQUISITIONS_SET)
        connection.query(byteArrayOf(parameterValue.value.toByte()), -1)
            .onSuccessAsync { return queryAvgAcquisitions() }
        return MessageResult.Error("Invalid response")
    }

    /**
     * Queries average acquisition switch status (AVRNMSW command).
     *
     * As defined in HM1507-3 manual, this command checks if signal averaging
     * is enabled or disabled:
     * - Returns 0 if averaging disabled
     * - Returns 1 if averaging enabled
     *
     * Query format: AVRNMSW?
     *
     * @return the state of the average acquisitions switch
     * @return the average acquisitions parameter
     */
    private suspend fun queryAvgAcquisitionsSwitch(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query(AVG_ACQUISITIONS_SWITCH_GET, 9, ReturnType.String)
            .onSuccessAsync {
                if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(it.value.last().digitToInt()))
            }
        return MessageResult.Error("Invalid response")
    }

    /**
     * Controls average acquisition switch state (AVRNMSW command).
     *
     * Following HM1507-3 specifications:
     * - 0 = Disable signal averaging
     * - 1 = Enable signal averaging
     *
     * Command format: AVRNMSW=n where n is 0 or 1
     *
     * @param parameterValue the value to be written (0 or 1)
     * @return the state of the average acquisitions switch read **AFTER** trying to write the parameter
     */
    private suspend fun setAvgAcquisitionsSwitch(
        parameterValue: ParameterValue.IntegerValue
    ): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        if (parameterValue.value !in 0..1) return MessageResult.Error("Invalid value: AVRNM must be set to 0 or 1")
        connection.send(AVG_ACQUISITIONS_SWITCH_SET)
        connection.query(parameterValue.value.toString(), -1, ReturnType.String)
            .onSuccessAsync {
                return queryAvgAcquisitionsSwitch()
            }
        return MessageResult.Error("Invalid response")
    }

    // Channels

    private suspend fun queryChannelSettings(channel: Int): MessageResult<ParameterValue.BytesValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        if (channel != 1 && channel != 2) return MessageResult.Error("Invalid channel")
        connection.send(if (channel == 1) CHANNEL_1 else CHANNEL_2)
        connection.query("?", 5, ReturnType.Bytes)
            .onSuccessAsync {
                if (it !is ParameterValue.BytesValue) return MessageResult.Error("Invalid response")
                return MessageResult.Result(
                    ParameterValue.BytesValue(it.value.drop(4).toByteArray())
                )
            }
        return MessageResult.Error("Invalid response")
    }

    // Waveform

    private suspend fun queryWaveformPreamble(): MessageResult<ParameterValue.BytesValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$WAVEFORM_PREAMBLE?", 17, ReturnType.Bytes)
            .onSuccessAsync {
                if (it !is ParameterValue.BytesValue) return MessageResult.Error("Invalid response")
                return MessageResult.Result(
                    ParameterValue.BytesValue(it.value.drop(7).toByteArray()) // drop "WFMPRE:"
                )
            }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryWaveform(channel: Int): MessageResult<ParameterValue.BytesValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        if (channel != 1 && channel != 2) return MessageResult.Error("Invalid channel")
        connection.send("$READ_WAVEFORM$channel:")
        connection.send(byteArrayOf(0, 0, 0, 8))
        connection.query(CRLF, 2048 + 11, ReturnType.Bytes)
            .onSuccessAsync {
                if (it !is ParameterValue.BytesValue) return MessageResult.Error("Invalid response")
                return MessageResult.Result(
                    ParameterValue.BytesValue(it.value.drop(11).toByteArray())
                )
            }
        return MessageResult.Error("Invalid response")
    }
}
