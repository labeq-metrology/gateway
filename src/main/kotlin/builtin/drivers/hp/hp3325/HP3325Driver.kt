// HP3325Driver.kt
package com.labeq.builtin.drivers.hp.hp3325

import com.labeq.device.DeviceDriver
import com.labeq.transport.*
import kotlinx.coroutines.runBlocking

class HP3325Driver(deviceConnection: DeviceConnection) : DeviceDriver(deviceConnection) {
    private companion object {
        const val INTERROGATE = "?"

        // Signal generation
        const val AMPLITUDE = "AM"
        const val FREQUENCY = "FR"
        const val WAVEFORM_FUNCTION = "FU"

        const val HV_MODE = "HV"
        const val HV_MODE_QUERY = "IRF"

        const val DC_OFFSET = "OF"

        const val PHASE = "PH"
        const val ZERO_PHASE = "AP"

        const val REAR_FRONT_OUTPUT = "RF"

        // Sweep-related
        const val SWEEP_START = "ST"
        const val SWEEP_STOP = "SP"
        const val SWEEP_TIME = "TI"
        const val SWEEP_MODE = "SM"

        const val DISC_SWEEP_CLEAR = "DCLR"
        const val DISC_SWEEP_STORE = "DSTO"
        const val DISC_SWEEP_RECALL = "DRCL"

        const val START_SINGLE_SWEEP = "SS"
        const val RESET_SINGLE_SWEEP = "RSW"

        const val START_CONTINUOUS_SWEEP = "SC"

        // Marker / auxiliary
        const val MARKER_FREQ = "MF"

        // Modulation
        const val MODULATION_SOURCE_AMPLITUDE = "MOAM"
        const val MODULATION_SOURCE_ARBITRARY_DATA = "MOAR"
        const val MODULATION_SOURCE_FREQUENCY = "MOFR"
        const val MODULATION_SOURCE_FUNCTION = "MOFU"

        const val AMPLITUDE_MODULATION = "MA"
        const val PHASE_MODULATION = "MP"

        // Memory / states
        const val STORE_STATE = "SR"
        const val RECALL_STATE = "RE"

        // Status / errors / self test
        const val AMPLITUDE_CAL = "AC"
        const val CAL_MODE = "CALM"

        const val ENHANCEMENTS = "ENH"

        const val ERROR = "ERR"
        const val ERROR_INT = "IER"

        const val EXT_REF_LOCKED = "EXTR"

        const val LOCAL_ENABLE = "LCL"
        const val REMOTE_ENABLE = "RMT"
        const val RESET_DEVICE = "*RST"

        const val STATUS_BYTE_MASK = "MS"
        const val SRQ_ENABLE = "ESTB"
        const val STATUS_BYTE_QUERY = "QSTB"

        const val ID = "ID"
        const val IDN = "*IDN"
        const val OPTION = "OPT"

        // Communication
        const val ECHO = "ECHO"
        const val RES_HEAD = "HEAD"

        const val DATA_TRANSFER_MODE = "MD"

        // Display
        const val DISPLAY_TOGGLE = "DISP"
        const val DISPLAY_STRING = "DSP"
    }

    private val freqRanges = mapOf(
        "HZ" to (0.0 to 21e6),
        "KH" to (0.0 to 21000.0),
        "MH" to (0.0 to 21.0)
    )

    private val amplitudeRangesNormal = mapOf(
        "VO" to (0.0 to 10.0),
        "MV" to (0.0 to 10000.0),
        "VR" to (0.0 to 3.54),
        "MR" to (0.0 to 3540.0),
        "DB" to (-70.0 to 24.0),
        "DV" to (-70.0 to 24.0)
    )

    private val offsetRangesNormal = mapOf(
        "VO" to (-5.0 to 5.0),
        "MV" to (-5000.0 to 5000.0)
    )

    private val amplitudeRangesHV = mapOf(
        "VO" to (0.0 to 40.0),
        "MV" to (0.0 to 40000.0),
        "VR" to (0.0 to 14.2),
        "MR" to (0.0 to 14200.0),
        "DB" to (-70.0 to 32.0),
        "DV" to (-70.0 to 32.0)
    )

    private val offsetRangesHV = mapOf(
        "VO" to (-20.0 to 20.0),
        "MV" to (-20000.0 to 20000.0)
    )

    private val timeRanges = mapOf("SE" to (0.0 to 1000.0), "S" to (0.0 to 1000.0))
    private val phaseRanges = mapOf("DE" to (-720.0 to 720.0), "DEG" to (-720.0 to 720.0))

    override suspend fun queryParameter(
        parameter: String, returnType: ReturnType
    ): MessageResult<ParameterValue> {
        connection.open()
        connection.send("\r\n")
        val result = when (parameter) {
            HP3325Device.PARAMETER_GENERATOR_FREQUENCY -> standardStringInterrogate(FREQUENCY)
            HP3325Device.PARAMETER_GENERATOR_AMPLITUDE -> standardStringInterrogate(AMPLITUDE)
            HP3325Device.PARAMETER_GENERATOR_FUNCTION -> standardIntInterrogate(WAVEFORM_FUNCTION)
            HP3325Device.PARAMETER_GENERATOR_PHASE -> standardStringInterrogate(PHASE)

            HP3325Device.PARAMETER_GENERATOR_HV -> standardIntInterrogate(HV_MODE_QUERY, "RF")
            HP3325Device.PARAMETER_ERROR -> standardIntInterrogate(ERROR_INT, "ER")
            HP3325Device.PARAMETER_IDENTIFICATION_STANDARD -> standardStringInterrogate(ID, "HP")
            HP3325Device.PARAMETER_IDENTIFICATION_488_2 -> standardStringInterrogate(IDN, "")
            HP3325Device.PARAMETER_OPTIONS -> standardStringInterrogate(OPTION, "")
            HP3325Device.PARAMETER_SRQ_ENABLE -> standardStringInterrogate(SRQ_ENABLE, "")
            HP3325Device.PARAMETER_STATUS_BYTE -> standardStringInterrogate(STATUS_BYTE_QUERY, "")
            HP3325Device.PARAMETER_DATA_TRANSFER_MODE -> standardIntInterrogate(DATA_TRANSFER_MODE, "")
            HP3325Device.PARAMETER_GENERATOR_OUTPUT_SELECT -> standardIntInterrogate(REAR_FRONT_OUTPUT, "RF")

            HP3325Device.PARAMETER_GENERATOR_ZERO_PHASE -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_GENERATOR_OFFSET -> standardStringInterrogate(DC_OFFSET)

            HP3325Device.PARAMETER_SWEEP_START -> standardStringInterrogate(SWEEP_START)
            HP3325Device.PARAMETER_SWEEP_STOP -> standardStringInterrogate(SWEEP_STOP)
            HP3325Device.PARAMETER_SWEEP_TIME -> standardStringInterrogate(SWEEP_TIME)
            HP3325Device.PARAMETER_SWEEP_MODE -> standardIntInterrogate(SWEEP_MODE)

            HP3325Device.PARAMETER_SWEEP_DISCRETE_CLEAR -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_SWEEP_DISCRETE_STORE -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_SWEEP_DISCRETE_RECALL -> MessageResult.Error("Write-only parameter.")

            HP3325Device.PARAMETER_SWEEP_SINGLE_START -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_SWEEP_SINGLE_RESET -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_SWEEP_CONTINUOUS_START -> MessageResult.Error("Write-only parameter.")

            HP3325Device.PARAMETER_MARKER_FREQUENCY -> standardStringInterrogate(MARKER_FREQ)

            HP3325Device.PARAMETER_MODULATION_SOURCE_AMPLITUDE -> standardStringInterrogate(MODULATION_SOURCE_AMPLITUDE)
            HP3325Device.PARAMETER_MODULATION_SOURCE_ARBITRARY_DATA -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_MODULATION_SOURCE_FREQUENCY -> standardStringInterrogate(MODULATION_SOURCE_FREQUENCY)
            HP3325Device.PARAMETER_MODULATION_SOURCE_FUNCTION -> standardIntInterrogate(MODULATION_SOURCE_FUNCTION)

            HP3325Device.PARAMETER_AMPLITUDE_MODULATION -> standardIntInterrogate(AMPLITUDE_MODULATION)
            HP3325Device.PARAMETER_PHASE_MODULATION -> standardIntInterrogate(PHASE_MODULATION)

            HP3325Device.PARAMETER_STATE_STORE -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_STATE_RECALL -> MessageResult.Error("Write-only parameter.")

            HP3325Device.PARAMETER_CALIBRATION_AMPLITUDE -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_CALIBRATION_MODE -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_ENHANCEMENTS -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_EXTERNAL_REFERENCE_LOCK -> standardIntInterrogate(EXT_REF_LOCKED, "")
            HP3325Device.PARAMETER_LOCAL_ENABLE -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_REMOTE_ENABLE -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_RESET_DEVICE -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_BYTE_MASK -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_RS232_ECHO -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_RES_HEAD -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_DISPLAY_ENABLE -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_DISPLAY_STRING -> MessageResult.Error("Write-only parameter.")

            else -> MessageResult.Error("Invalid parameter: $parameter")
        }
        connection.close()

        return if (result is MessageResult.Result) {
            MessageResult.Result(
                result.result.convertTo(
                    if (returnType == ReturnType.SameAsInput) ReturnType.String else returnType
                )
            )
        } else {
            result
        }
    }

    override suspend fun writeParameter(
        parameter: String, value: ParameterValue, returnType: ReturnType
    ): MessageResult<ParameterValue> {
        connection.open()
        val result = when (parameter) {
            HP3325Device.PARAMETER_GENERATOR_FREQUENCY -> writeNumericCommand(FREQUENCY, value, "HZ", freqRanges)
            HP3325Device.PARAMETER_SWEEP_START -> writeNumericCommand(SWEEP_START, value, "HZ", freqRanges)
            HP3325Device.PARAMETER_SWEEP_STOP -> writeNumericCommand(SWEEP_STOP, value, "HZ", freqRanges)
            HP3325Device.PARAMETER_MARKER_FREQUENCY -> writeNumericCommand(MARKER_FREQ, value, "HZ", freqRanges)
            HP3325Device.PARAMETER_MODULATION_SOURCE_FREQUENCY -> writeNumericCommand(
                MODULATION_SOURCE_FREQUENCY,
                value,
                "HZ",
                freqRanges
            )

            HP3325Device.PARAMETER_GENERATOR_AMPLITUDE, HP3325Device.PARAMETER_MODULATION_SOURCE_AMPLITUDE -> {
                val cmd =
                    if (parameter == HP3325Device.PARAMETER_GENERATOR_AMPLITUDE) AMPLITUDE else MODULATION_SOURCE_AMPLITUDE
                val ranges = if (isHvEnabled()) amplitudeRangesHV else amplitudeRangesNormal
                writeNumericCommand(cmd, value, "VO", ranges)
            }

            HP3325Device.PARAMETER_GENERATOR_OFFSET -> {
                val ranges = if (isHvEnabled()) offsetRangesHV else offsetRangesNormal
                writeNumericCommand(DC_OFFSET, value, "VO", ranges)
            }

            HP3325Device.PARAMETER_GENERATOR_PHASE -> writeNumericCommand(PHASE, value, "DE", phaseRanges)
            HP3325Device.PARAMETER_SWEEP_TIME -> writeNumericCommand(SWEEP_TIME, value, "SE", timeRanges)

            HP3325Device.PARAMETER_GENERATOR_FUNCTION -> writeIntCommand(WAVEFORM_FUNCTION, value, min = 0, max = 5)
            HP3325Device.PARAMETER_SWEEP_MODE -> writeIntCommand(SWEEP_MODE, value, min = 1, max = 3)
            HP3325Device.PARAMETER_MODULATION_SOURCE_FUNCTION -> writeIntCommand(
                MODULATION_SOURCE_FUNCTION,
                value,
                min = 0,
                max = 3
            )

            HP3325Device.PARAMETER_GENERATOR_HV -> writeIntCommand(
                HV_MODE,
                value,
                min = 0,
                max = 1,
                interrogateParam = "HV"
            )

            HP3325Device.PARAMETER_GENERATOR_OUTPUT_SELECT -> writeIntCommand(
                REAR_FRONT_OUTPUT,
                value,
                min = 1,
                max = 2,
                interrogateParam = "RF"
            )

            HP3325Device.PARAMETER_AMPLITUDE_MODULATION -> writeIntCommand(
                AMPLITUDE_MODULATION,
                value,
                min = 0,
                max = 1
            )

            HP3325Device.PARAMETER_PHASE_MODULATION -> writeIntCommand(PHASE_MODULATION, value, min = 0, max = 1)
            HP3325Device.PARAMETER_ENHANCEMENTS -> writeIntCommand(
                ENHANCEMENTS,
                value,
                min = 0,
                max = 1,
                interrogate = false
            )

            HP3325Device.PARAMETER_RS232_ECHO -> writeIntCommand(ECHO, value, min = 0, max = 1, interrogate = false)
            HP3325Device.PARAMETER_RES_HEAD -> writeIntCommand(RES_HEAD, value, min = 0, max = 1, interrogate = false)
            HP3325Device.PARAMETER_DATA_TRANSFER_MODE -> writeIntCommand(
                DATA_TRANSFER_MODE,
                value,
                min = 1,
                max = 2,
                interrogateParam = ""
            )

            HP3325Device.PARAMETER_DISPLAY_ENABLE -> writeIntCommand(
                DISPLAY_TOGGLE,
                value,
                min = 0,
                max = 1,
                interrogate = false
            )

            HP3325Device.PARAMETER_SWEEP_DISCRETE_STORE -> writeActionCommand(DISC_SWEEP_STORE, value)
            HP3325Device.PARAMETER_SWEEP_DISCRETE_RECALL -> writeActionCommand(DISC_SWEEP_RECALL, value)
            HP3325Device.PARAMETER_STATE_STORE -> writeActionCommand(STORE_STATE, value)
            HP3325Device.PARAMETER_STATE_RECALL -> writeActionCommand(RECALL_STATE, value)
            HP3325Device.PARAMETER_CALIBRATION_MODE -> writeActionCommand(CAL_MODE, value)
            HP3325Device.PARAMETER_SRQ_ENABLE -> writeActionCommand(SRQ_ENABLE, value)

            HP3325Device.PARAMETER_GENERATOR_ZERO_PHASE -> writeActionCommand(ZERO_PHASE)
            HP3325Device.PARAMETER_SWEEP_DISCRETE_CLEAR -> writeActionCommand(DISC_SWEEP_CLEAR)
            HP3325Device.PARAMETER_SWEEP_SINGLE_START -> writeActionCommand(START_SINGLE_SWEEP)
            HP3325Device.PARAMETER_SWEEP_SINGLE_RESET -> writeActionCommand(RESET_SINGLE_SWEEP)
            HP3325Device.PARAMETER_SWEEP_CONTINUOUS_START -> writeActionCommand(START_CONTINUOUS_SWEEP)
            HP3325Device.PARAMETER_CALIBRATION_AMPLITUDE -> writeActionCommand(AMPLITUDE_CAL)
            HP3325Device.PARAMETER_LOCAL_ENABLE -> writeActionCommand(LOCAL_ENABLE)
            HP3325Device.PARAMETER_REMOTE_ENABLE -> writeActionCommand(REMOTE_ENABLE)
            HP3325Device.PARAMETER_RESET_DEVICE -> writeActionCommand(RESET_DEVICE)

            HP3325Device.PARAMETER_DISPLAY_STRING -> writeStringCommand(
                DISPLAY_STRING,
                value,
                quote = true,
                interrogate = false
            )

            else -> MessageResult.Error("Invalid parameter or write not supported: $parameter")
        }
        connection.send("\r\n")
        connection.close()

        return if (result is MessageResult.Result) {
            MessageResult.Result(result.result.convertTo(returnType))
        } else {
            result
        }
    }

    override suspend fun writeData(data: ByteArray, returnType: ReturnType): MessageResult<ParameterValue> {
        TODO("Not yet implemented")
    }

    override suspend fun writeData(data: String, returnType: ReturnType): MessageResult<ParameterValue> {
        TODO("Not yet implemented")
    }

    override fun connect(): Boolean {
        return runBlocking {
            if (!connection.open()) return@runBlocking false
            connection.send(REMOTE_ENABLE)
            connection.send("${DISPLAY_TOGGLE}1")

            val res = standardStringInterrogate(AMPLITUDE)
            if (res is MessageResult.Result) {
                connection.send("$DISPLAY_STRING'GATEWAY CTL'")
                return@runBlocking true
            }
            false
        }
    }

    override fun disconnect(): Boolean {
        return runBlocking {
            if (!connection.open()) return@runBlocking false
            connection.send("$DISPLAY_STRING'GATEWAY dSC'")
            connection.send(LOCAL_ENABLE)
            connection.close()
        }
    }

    private suspend fun isHvEnabled(): Boolean {
        if (!connection.open()) return false
//            val res = connection.query(HV_MODE_QUERY + INTERROGATE, -1)

//            if (res is MessageResult.Result) {
//                val strVal = (res.result as? ParameterValue.StringValue)?.value?.trim() ?: continue
//                // Accept either HV or RF prefix anomaly from option 002 instruments
//                val cleaned = strVal.removePrefix("HV").removePrefix("RF").trim()
//                return cleaned.toIntOrNull() == 1
//            }
        return false
    }

    private fun stripPrefix(response: String, prefix: String): String =
        if (prefix.isEmpty()) response else response.removePrefix(prefix).trim()

    private suspend fun standardStringInterrogate(
        parameter: String,
        interrogateParam: String = parameter,
        maxRetries: Int = 6
    ): MessageResult<ParameterValue.StringValue> {
        var lastError = "Unknown error"

        for (attempt in 1..maxRetries) {
            if (!connection.open()) return MessageResult.Error("Connection is not open")

            val result = connection.query(parameter + INTERROGATE, -1)

            if (result is MessageResult.Result) {
                val strValue = result.result as? ParameterValue.StringValue ?: continue
                val responseText = strValue.value.trim()

                if (interrogateParam.isEmpty() || responseText.startsWith(interrogateParam, ignoreCase = true)) {
                    return MessageResult.Result(ParameterValue.StringValue(stripPrefix(responseText, interrogateParam)))
                } else {
                    // Mismatch detected. We do nothing except log it and loop.
                    // The loop sends the parameter query again, exhausting the stale response buffer naturally.
                    lastError = "Stale or mismatched device response. Expected $interrogateParam..., got: $responseText"
                }
            } else if (result is MessageResult.Error) {
                lastError = result.error
            }
        }

        return MessageResult.Error("Failed after $maxRetries attempts. Last error: $lastError")
    }

    private suspend fun standardIntInterrogate(
        parameter: String,
        interrogateParam: String = parameter,
        maxRetries: Int = 6
    ): MessageResult<ParameterValue.IntegerValue> {
        var lastError = "Unknown error"

        for (attempt in 1..maxRetries) {
            if (!connection.open()) return MessageResult.Error("Connection is not open")

            val result = connection.query(parameter + INTERROGATE, -1)

            if (result is MessageResult.Result) {
                val strValue = result.result as? ParameterValue.StringValue ?: continue
                val responseText = strValue.value.trim()

                if (interrogateParam.isEmpty() || responseText.startsWith(interrogateParam, ignoreCase = true)) {
                    val parsedInt = if (interrogateParam.isEmpty()) {
                        responseText.replace(Regex("[^0-9-]"), "").toIntOrNull() ?: 0
                    } else {
                        stripPrefix(responseText, interrogateParam).toIntOrNull() ?: 0
                    }
                    return MessageResult.Result(ParameterValue.IntegerValue(parsedInt))
                } else {
                    // Mismatch detected. Loop naturally drains the buffer.
                    lastError = "Stale or mismatched device response. Expected $interrogateParam..., got: $responseText"
                }
            } else if (result is MessageResult.Error) {
                lastError = result.error
            }
        }

        return MessageResult.Error("Failed after $maxRetries attempts. Last error: $lastError")
    }

    private suspend fun writeNumericCommand(
        command: String,
        value: ParameterValue,
        defaultSuffix: String,
        ranges: Map<String, Pair<Double, Double>>
    ): MessageResult<ParameterValue> {
        var numVal: Double
        var unit: String

        when (value) {
            is ParameterValue.StringValue -> {
                val regex = "(?<value>[-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)\\s*(?<unit>[a-zA-Z]*)".toRegex()
                val match = regex.matchEntire(value.value.trim())
                    ?: return MessageResult.Error("Invalid format: ${value.value}")
                numVal = match.groupValues[1].toDoubleOrNull() ?: 0.0
                unit = match.groupValues[2].uppercase().takeIf { it.isNotEmpty() } ?: defaultSuffix
            }

            is ParameterValue.DoubleValue -> {
                numVal = value.value
                unit = defaultSuffix
            }

            is ParameterValue.IntegerValue -> {
                numVal = value.value.toDouble()
                unit = defaultSuffix
            }

            else -> return MessageResult.Error("Invalid content type")
        }

        val range = ranges[unit] ?: return MessageResult.Error("Invalid unit for command: $unit")

        if (numVal < range.first || numVal > range.second) {
            return MessageResult.Error("Value out of range ($numVal $unit is not within [${range.first}, ${range.second}])")
        }

        val valueStr = if (numVal % 1.0 == 0.0) numVal.toLong().toString() else numVal.toString()

        connection.send("$command$valueStr$unit").onFailure { return MessageResult.Error(it) }

        return standardStringInterrogate(command)
    }

    private suspend fun writeIntCommand(
        command: String,
        value: ParameterValue,
        min: Int? = null,
        max: Int? = null,
        interrogate: Boolean = true,
        interrogateParam: String = command
    ): MessageResult<ParameterValue> {
        val intValue = when (value) {
            is ParameterValue.IntegerValue -> value.value
            is ParameterValue.DoubleValue -> value.value.toInt()
            is ParameterValue.StringValue -> value.value.toIntOrNull()
                ?: return MessageResult.Error("Invalid integer format")

            else -> return MessageResult.Error("Invalid content type")
        }

        if (min != null && intValue < min) return MessageResult.Error("Value below minimum $min")
        if (max != null && intValue > max) return MessageResult.Error("Value above maximum $max")

        connection.send("$command$intValue").onFailure { return MessageResult.Error(it) }

        return if (interrogate) standardIntInterrogate(command, interrogateParam) else MessageResult.Result(
            ParameterValue.IntegerValue(intValue)
        )
    }

    private suspend fun writeStringCommand(
        command: String,
        value: ParameterValue,
        quote: Boolean = false,
        interrogate: Boolean = true
    ): MessageResult<ParameterValue> {
        val textValue =
            (value as? ParameterValue.StringValue)?.value ?: return MessageResult.Error("Invalid string type")
        val payload = if (quote) "'$textValue'" else textValue

        connection.send("$command$payload").onFailure { return MessageResult.Error(it) }

        return if (interrogate) standardStringInterrogate(command) else MessageResult.Result(value)
    }

    private suspend fun writeActionCommand(
        command: String,
        value: ParameterValue? = null
    ): MessageResult<ParameterValue> {
        val payload = when (value) {
            is ParameterValue.IntegerValue -> value.value.toString()
            is ParameterValue.DoubleValue -> value.value.toInt().toString()
            is ParameterValue.StringValue -> value.value
            null -> ""
            else -> return MessageResult.Error("Invalid format")
        }

        connection.send("$command$payload").onFailure { return MessageResult.Error(it) }

        return MessageResult.Result(ParameterValue.IntegerValue(1))
    }
}
