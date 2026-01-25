package com.labeq.builtin.drivers.hp.hp3325

import com.labeq.device.DeviceDriver
import com.labeq.transport.*
import kotlinx.coroutines.runBlocking

class HP3325Driver(deviceConnection: DeviceConnection) : DeviceDriver(deviceConnection) {
    private companion object {
        val FREQUENCY_UNITS = listOf<String>("HZ", "KH", "MH");

        const val INTERROGATE = "?"

        // Signal generation
        const val AMPLITUDE = "AM"
        const val FREQUENCY = "FR"
        const val WAVEFORM_FUNCTION = "FU"

        const val HV_MODE = "HV"
        const val HV_MODE_QUERY = "IHV" // Legacy HV mode query, HP3325A compatible

        const val DC_OFFSET = "OF"

        const val PHASE = "PH" // Set the phase of the main signal
        const val ZERO_PHASE = "AP" // Assign zero phase

        const val REAR_FRONT_OUTPUT = "RF"

        // Sweep-related
        const val SWEEP_START = "ST" // Start frequency
        const val SWEEP_STOP = "SP" // Stop frequency
        const val SWEEP_TIME = "TI" // Sweep time
        const val SWEEP_MODE = "SM" // Sweep mode

        const val DISC_SWEEP_CLEAR = "DCLR" // Discrete sweep clear
        const val DISC_SWEEP_STORE = "DSTO" // Discrete sweep store (B only)
        const val DISC_SWEEP_RECALL = "DRCL" // Discrete sweep recall (B only)

        const val START_SINGLE_SWEEP = "SS"
        const val RESET_SINGLE_SWEEP = "RSW" // (B only)

        const val START_CONTINUOUS_SWEEP = "SC"

        // Marker / auxiliary
        const val MARKER_FREQ = "MF" // Marker frequency

        // Modulation
        const val MODULATION_SOURCE_AMPLITUDE = "MOAM" // (B only)
        const val MODULATION_SOURCE_ARBITRARY_DATA = "MOAR" // (B only)
        const val MODULATION_SOURCE_FREQUENCY = "MOFR" // (B only)
        const val MODULATION_SOURCE_FUNCTION = "MOFU" // (B only)

        const val AMPLITUDE_MODULATION = "MA"
        const val PHASE_MODULATION = "MP"

        // Memory / states
        const val STORE_STATE = "SR"      // Store state
        const val RECALL_STATE = "RE"     // Recall state (RE<X>)

        // Status / errors / self test
        const val AMPLITUDE_CAL = "AC"    // Amplitude calibration
        const val CAL_MODE = "CALM"       // Calibration mode

        const val ENHANCEMENTS = "ENH" // Compatibility mode on/off (B only)

        const val ERROR = "ERR" // Three-digit error code (B only)
        const val ERROR_INT = "IER" // One-digit error code

        const val EXT_REF_LOCKED = "EXTR" // Check if the reference oscillator is locked to an external input (B only)

        const val LOCAL_ENABLE = "LCL" // Return to local mode
        const val REMOTE_ENABLE = "RMT" // Return to remote mode
        const val RESET_DEVICE = "*RST"

        const val STATUS_BYTE_MASK = "MS" // Set status byte mask, alpha version of ESTB
        const val SRQ_ENABLE = "ESTB" // Sets status byte mask to cause SRQ (B only)
        const val STATUS_BYTE_QUERY = "QSTB" // (B only)

        const val ID = "ID" // Plain identification (B only)
        const val IDN = "*IDN" // IEEE-488.2 identification (B only)
        const val OPTION = "OPT" // Returns a list of options the device is equipped with (B only)

        // Communication
        const val ECHO = "ECHO"           // Echo on/off (B only)
        const val RES_HEAD = "HEAD" // Controls the presence of the alpha headers (B only)

        const val DATA_TRANSFER_MODE = "MD" // HP-IB/GPIB data transfer mode

        // Display
        const val DISPLAY_TOGGLE = "DISP" // Display on/off (B only)
        const val DISPLAY_STRING = "DSP" // Display string (B only)
    }

    override suspend fun queryParameter(
        parameter: String, returnType: ReturnType
    ): MessageResult<ParameterValue> {
        return when (parameter) {
            HP3325Device.PARAMETER_GENERATOR_FREQUENCY -> standardStringInterrogate(FREQUENCY)
            HP3325Device.PARAMETER_GENERATOR_AMPLITUDE -> standardStringInterrogate(AMPLITUDE)
            HP3325Device.PARAMETER_GENERATOR_FUNCTION -> standardIntInterrogate(WAVEFORM_FUNCTION)
            HP3325Device.PARAMETER_GENERATOR_PHASE -> standardStringInterrogate(PHASE)
            HP3325Device.PARAMETER_GENERATOR_HV -> standardStringInterrogate(HV_MODE)
            HP3325Device.PARAMETER_GENERATOR_ZERO_PHASE -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_GENERATOR_OFFSET -> standardStringInterrogate(DC_OFFSET)
            HP3325Device.PARAMETER_GENERATOR_OUTPUT_SELECT -> standardIntInterrogate(REAR_FRONT_OUTPUT)

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
            HP3325Device.PARAMETER_ERROR -> standardIntInterrogate(ERROR)

            HP3325Device.PARAMETER_EXTERNAL_REFERENCE_LOCK -> standardIntInterrogate(EXT_REF_LOCKED)

            HP3325Device.PARAMETER_LOCAL_ENABLE -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_REMOTE_ENABLE -> MessageResult.Error("Write-only parameter.")

            HP3325Device.PARAMETER_RESET_DEVICE -> MessageResult.Error("Write-only parameter.")

            HP3325Device.PARAMETER_BYTE_MASK -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_SRQ_ENABLE -> standardStringInterrogate(SRQ_ENABLE)
            HP3325Device.PARAMETER_STATUS_BYTE -> standardStringInterrogate(STATUS_BYTE_QUERY)

            HP3325Device.PARAMETER_IDENTIFICATION_STANDARD -> standardStringInterrogate(ID)
            HP3325Device.PARAMETER_IDENTIFICATION_488_2 -> standardStringInterrogate(IDN)

            HP3325Device.PARAMETER_OPTIONS -> standardStringInterrogate(OPTION)

            HP3325Device.PARAMETER_RS232_ECHO -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_RES_HEAD -> MessageResult.Error("Write-only parameter.")

            HP3325Device.PARAMETER_DATA_TRANSFER_MODE -> standardIntInterrogate(DATA_TRANSFER_MODE)

            HP3325Device.PARAMETER_DISPLAY_ENABLE -> MessageResult.Error("Write-only parameter.")
            HP3325Device.PARAMETER_DISPLAY_STRING -> MessageResult.Error("Write-only parameter.")

            else -> MessageResult.Error("Invalid parameter: $parameter")
        }
    }

    override suspend fun writeParameter(
        parameter: String, value: ParameterValue, returnType: ReturnType
    ): MessageResult<ParameterValue> {
        return when (parameter) {
            HP3325Device.PARAMETER_GENERATOR_FREQUENCY -> writeFrequency(value)

            else -> MessageResult.Error("Invalid parameter: $parameter")
        }
    }


    override suspend fun writeData(
        data: ByteArray, returnType: ReturnType
    ): MessageResult<ParameterValue> {
        TODO("Not yet implemented")
    }

    override suspend fun writeData(
        data: String, returnType: ReturnType
    ): MessageResult<ParameterValue> {
        TODO("Not yet implemented")
    }

    override fun connect(): Boolean {
        return runBlocking {
            if (!connection.open()) return@runBlocking false
            connection.send(REMOTE_ENABLE)
            connection.send("DISP1")
//            connection.send(RESET_DEVICE)
            connection.query(AMPLITUDE + INTERROGATE, -1).onSuccess {
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

    // --- Generic helpers ---
    private fun stripPrefix(response: String, prefix: String): String = response.removePrefix(prefix).trim()

    private suspend fun standardStringInterrogate(parameter: String): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(parameter + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            return MessageResult.Result(ParameterValue.StringValue(stripPrefix(it.value, parameter)))
        }
        return MessageResult.Error("Unknown error")
    }

    private suspend fun standardIntInterrogate(parameter: String): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(parameter + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            return MessageResult.Result(
                ParameterValue.IntegerValue(stripPrefix(it.value, parameter).toInt())
            )
        }
        return MessageResult.Error("Unknown error")
    }

    private suspend fun writeFrequency(value: ParameterValue): MessageResult<ParameterValue.StringValue> {
        var frequency: Double;
        var unit: String;
        when (value) {
            is ParameterValue.StringValue -> {
                val frequencyRegex = "(?<frequency>[0-9]+\\.?[0-9]*)(?<unit>[a-zA-Z]{2})".toRegex();
                val match = frequencyRegex.matchEntire(value.value) ?: return MessageResult.Error("Invalid frequency: $value");

                unit = match.groupValues[2].uppercase()
                if (!FREQUENCY_UNITS.contains(unit)) return MessageResult.Error("Invalid unit: $unit")
                frequency = match.groupValues[1].toDouble()
            }
            is ParameterValue.DoubleValue -> {
                frequency = value.value;
                unit = "HZ";
            }
            else -> return MessageResult.Error("Invalid content type")
        }

        val normalizedFrequency = when (unit) {
            "HZ" -> frequency;
            "KH" -> frequency * 1000;
            "MH" -> frequency * 1000000;
            else -> return MessageResult.Error("Invalid unit: $unit")
        }

        if (normalizedFrequency <= 0 || normalizedFrequency > 60000000) return MessageResult.Error("Frequency out of range")

        connection.send(FREQUENCY + normalizedFrequency + "HZ").onSuccess {
            return standardStringInterrogate(FREQUENCY)
        }.onFailure { return MessageResult.Error(it) }

        return MessageResult.Error("Unknown error")
    }

}
