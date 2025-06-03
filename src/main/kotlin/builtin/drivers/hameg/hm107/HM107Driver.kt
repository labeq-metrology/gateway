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

    }

    override suspend fun queryParameter(
        parameter: String,
        returnType: ReturnType
    ): MessageResult<ParameterValue> {
        return when (parameter) {
            "scope.acquisition.avg" -> queryAvgAcquisitions()
            "scope.acquisition.switch" -> queryAvgAcquisitionsSwitch()
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
            .onSuccess {
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
        connection.query(AVG_ACQUISITIONS_GET, 7, ReturnType.BYTES)
            .onSuccess {
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
            .onSuccess { return queryAvgAcquisitions() }
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
        connection.query(AVG_ACQUISITIONS_SWITCH_GET, 9, ReturnType.STRING)
            .onSuccess {
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
        connection.query(parameterValue.value.toString(), -1, ReturnType.STRING)
            .onSuccess {
                return queryAvgAcquisitionsSwitch()
            }
        return MessageResult.Error("Invalid response")
    }
}