package com.labeq.builtin.drivers.hp.hp3325

import com.labeq.communication.MQTTEventBus
import com.labeq.device.DeviceDriver
import com.labeq.device.generic.*
import com.labeq.transport.MessageResult
import com.labeq.transport.ParameterValue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class HP3325Device(
    eventBus: MQTTEventBus,
    name: String,
    manufacturer: String,
    model: String,
    uuid: Uuid,
    driver: DeviceDriver,
) : GenericDevice(eventBus, name, manufacturer, model, uuid, driver) {

    companion object {
        // Basic generator parameters
        const val PARAMETER_GENERATOR_FREQUENCY = "generator.frequency"
        const val PARAMETER_GENERATOR_AMPLITUDE = "generator.amplitude"
        const val PARAMETER_GENERATOR_FUNCTION  = "generator.function"
        const val PARAMETER_GENERATOR_WAVEFORM  = "generator.waveform"
        const val PARAMETER_GENERATOR_OFFSET    = "generator.offset"
        const val PARAMETER_GENERATOR_PHASE     = "generator.phase"

        // Sweep parameters
        const val PARAMETER_SWEEP_START  = "sweep.start"
        const val PARAMETER_SWEEP_STOP   = "sweep.stop"
        const val PARAMETER_SWEEP_TIME   = "sweep.time"
        const val PARAMETER_SWEEP_MODE   = "sweep.mode"
        const val PARAMETER_SWEEP_TRIGGER = "sweep.trigger"  // stateless, setter only

        // Modulation
        const val PARAMETER_MOD_ENABLED     = "modulation.enabled"
        const val PARAMETER_MOD_SOURCE      = "modulation.source"
        const val PARAMETER_MOD_AM_DEPTH    = "modulation.am.depth"
        const val PARAMETER_MOD_FM_DEVIATION= "modulation.fm.deviation"
        const val PARAMETER_MOD_PM_DEVIATION= "modulation.pm.deviation"

        // Memory / state control
        const val PARAMETER_MEMORY_STORE  = "memory.store"
        const val PARAMETER_MEMORY_RECALL = "memory.recall"

        // Status / error / display
        const val PARAMETER_DEVICE_ERROR  = "device.error"
        const val PARAMETER_DEVICE_STATUS = "device.status"
        const val PARAMETER_DISPLAY_TEXT  = "display.text"
    }

    override val parameters: MutableMap<String, ParameterValue> = mutableMapOf(
        // generator core
        PARAMETER_GENERATOR_FREQUENCY to ParameterValue.StringValue("0HZ"),
        PARAMETER_GENERATOR_AMPLITUDE to ParameterValue.StringValue("0.001VO"),
        PARAMETER_GENERATOR_FUNCTION  to ParameterValue.IntegerValue(0),
        PARAMETER_GENERATOR_WAVEFORM  to ParameterValue.StringValue("SINE"),
        PARAMETER_GENERATOR_OFFSET    to ParameterValue.StringValue("0V"),
        PARAMETER_GENERATOR_PHASE     to ParameterValue.StringValue("0DEG"),

        // sweep
        PARAMETER_SWEEP_START  to ParameterValue.StringValue("1KH"),
        PARAMETER_SWEEP_STOP   to ParameterValue.StringValue("10KH"),
        PARAMETER_SWEEP_TIME   to ParameterValue.StringValue("1S"),
        PARAMETER_SWEEP_MODE   to ParameterValue.StringValue("LIN"),

        // sweep.trigger – brak domyślnej wartości (stateless)
        PARAMETER_SWEEP_TRIGGER to ParameterValue.StringValue(""),

        // modulation
        PARAMETER_MOD_ENABLED      to ParameterValue.IntegerValue(0),
        PARAMETER_MOD_SOURCE       to ParameterValue.StringValue("INT"),
        PARAMETER_MOD_AM_DEPTH     to ParameterValue.StringValue("0%"),
        PARAMETER_MOD_FM_DEVIATION to ParameterValue.StringValue("0HZ"),
        PARAMETER_MOD_PM_DEVIATION to ParameterValue.StringValue("0DEG"),

        // memory
        PARAMETER_MEMORY_STORE     to ParameterValue.IntegerValue(0),
        PARAMETER_MEMORY_RECALL    to ParameterValue.IntegerValue(0),

        // status / error
        PARAMETER_DEVICE_ERROR  to ParameterValue.StringValue(""),
        PARAMETER_DEVICE_STATUS to ParameterValue.StringValue(""),

        // display
        PARAMETER_DISPLAY_TEXT  to ParameterValue.StringValue("")
    )

    // -----------------------------
    // Device connectivity
    // -----------------------------
    override fun connect(): Boolean = driver.connect()
    override fun disconnect(): Boolean = driver.disconnect()

    // -----------------------------
    // State synchronization
    // -----------------------------
    override suspend fun writeState(force: Boolean): Boolean {
        // push all parameters to driver
        runCatching {
            parameters.forEach { (key, value) ->
                driver.writeParameter(key, value)
            }
        }.onFailure { return false }
        return true
    }

    override suspend fun readState(): Boolean {
        // read all supported parameters from driver
        runCatching {
            parameters.keys.forEach { key ->
                driver.queryParameter(key)?.let { result ->
                    if (result is MessageResult.Result) {
                        parameters[key] = result.result
                    }
                }
            }
        }.onFailure { return false }
        return true
    }

    // -----------------------------
    // Parameter bindings
    // -----------------------------
    @ParameterSetter(PARAMETER_GENERATOR_FREQUENCY)
    suspend fun setFrequency(value: ParameterValue) =
        driver.writeParameter(PARAMETER_GENERATOR_FREQUENCY, value)

    @ParameterGetter(PARAMETER_GENERATOR_FREQUENCY)
    suspend fun getFrequency() =
        driver.queryParameter(PARAMETER_GENERATOR_FREQUENCY)

    @ParameterSetter(PARAMETER_GENERATOR_AMPLITUDE)
    suspend fun setAmplitude(value: ParameterValue) =
        driver.writeParameter(PARAMETER_GENERATOR_AMPLITUDE, value)

    @ParameterGetter(PARAMETER_GENERATOR_AMPLITUDE)
    suspend fun getAmplitude() =
        driver.queryParameter(PARAMETER_GENERATOR_AMPLITUDE)

    @ParameterSetter(PARAMETER_GENERATOR_FUNCTION)
    suspend fun setFunction(value: ParameterValue) =
        driver.writeParameter(PARAMETER_GENERATOR_FUNCTION, value)

    @ParameterGetter(PARAMETER_GENERATOR_FUNCTION)
    suspend fun getFunction() =
        driver.queryParameter(PARAMETER_GENERATOR_FUNCTION)

    @ParameterSetter(PARAMETER_GENERATOR_WAVEFORM)
    suspend fun setWaveform(value: ParameterValue) =
        driver.writeParameter(PARAMETER_GENERATOR_WAVEFORM, value)

    @ParameterGetter(PARAMETER_GENERATOR_WAVEFORM)
    suspend fun getWaveform() =
        driver.queryParameter(PARAMETER_GENERATOR_WAVEFORM)

    @ParameterSetter(PARAMETER_GENERATOR_OFFSET)
    suspend fun setOffset(value: ParameterValue) =
        driver.writeParameter(PARAMETER_GENERATOR_OFFSET, value)

    @ParameterGetter(PARAMETER_GENERATOR_OFFSET)
    suspend fun getOffset() =
        driver.queryParameter(PARAMETER_GENERATOR_OFFSET)

    @ParameterSetter(PARAMETER_GENERATOR_PHASE)
    suspend fun setPhase(value: ParameterValue) =
        driver.writeParameter(PARAMETER_GENERATOR_PHASE, value)

    @ParameterGetter(PARAMETER_GENERATOR_PHASE)
    suspend fun getPhase() =
        driver.queryParameter(PARAMETER_GENERATOR_PHASE)

    // ---- Sweep ----
    @ParameterSetter(PARAMETER_SWEEP_START)
    suspend fun setSweepStart(value: ParameterValue) =
        driver.writeParameter(PARAMETER_SWEEP_START, value)

    @ParameterGetter(PARAMETER_SWEEP_START)
    suspend fun getSweepStart() =
        driver.queryParameter(PARAMETER_SWEEP_START)

    @ParameterSetter(PARAMETER_SWEEP_STOP)
    suspend fun setSweepStop(value: ParameterValue) =
        driver.writeParameter(PARAMETER_SWEEP_STOP, value)

    @ParameterGetter(PARAMETER_SWEEP_STOP)
    suspend fun getSweepStop() =
        driver.queryParameter(PARAMETER_SWEEP_STOP)

    @ParameterSetter(PARAMETER_SWEEP_TIME)
    suspend fun setSweepTime(value: ParameterValue) =
        driver.writeParameter(PARAMETER_SWEEP_TIME, value)

    @ParameterGetter(PARAMETER_SWEEP_TIME)
    suspend fun getSweepTime() =
        driver.queryParameter(PARAMETER_SWEEP_TIME)

    @ParameterSetter(PARAMETER_SWEEP_MODE)
    suspend fun setSweepMode(value: ParameterValue) =
        driver.writeParameter(PARAMETER_SWEEP_MODE, value)

    @ParameterGetter(PARAMETER_SWEEP_MODE)
    suspend fun getSweepMode() =
        driver.queryParameter(PARAMETER_SWEEP_MODE)

    @ParameterSetter(PARAMETER_SWEEP_TRIGGER)
    suspend fun triggerSweep(@Suppress("UNUSED_PARAMETER") value: ParameterValue) =
        driver.writeParameter(PARAMETER_SWEEP_TRIGGER, ParameterValue.StringValue("TRIGGER"))

    // ---- Modulation ----
    @ParameterSetter(PARAMETER_MOD_ENABLED)
    suspend fun setModEnabled(value: ParameterValue) =
        driver.writeParameter(PARAMETER_MOD_ENABLED, value)

    @ParameterGetter(PARAMETER_MOD_ENABLED)
    suspend fun getModEnabled() =
        driver.queryParameter(PARAMETER_MOD_ENABLED)

    @ParameterSetter(PARAMETER_MOD_SOURCE)
    suspend fun setModSource(value: ParameterValue) =
        driver.writeParameter(PARAMETER_MOD_SOURCE, value)

    @ParameterGetter(PARAMETER_MOD_SOURCE)
    suspend fun getModSource() =
        driver.queryParameter(PARAMETER_MOD_SOURCE)

    @ParameterSetter(PARAMETER_MOD_AM_DEPTH)
    suspend fun setAMDepth(value: ParameterValue) =
        driver.writeParameter(PARAMETER_MOD_AM_DEPTH, value)

    @ParameterGetter(PARAMETER_MOD_AM_DEPTH)
    suspend fun getAMDepth() =
        driver.queryParameter(PARAMETER_MOD_AM_DEPTH)

    @ParameterSetter(PARAMETER_MOD_FM_DEVIATION)
    suspend fun setFMDeviation(value: ParameterValue) =
        driver.writeParameter(PARAMETER_MOD_FM_DEVIATION, value)

    @ParameterGetter(PARAMETER_MOD_FM_DEVIATION)
    suspend fun getFMDeviation() =
        driver.queryParameter(PARAMETER_MOD_FM_DEVIATION)

    @ParameterSetter(PARAMETER_MOD_PM_DEVIATION)
    suspend fun setPMDeviation(value: ParameterValue) =
        driver.writeParameter(PARAMETER_MOD_PM_DEVIATION, value)

    @ParameterGetter(PARAMETER_MOD_PM_DEVIATION)
    suspend fun getPMDeviation() =
        driver.queryParameter(PARAMETER_MOD_PM_DEVIATION)

    // ---- Memory ----
    @ParameterSetter(PARAMETER_MEMORY_STORE)
    suspend fun storeMemory(value: ParameterValue) =
        driver.writeParameter(PARAMETER_MEMORY_STORE, value)

    @ParameterGetter(PARAMETER_MEMORY_STORE)
    suspend fun getMemoryStore() =
        driver.queryParameter(PARAMETER_MEMORY_STORE)

    @ParameterSetter(PARAMETER_MEMORY_RECALL)
    suspend fun recallMemory(value: ParameterValue) =
        driver.writeParameter(PARAMETER_MEMORY_RECALL, value)

    @ParameterGetter(PARAMETER_MEMORY_RECALL)
    suspend fun getMemoryRecall() =
        driver.queryParameter(PARAMETER_MEMORY_RECALL)

    // ---- Device status ----
    @ParameterGetter(PARAMETER_DEVICE_ERROR)
    suspend fun getDeviceError() =
        driver.queryParameter(PARAMETER_DEVICE_ERROR)

    @ParameterGetter(PARAMETER_DEVICE_STATUS)
    suspend fun getDeviceStatus() =
        driver.queryParameter(PARAMETER_DEVICE_STATUS)

    // ---- Display ----
    @ParameterSetter(PARAMETER_DISPLAY_TEXT)
    suspend fun setDisplayText(value: ParameterValue) =
        driver.writeParameter(PARAMETER_DISPLAY_TEXT, value)

    @ParameterGetter(PARAMETER_DISPLAY_TEXT)
    suspend fun getDisplayText() =
        driver.queryParameter(PARAMETER_DISPLAY_TEXT)
}
