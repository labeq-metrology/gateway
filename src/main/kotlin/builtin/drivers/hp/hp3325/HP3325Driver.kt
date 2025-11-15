package com.labeq.builtin.drivers.hp.hp3325

import com.labeq.device.DeviceDriver
import com.labeq.transport.*
import kotlinx.coroutines.runBlocking

class HP3325Driver(deviceConnection: DeviceConnection) : DeviceDriver(deviceConnection) {
    private companion object {
        // core / already present (kept for continuity)
        const val REMOTE_ENABLE = "RMT"
        const val LOCAL_ENABLE = "LCL"
        const val RESET_DEVICE = "RST"

        const val AMPLITUDE = "AM"
        const val FREQUENCY = "FR"
        const val FUNCTION = "FU"

        const val DISPLAY_STRING = "DSP"
        const val INTERROGATE = "?"

        // extended command tokens (from command reference)
        const val OFFSET = "OF"           // DC offset
        const val PHASE = "PH"            // Phase
        const val WAVEFORM = "WV"         // Waveform shape selection (SINE, SQR, TRI, RAMP, PULSE, etc.)

        // Sweep-related
        const val SWEEP_START = "ST"      // Start frequency
        const val SWEEP_STOP = "SP"       // Stop frequency
        const val SWEEP_TIME = "TM"       // Sweep time
        const val SWEEP_MODE = "SW"       // Sweep mode (LIN/LOG/DIS)
        const val SWEEP_TRIGGER = "TR"    // Sweep trigger/control (TRIG, CONT, SINGLE) - verify token
        const val SWEEP_RESET = "SR"      // Reset/start sweep (verify token in manual)

        // Marker / auxiliary
        const val MARKER = "MK"           // Marker frequency / marker control
        const val MARKER_CF = "MCF"       // Marker + center freq (Mkr+CF) - example token

        // Modulation
        const val MOD_ENABLE = "MD"       // Modulation enable
        const val MOD_SOURCE = "MS"       // Modulation source
        const val AM_DEPTH = "AD"         // AM depth
        const val FM_DEV = "FD"           // FM deviation
        const val PM_DEVIATION = "PD"     // PM deviation

        // Output / amplitude units
        const val AMP_UNITS = "AU"        // Amplitude units (VR, MR, DB, etc.) - verify token

        // Memory / states
        const val STORE_STATE = "SS"      // Store state
        const val RECALL_STATE = "RS"     // Recall state
        const val MEMORY_CLEAR = "MC"     // Memory clear (beware MC clash with other tokens — verify)

        // Status / errors / self test
        const val SELF_TEST = "TST"       // Self test / test
        const val ERROR_QUERY = "ERR"     // Error query (often "ERR?" or "ER?") - verify
        const val STATUS_QUERY = "STB"    // Status byte / status query token (STB is common) - verify

        // RS-232 / misc
        const val ECHO = "ECHO"           // Echo on/off
        const val ADDRESS = "ADDR"        // HP-IB address set/read (or sometimes "A")
        const val DISPLAY_CLEAR = "DSPCLR"// Clear display (example token)
        const val AUX_OUTPUT = "AX"       // Auxiliary output control (placeholder)
    }

    override suspend fun queryParameter(
        parameter: String, returnType: ReturnType
    ): MessageResult<ParameterValue> {
        return when (parameter) {

            // generator
            HP3325Device.PARAMETER_GENERATOR_FREQUENCY -> readFrequency()
            HP3325Device.PARAMETER_GENERATOR_AMPLITUDE -> readAmplitude()
            HP3325Device.PARAMETER_GENERATOR_FUNCTION -> readFunction()
            HP3325Device.PARAMETER_GENERATOR_WAVEFORM -> readWaveform()
            HP3325Device.PARAMETER_GENERATOR_OFFSET -> readOffset()
            HP3325Device.PARAMETER_GENERATOR_PHASE -> readPhase()

            // sweep
            HP3325Device.PARAMETER_SWEEP_START -> readSweepStart()
            HP3325Device.PARAMETER_SWEEP_STOP -> readSweepStop()
            HP3325Device.PARAMETER_SWEEP_TIME -> readSweepTime()
            HP3325Device.PARAMETER_SWEEP_MODE -> readSweepMode()

            // modulation
            HP3325Device.PARAMETER_MOD_ENABLED -> readModulationEnable()
            HP3325Device.PARAMETER_MOD_SOURCE -> readModulationSource()
            HP3325Device.PARAMETER_MOD_AM_DEPTH -> readAMDepth()
            HP3325Device.PARAMETER_MOD_FM_DEVIATION -> readFMDeviation()
            HP3325Device.PARAMETER_MOD_PM_DEVIATION -> readPMDeviation()

            HP3325Device.PARAMETER_MEMORY_STORE ->
                MessageResult.Error("Parameter $parameter is write-only")

            HP3325Device.PARAMETER_MEMORY_RECALL ->
                MessageResult.Error("Parameter $parameter is write-only")

            // status
            HP3325Device.PARAMETER_DEVICE_ERROR -> queryError()
            HP3325Device.PARAMETER_DEVICE_STATUS -> queryStatusByte()

            // display
            HP3325Device.PARAMETER_DISPLAY_TEXT ->
                MessageResult.Error("Display text is write-only")

            else -> MessageResult.Error("Invalid parameter: $parameter")
        }
    }

    override suspend fun writeParameter(
        parameter: String, value: ParameterValue, returnType: ReturnType
    ): MessageResult<ParameterValue> {
        return when (parameter) {

            // generator
            HP3325Device.PARAMETER_GENERATOR_FREQUENCY ->
                setFrequency(value as ParameterValue.StringValue)

            HP3325Device.PARAMETER_GENERATOR_AMPLITUDE ->
                setAmplitudeWithUnit(value as ParameterValue.StringValue)

            HP3325Device.PARAMETER_GENERATOR_FUNCTION ->
                setFunction(value as ParameterValue.IntegerValue)

            HP3325Device.PARAMETER_GENERATOR_WAVEFORM ->
                setWaveform(value as ParameterValue.StringValue)

            HP3325Device.PARAMETER_GENERATOR_OFFSET ->
                setOffset(value as ParameterValue.StringValue)

            HP3325Device.PARAMETER_GENERATOR_PHASE ->
                setPhase(value as ParameterValue.StringValue)

            // sweep
            HP3325Device.PARAMETER_SWEEP_START ->
                setSweepStart(value as ParameterValue.StringValue)

            HP3325Device.PARAMETER_SWEEP_STOP ->
                setSweepStop(value as ParameterValue.StringValue)

            HP3325Device.PARAMETER_SWEEP_TIME ->
                setSweepTime(value as ParameterValue.StringValue)

            HP3325Device.PARAMETER_SWEEP_MODE ->
                setSweepMode(value as ParameterValue.StringValue)

            HP3325Device.PARAMETER_SWEEP_TRIGGER -> {
                triggerSweepSingle()
            }

            // modulation
            HP3325Device.PARAMETER_MOD_ENABLED ->
                setModulationEnable((value as ParameterValue.IntegerValue).value != 0)

            HP3325Device.PARAMETER_MOD_SOURCE ->
                setModulationSource(value as ParameterValue.StringValue)

            HP3325Device.PARAMETER_MOD_AM_DEPTH ->
                setAMDepth(value as ParameterValue.StringValue)

            HP3325Device.PARAMETER_MOD_FM_DEVIATION ->
                setFMDeviation(value as ParameterValue.StringValue)

            HP3325Device.PARAMETER_MOD_PM_DEVIATION ->
                setPMDeviation(value as ParameterValue.StringValue)

            // memory
            HP3325Device.PARAMETER_MEMORY_STORE ->
                storeState((value as ParameterValue.IntegerValue).value)

            HP3325Device.PARAMETER_MEMORY_RECALL ->
                recallState((value as ParameterValue.IntegerValue).value)

            // display
            HP3325Device.PARAMETER_DISPLAY_TEXT ->
                setDisplayText((value as ParameterValue.StringValue).value)

            else ->
                MessageResult.Error("Invalid parameter: $parameter")
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
            connection.send(RESET_DEVICE)
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

    // Wklej poniżej do klasy HP3325Driver (po istniejących metodach).
// Źródła: przesłany manual (Remote/Command Reference) i dokumenty online dotyczące HP3325B.
// :contentReference[oaicite:2]{index=2} :contentReference[oaicite:3]{index=3}

    // --- Generic helpers ---
    private fun stripPrefix(response: String, prefix: String): String =
        response.removePrefix(prefix).trim()

    // -------------------------
    // Frequency (FR)
    // -------------------------
    suspend fun setFrequency(freq: ParameterValue.StringValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        val freqRegex = Regex("^[0-9]*\\.?[0-9]+(HZ|KH|MH|GH)\$", RegexOption.IGNORE_CASE)
        if (!freqRegex.matches(freq.value)) return MessageResult.Error("Invalid frequency format: ${freq.value}")
        // Validate numeric frequency within instrument limits (0 - 60 MHz approx)
        val numeric = freq.value.dropLastWhile { it.isLetter() }.toDoubleOrNull()
            ?: return MessageResult.Error("Can't parse frequency")
        if (numeric < 0.0 || numeric > 60999999.999) return MessageResult.Error("Frequency out of range: $numeric")
        connection.send(FREQUENCY + freq.value)
        return readFrequency()
    }

    suspend fun readFrequency(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(FREQUENCY + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            // manual often returns "FRxxxKHZ" or similar
            return MessageResult.Result(ParameterValue.StringValue(stripPrefix(it.value, FREQUENCY)))
        }
        return MessageResult.Error("Unknown error")
    }

    // -------------------------
    // Amplitude (AM) - already implemented above, but include units helper
    // -------------------------
    suspend fun setAmplitudeWithUnit(amplitude: ParameterValue.StringValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        // Accept formats like "1.000VR", "1000MV", "10.0DB"
        val ampRegex = Regex("^[0-9]*\\.?[0-9]+(VR|MR|MV|DB|DV|VO)\$", RegexOption.IGNORE_CASE)
        if (!ampRegex.matches(amplitude.value)) return MessageResult.Error("Invalid amplitude format: ${amplitude.value}")
        val numeric = amplitude.value.dropLastWhile { it.isLetter() }.toDoubleOrNull()
            ?: return MessageResult.Error("Can't parse amplitude")
        // Range example: 0..40 Vpp typical? Use safe range check (0..40)
        if (numeric < 0.0 || numeric > 40.0) return MessageResult.Error("Amplitude out of range: $numeric")
        connection.send(AMPLITUDE + amplitude.value)
        return readAmplitude()
    }

    suspend fun readAmplitude(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(AMPLITUDE + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            return MessageResult.Result(ParameterValue.StringValue(stripPrefix(it.value, AMPLITUDE)))
        }
        return MessageResult.Error("Unknown error")
    }

    // -------------------------
    // DC Offset (OF) - already exampled earlier; provide concise version
    // -------------------------
    suspend fun setOffset(offset: ParameterValue.StringValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        val regex = Regex("^[-+]?[0-9]*\\.?[0-9]+(V|MV|UV)?\$", RegexOption.IGNORE_CASE)
        if (!regex.matches(offset.value)) return MessageResult.Error("Invalid offset format: ${offset.value}")
        val numeric = offset.value.dropLastWhile { it.isLetter() }.toDoubleOrNull()
            ?: return MessageResult.Error("Can't parse offset")
        if (numeric < -20.0 || numeric > 20.0) return MessageResult.Error("Offset out of range: $numeric")
        connection.send(OFFSET + offset.value)
        return readOffset()
    }

    suspend fun readOffset(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(OFFSET + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            return MessageResult.Result(ParameterValue.StringValue(stripPrefix(it.value, OFFSET)))
        }
        return MessageResult.Error("Unknown error")
    }

    // -------------------------
    // Function / Waveform (FU / WV)
    // -------------------------
    suspend fun setFunction(function: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        // manual maps functions 0..5 to SINE/TRI/SQ/PULSE/RAMP/DC maybe - keep check 0..5
        if (function.value !in 0..5) return MessageResult.Error("Invalid function: ${function.value}")
        connection.send(FUNCTION + function.value)
        return readFunction()
    }

    suspend fun readFunction(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(FUNCTION + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            val v = stripPrefix(it.value, FUNCTION).toIntOrNull()
                ?: return MessageResult.Error("Invalid function response: ${it.value}")
            return MessageResult.Result(ParameterValue.IntegerValue(v))
        }
        return MessageResult.Error("Unknown error")
    }

    // Waveform textual selector (alternative on some firmwares)
    suspend fun setWaveform(shape: ParameterValue.StringValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        val allowed = setOf("SINE", "SIN", "SQR", "SQUARE", "TRI", "TRIANGLE", "RAMP", "PULSE", "DC")
        if (shape.value.uppercase() !in allowed) return MessageResult.Error("Invalid waveform: ${shape.value}")
        connection.send(WAVEFORM + shape.value.uppercase())
        return readWaveform()
    }

    suspend fun readWaveform(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(WAVEFORM + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            return MessageResult.Result(ParameterValue.StringValue(stripPrefix(it.value, WAVEFORM)))
        }
        return MessageResult.Error("Unknown error")
    }

    // -------------------------
    // Phase
    // -------------------------
    suspend fun setPhase(phase: ParameterValue.StringValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        val regex = Regex("^[-+]?[0-9]*\\.?[0-9]+(DEG)?\$", RegexOption.IGNORE_CASE)
        if (!regex.matches(phase.value)) return MessageResult.Error("Invalid phase format: ${phase.value}")
        val numeric = phase.value.dropLastWhile { it.isLetter() }.toDoubleOrNull()
            ?: return MessageResult.Error("Can't parse phase")
        if (numeric < -360.0 || numeric > 360.0) return MessageResult.Error("Phase out of range: $numeric")
        connection.send(PHASE + phase.value)
        return readPhase()
    }

    suspend fun readPhase(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(PHASE + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            return MessageResult.Result(ParameterValue.StringValue(stripPrefix(it.value, PHASE)))
        }
        return MessageResult.Error("Unknown error")
    }

    // -------------------------
    // Sweep controls (start/stop/time/mode/trigger)
    // -------------------------
    suspend fun setSweepStart(start: ParameterValue.StringValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        val freqRegex = Regex("^[0-9]*\\.?[0-9]+(HZ|KH|MH|GH)\$", RegexOption.IGNORE_CASE)
        if (!freqRegex.matches(start.value)) return MessageResult.Error("Invalid start frequency format: ${start.value}")
        connection.send(SWEEP_START + start.value)
        return readSweepStart()
    }

    suspend fun readSweepStart(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(SWEEP_START + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            return MessageResult.Result(ParameterValue.StringValue(stripPrefix(it.value, SWEEP_START)))
        }
        return MessageResult.Error("Unknown error")
    }

    suspend fun setSweepStop(stop: ParameterValue.StringValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        val freqRegex = Regex("^[0-9]*\\.?[0-9]+(HZ|KH|MH|GH)\$", RegexOption.IGNORE_CASE)
        if (!freqRegex.matches(stop.value)) return MessageResult.Error("Invalid stop frequency format: ${stop.value}")
        connection.send(SWEEP_STOP + stop.value)
        return readSweepStop()
    }

    suspend fun readSweepStop(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(SWEEP_STOP + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            return MessageResult.Result(ParameterValue.StringValue(stripPrefix(it.value, SWEEP_STOP)))
        }
        return MessageResult.Error("Unknown error")
    }

    suspend fun setSweepTime(time: ParameterValue.StringValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        val timeRegex = Regex("^[0-9]*\\.?[0-9]+(S|MS|US|MINS)?\$", RegexOption.IGNORE_CASE)
        if (!timeRegex.matches(time.value)) return MessageResult.Error("Invalid sweep time format: ${time.value}")
        connection.send(SWEEP_TIME + time.value)
        return readSweepTime()
    }

    suspend fun readSweepTime(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(SWEEP_TIME + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            return MessageResult.Result(ParameterValue.StringValue(stripPrefix(it.value, SWEEP_TIME)))
        }
        return MessageResult.Error("Unknown error")
    }

    suspend fun setSweepMode(mode: ParameterValue.StringValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        val m = mode.value.uppercase()
        val allowed = setOf("LIN", "LOG", "DIS", "CONT")
        if (m !in allowed) return MessageResult.Error("Invalid sweep mode: ${mode.value}")
        connection.send(SWEEP_MODE + m)
        return readSweepMode()
    }

    suspend fun readSweepMode(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(SWEEP_MODE + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            return MessageResult.Result(ParameterValue.StringValue(stripPrefix(it.value, SWEEP_MODE)))
        }
        return MessageResult.Error("Unknown error")
    }

    suspend fun triggerSweepSingle(): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        // Single sweep trigger may be e.g. "TRIG" or "SR" - using SWEEP_TRIGGER + "SINGLE" as general form
        connection.send(SWEEP_TRIGGER + "SINGLE")
        return MessageResult.Result(ParameterValue.StringValue("OK"))
    }

    // -------------------------
    // Marker
    // -------------------------
    suspend fun setMarker(freq: ParameterValue.StringValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        val freqRegex = Regex("^[0-9]*\\.?[0-9]+(HZ|KH|MH)\$", RegexOption.IGNORE_CASE)
        if (!freqRegex.matches(freq.value)) return MessageResult.Error("Invalid marker frequency format: ${freq.value}")
        connection.send(MARKER + freq.value)
        return readMarker()
    }

    suspend fun readMarker(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(MARKER + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            return MessageResult.Result(ParameterValue.StringValue(stripPrefix(it.value, MARKER)))
        }
        return MessageResult.Error("Unknown error")
    }

    // -------------------------
    // Modulation (AM/FM/PM)
    // -------------------------
    suspend fun setModulationEnable(enabled: Boolean): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.send(MOD_ENABLE + (if (enabled) "1" else "0"))
        return readModulationEnable()
    }

    suspend fun readModulationEnable(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(MOD_ENABLE + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            val i = stripPrefix(it.value, MOD_ENABLE).toIntOrNull()
                ?: return MessageResult.Error("Invalid modulation response: ${it.value}")
            return MessageResult.Result(ParameterValue.IntegerValue(i))
        }
        return MessageResult.Error("Unknown error")
    }

    suspend fun setModulationSource(src: ParameterValue.StringValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        val allowed = setOf("INT", "EXT", "MAN") // check manual for exact tokens
        val s = src.value.uppercase()
        if (s !in allowed) return MessageResult.Error("Invalid modulation source: ${src.value}")
        connection.send(MOD_SOURCE + s)
        return readModulationSource()
    }

    suspend fun readModulationSource(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(MOD_SOURCE + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            return MessageResult.Result(ParameterValue.StringValue(stripPrefix(it.value, MOD_SOURCE)))
        }
        return MessageResult.Error("Unknown error")
    }

    suspend fun setAMDepth(depth: ParameterValue.StringValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        val regex = Regex("^[0-9]*\\.?[0-9]+%?\$")
        if (!regex.matches(depth.value)) return MessageResult.Error("Invalid AM depth: ${depth.value}")
        val numeric =
            depth.value.removeSuffix("%").toDoubleOrNull() ?: return MessageResult.Error("Can't parse AM depth")
        if (numeric !in 0.0..100.0) return MessageResult.Error("AM depth out of range: $numeric")
        connection.send(AM_DEPTH + depth.value)
        return readAMDepth()
    }

    suspend fun readAMDepth(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(AM_DEPTH + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            return MessageResult.Result(ParameterValue.StringValue(stripPrefix(it.value, AM_DEPTH)))
        }
        return MessageResult.Error("Unknown error")
    }

    // FM / PM
    suspend fun setFMDeviation(dev: ParameterValue.StringValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        val regex = Regex("^[0-9]*\\.?[0-9]+(HZ|KH|MH|DEG)?\$", RegexOption.IGNORE_CASE)
        if (!regex.matches(dev.value)) return MessageResult.Error("Invalid FM/PM deviation: ${dev.value}")
        connection.send(FM_DEV + dev.value)
        return readFMDeviation()
    }

    suspend fun readFMDeviation(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(FM_DEV + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            return MessageResult.Result(ParameterValue.StringValue(stripPrefix(it.value, FM_DEV)))
        }
        return MessageResult.Error("Unknown error")
    }

    suspend fun setPMDeviation(dev: ParameterValue.StringValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        val regex = Regex("^[0-9]*\\.?[0-9]+(DEG)?\$", RegexOption.IGNORE_CASE)
        if (!regex.matches(dev.value)) return MessageResult.Error("Invalid PM deviation: ${dev.value}")
        connection.send(PM_DEVIATION + dev.value)
        return readPMDeviation()
    }

    suspend fun readPMDeviation(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(PM_DEVIATION + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            return MessageResult.Result(ParameterValue.StringValue(stripPrefix(it.value, PM_DEVIATION)))
        }
        return MessageResult.Error("Unknown error")
    }

    // -------------------------
    // Memory / Store / Recall
    // -------------------------
    suspend fun storeState(slot: Int): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        if (slot !in 0..9) return MessageResult.Error("Invalid memory slot: $slot")
        connection.send(STORE_STATE + slot)
        return MessageResult.Result(ParameterValue.StringValue("OK"))
    }

    suspend fun recallState(slot: Int): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        if (slot !in 0..9) return MessageResult.Error("Invalid memory slot: $slot")
        connection.send(RECALL_STATE + slot)
        return MessageResult.Result(ParameterValue.StringValue("OK"))
    }

    suspend fun clearMemory(): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.send(MEMORY_CLEAR)
        return MessageResult.Result(ParameterValue.StringValue("OK"))
    }

    // -------------------------
    // Diagnostics / Errors / Status
    // -------------------------
    suspend fun runSelfTest(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.send(SELF_TEST)
        connection.query(SELF_TEST + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid self-test response $it")
            return MessageResult.Result(ParameterValue.StringValue(stripPrefix(it.value, SELF_TEST)))
        }
        return MessageResult.Error("Unknown error")
    }

    suspend fun queryError(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        // many manuals use "ERR?" or "ER?" - we accept ERR here; adapt if necessary
        connection.query(ERROR_QUERY + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid error response $it")
            return MessageResult.Result(ParameterValue.StringValue(stripPrefix(it.value, ERROR_QUERY)))
        }
        return MessageResult.Error("Unknown error")
    }

    suspend fun queryStatusByte(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        // STB / STATUS query token varies; many references use "STB?" for status byte
        connection.query(STATUS_QUERY + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid status response $it")
            return MessageResult.Result(ParameterValue.StringValue(stripPrefix(it.value, STATUS_QUERY)))
        }
        return MessageResult.Error("Unknown error")
    }

    // -------------------------
    // RS-232 helpers (ECHO, ADDRESS)
    // -------------------------
    suspend fun setEcho(enabled: Boolean): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.send(ECHO + (if (enabled) "1" else "0"))
        return MessageResult.Result(ParameterValue.StringValue("OK"))
    }

    suspend fun setAddress(addr: Int): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        if (addr !in 0..31) return MessageResult.Error("Invalid HP-IB address: $addr")
        connection.send(ADDRESS + addr)
        return MessageResult.Result(ParameterValue.StringValue("OK"))
    }

    // -------------------------
    // Display helpers
    // -------------------------
    suspend fun setDisplayText(text: String): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        val safe = text.replace("'", "\"")
        connection.send("$DISPLAY_STRING'$safe'")
        return MessageResult.Result(ParameterValue.StringValue("OK"))
    }

    suspend fun clearDisplay(): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.send(DISPLAY_CLEAR)
        return MessageResult.Result(ParameterValue.StringValue("OK"))
    }

    // -------------------------
    // Final note: TBD / TODO
    // -------------------------
    // - Verify exact tokens for: ERROR_QUERY (ERR/ER), STATUS_QUERY (STB/ST?), SWEEP_TRIGGER/SWEEP_RESET token names.
    // - If instrument responds with prefixed codes (e.g. "FR1000HZ"), parsing uses removePrefix; adapt if it returns "1000HZ" only.

}
