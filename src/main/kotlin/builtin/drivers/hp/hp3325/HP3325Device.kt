// HP3325Device.kt
package com.labeq.builtin.drivers.hp.hp3325

import com.labeq.communication.MQTTEventBus
import com.labeq.device.DeviceMetadata
import com.labeq.device.DeviceDriver
import com.labeq.device.GenericDevice
import com.labeq.device.ParameterGetter
import com.labeq.device.ParameterSetter
import com.labeq.transport.MessageResult
import com.labeq.transport.ParameterValue
import com.labeq.transport.ReturnType
import com.labeq.transport.onSuccess
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class HP3325Device(
    eventBus: MQTTEventBus,
    driver: DeviceDriver,
    data: DeviceMetadata
) : GenericDevice(eventBus, driver, data) {

    companion object {
        // Signal generation
        const val PARAMETER_GENERATOR_FREQUENCY = "generator.frequency"
        const val PARAMETER_GENERATOR_FREQUENCY_HZ = "generator.frequency.hz"
        const val PARAMETER_GENERATOR_AMPLITUDE = "generator.amplitude"
        const val PARAMETER_GENERATOR_AMPLITUDE_V = "generator.amplitude.v"
        const val PARAMETER_GENERATOR_FUNCTION = "generator.function"
        const val PARAMETER_GENERATOR_PHASE = "generator.phase"
        const val PARAMETER_GENERATOR_PHASE_DEG = "generator.phase.deg"
        const val PARAMETER_GENERATOR_HV = "generator.hv"
        const val PARAMETER_GENERATOR_ZERO_PHASE = "generator.zero_phase"
        const val PARAMETER_GENERATOR_OFFSET = "generator.offset"
        const val PARAMETER_GENERATOR_OFFSET_V = "generator.offset.v"
        const val PARAMETER_GENERATOR_OUTPUT_SELECT = "generator.output_select"

        // Sweep parameters
        const val PARAMETER_SWEEP_START = "sweep.start"
        const val PARAMETER_SWEEP_START_HZ = "sweep.start.hz"
        const val PARAMETER_SWEEP_STOP = "sweep.stop"
        const val PARAMETER_SWEEP_STOP_HZ = "sweep.stop.hz"
        const val PARAMETER_SWEEP_TIME = "sweep.time"
        const val PARAMETER_SWEEP_TIME_S = "sweep.time.s"
        const val PARAMETER_SWEEP_MODE = "sweep.mode"

        const val PARAMETER_SWEEP_DISCRETE_CLEAR = "sweep.discrete.clear"
        const val PARAMETER_SWEEP_DISCRETE_STORE = "sweep.discrete.store"
        const val PARAMETER_SWEEP_DISCRETE_RECALL = "sweep.discrete.recall"

        const val PARAMETER_SWEEP_SINGLE_START = "sweep.single.start"
        const val PARAMETER_SWEEP_SINGLE_RESET = "sweep.single.reset"

        const val PARAMETER_SWEEP_CONTINUOUS_START = "sweep.continuous.start"

        // Marker
        const val PARAMETER_MARKER_FREQUENCY = "marker.frequency"
        const val PARAMETER_MARKER_FREQUENCY_HZ = "marker.frequency.hz"

        // Modulation
        const val PARAMETER_MODULATION_SOURCE_AMPLITUDE = "modulation.source.amplitude"
        const val PARAMETER_MODULATION_SOURCE_AMPLITUDE_V = "modulation.source.amplitude.v"
        const val PARAMETER_MODULATION_SOURCE_ARBITRARY_DATA = "modulation.source.arbitrary_data"
        const val PARAMETER_MODULATION_SOURCE_FREQUENCY = "modulation.source.frequency"
        const val PARAMETER_MODULATION_SOURCE_FREQUENCY_HZ = "modulation.source.frequency.hz"
        const val PARAMETER_MODULATION_SOURCE_FUNCTION = "modulation.source.function"

        const val PARAMETER_AMPLITUDE_MODULATION = "modulation.amplitude"
        const val PARAMETER_PHASE_MODULATION = "modulation.phase"

        // Memory / states
        const val PARAMETER_STATE_STORE = "state.store"
        const val PARAMETER_STATE_RECALL = "state.recall"

        // Status / errors / self test
        const val PARAMETER_CALIBRATION_AMPLITUDE = "calibration.amplitude"
        const val PARAMETER_CALIBRATION_MODE = "calibration.mode"

        const val PARAMETER_ENHANCEMENTS = "status.enhancements"
        const val PARAMETER_ERROR = "status.error"

        const val PARAMETER_EXTERNAL_REFERENCE_LOCK = "status.external_reference_lock"

        const val PARAMETER_LOCAL_ENABLE = "status.local_enable"
        const val PARAMETER_REMOTE_ENABLE = "status.remote_enable"

        const val PARAMETER_RESET_DEVICE = "status.reset"

        const val PARAMETER_BYTE_MASK = "status.byte_mask"
        const val PARAMETER_SRQ_ENABLE = "status.srq_enable"
        const val PARAMETER_STATUS_BYTE = "status.byte"

        const val PARAMETER_IDENTIFICATION_STANDARD = "status.id.standard"
        const val PARAMETER_IDENTIFICATION_488_2 = "status.id.ieee488"

        const val PARAMETER_OPTIONS = "status.options"

        // Communication
        const val PARAMETER_RS232_ECHO = "communication.echo"
        const val PARAMETER_RES_HEAD = "communication.head"

        const val PARAMETER_DATA_TRANSFER_MODE = "communication.data_transfer_mode"

        // Display
        const val PARAMETER_DISPLAY_ENABLE = "display.enable"
        const val PARAMETER_DISPLAY_STRING = "display.string"
    }

    private val freqMultipliers = mapOf("HZ" to 1.0, "KH" to 1e3, "MH" to 1e6)
    // Basic volt multipliers purely to parse device query answers into numeric helper properties
    private val voltMultipliers = mapOf("VO" to 1.0, "MV" to 1e-3, "VR" to 1.0, "MR" to 1e-3, "DB" to 1.0, "DV" to 1.0)

    override val parameters: MutableMap<String, ParameterValue> = mutableMapOf(
        PARAMETER_GENERATOR_FREQUENCY to ParameterValue.StringValue("0HZ"),
        PARAMETER_GENERATOR_FREQUENCY_HZ to ParameterValue.DoubleValue(0.0),
        PARAMETER_GENERATOR_AMPLITUDE to ParameterValue.StringValue("0VO"),
        PARAMETER_GENERATOR_AMPLITUDE_V to ParameterValue.DoubleValue(0.0),
        PARAMETER_GENERATOR_FUNCTION to ParameterValue.IntegerValue(0),
        PARAMETER_GENERATOR_HV to ParameterValue.StringValue(""),
        PARAMETER_GENERATOR_PHASE to ParameterValue.StringValue("0DEG"),
        PARAMETER_GENERATOR_PHASE_DEG to ParameterValue.DoubleValue(0.0),
        PARAMETER_GENERATOR_ZERO_PHASE to ParameterValue.IntegerValue(0),
        PARAMETER_GENERATOR_OFFSET to ParameterValue.StringValue("0VO"),
        PARAMETER_GENERATOR_OFFSET_V to ParameterValue.DoubleValue(0.0),
        PARAMETER_GENERATOR_OUTPUT_SELECT to ParameterValue.IntegerValue(1),
        PARAMETER_SWEEP_START to ParameterValue.StringValue("0HZ"),
        PARAMETER_SWEEP_START_HZ to ParameterValue.DoubleValue(0.0),
        PARAMETER_SWEEP_STOP to ParameterValue.StringValue("0HZ"),
        PARAMETER_SWEEP_STOP_HZ to ParameterValue.DoubleValue(0.0),
        PARAMETER_SWEEP_TIME to ParameterValue.StringValue("0SE"),
        PARAMETER_SWEEP_TIME_S to ParameterValue.DoubleValue(0.0),
        PARAMETER_SWEEP_MODE to ParameterValue.IntegerValue(1),
        PARAMETER_SWEEP_DISCRETE_CLEAR to ParameterValue.IntegerValue(0),
        PARAMETER_SWEEP_DISCRETE_STORE to ParameterValue.IntegerValue(0),
        PARAMETER_SWEEP_DISCRETE_RECALL to ParameterValue.IntegerValue(0),
        PARAMETER_SWEEP_SINGLE_START to ParameterValue.IntegerValue(0),
        PARAMETER_SWEEP_SINGLE_RESET to ParameterValue.IntegerValue(0),
        PARAMETER_SWEEP_CONTINUOUS_START to ParameterValue.IntegerValue(0),
        PARAMETER_MARKER_FREQUENCY to ParameterValue.StringValue("0HZ"),
        PARAMETER_MARKER_FREQUENCY_HZ to ParameterValue.DoubleValue(0.0),
        PARAMETER_MODULATION_SOURCE_AMPLITUDE to ParameterValue.StringValue("0VO"),
        PARAMETER_MODULATION_SOURCE_AMPLITUDE_V to ParameterValue.DoubleValue(0.0),
        PARAMETER_MODULATION_SOURCE_ARBITRARY_DATA to ParameterValue.DoubleArrayValue(doubleArrayOf()),
        PARAMETER_MODULATION_SOURCE_FREQUENCY to ParameterValue.StringValue("0HZ"),
        PARAMETER_MODULATION_SOURCE_FREQUENCY_HZ to ParameterValue.DoubleValue(0.0),
        PARAMETER_MODULATION_SOURCE_FUNCTION to ParameterValue.IntegerValue(0),
        PARAMETER_AMPLITUDE_MODULATION to ParameterValue.IntegerValue(0),
        PARAMETER_PHASE_MODULATION to ParameterValue.IntegerValue(0),
        PARAMETER_STATE_STORE to ParameterValue.IntegerValue(0),
        PARAMETER_STATE_RECALL to ParameterValue.IntegerValue(0),
        PARAMETER_CALIBRATION_AMPLITUDE to ParameterValue.IntegerValue(0),
        PARAMETER_CALIBRATION_MODE to ParameterValue.IntegerValue(0),
        PARAMETER_ENHANCEMENTS to ParameterValue.IntegerValue(0),
        PARAMETER_ERROR to ParameterValue.IntegerValue(0),
        PARAMETER_EXTERNAL_REFERENCE_LOCK to ParameterValue.IntegerValue(0),
        PARAMETER_LOCAL_ENABLE to ParameterValue.IntegerValue(0),
        PARAMETER_REMOTE_ENABLE to ParameterValue.IntegerValue(0),
        PARAMETER_RESET_DEVICE to ParameterValue.IntegerValue(0),
        PARAMETER_BYTE_MASK to ParameterValue.BytesValue(byteArrayOf()),
        PARAMETER_SRQ_ENABLE to ParameterValue.StringValue(""),
        PARAMETER_STATUS_BYTE to ParameterValue.BytesValue(byteArrayOf()),
        PARAMETER_IDENTIFICATION_STANDARD to ParameterValue.StringValue(""),
        PARAMETER_IDENTIFICATION_488_2 to ParameterValue.StringValue(""),
        PARAMETER_OPTIONS to ParameterValue.StringValue(""),
        PARAMETER_RS232_ECHO to ParameterValue.IntegerValue(0),
        PARAMETER_RES_HEAD to ParameterValue.IntegerValue(0),
        PARAMETER_DATA_TRANSFER_MODE to ParameterValue.IntegerValue(1),
        PARAMETER_DISPLAY_ENABLE to ParameterValue.IntegerValue(0),
        PARAMETER_DISPLAY_STRING to ParameterValue.StringValue("")
    )

    override fun connect(): Boolean = driver.connect()
    override fun disconnect(): Boolean = driver.disconnect()
    override suspend fun identify(): MessageResult<String> {
        driver.queryParameter(PARAMETER_IDENTIFICATION_488_2, ReturnType.String).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid type")
            return MessageResult.Result(it.value)
        }
        return MessageResult.Error("Unknown error")
    }

    private fun parseStringValue(res: MessageResult<ParameterValue>, multipliers: Map<String, Double>): MessageResult<ParameterValue> {
        if (res !is MessageResult.Result) return res
        val strValue = (res.result as? ParameterValue.StringValue)?.value ?: return MessageResult.Error("Invalid type")
        val regex = "(?<value>[-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)\\s*(?<unit>[a-zA-Z]*)".toRegex()
        val match = regex.matchEntire(strValue.trim()) ?: return MessageResult.Error("Unparseable format: $strValue")
        val value = match.groupValues[1].toDoubleOrNull() ?: 0.0
        val unit = match.groupValues[2].uppercase()
        val multiplier = multipliers[unit] ?: 1.0
        return MessageResult.Result(ParameterValue.DoubleValue(value * multiplier))
    }

    @ParameterGetter(PARAMETER_GENERATOR_AMPLITUDE)
    suspend fun getGeneratorAmplitude(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_GENERATOR_AMPLITUDE)
    @ParameterSetter(PARAMETER_GENERATOR_AMPLITUDE)
    suspend fun setGeneratorAmplitude(value: ParameterValue) = driver.writeParameter(PARAMETER_GENERATOR_AMPLITUDE, value)

    @ParameterGetter(PARAMETER_GENERATOR_AMPLITUDE_V)
    suspend fun getGeneratorAmplitudeV() = parseStringValue(getGeneratorAmplitude(), voltMultipliers)

    @ParameterGetter(PARAMETER_GENERATOR_FREQUENCY)
    suspend fun getGeneratorFrequency(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_GENERATOR_FREQUENCY)
    @ParameterSetter(PARAMETER_GENERATOR_FREQUENCY)
    suspend fun setGeneratorFrequency(value: ParameterValue): MessageResult<ParameterValue> = driver.writeParameter(PARAMETER_GENERATOR_FREQUENCY, value)

    @ParameterGetter(PARAMETER_GENERATOR_FREQUENCY_HZ)
    suspend fun getGeneratorFrequencyHz() = parseStringValue(getGeneratorFrequency(), freqMultipliers)

    @ParameterGetter(PARAMETER_GENERATOR_FUNCTION)
    suspend fun getGeneratorFunction(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_GENERATOR_FUNCTION)
    @ParameterSetter(PARAMETER_GENERATOR_FUNCTION)
    suspend fun setGeneratorFunction(value: ParameterValue) = driver.writeParameter(PARAMETER_GENERATOR_FUNCTION, value)

    @ParameterGetter(PARAMETER_GENERATOR_PHASE)
    suspend fun getGeneratorPhase(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_GENERATOR_PHASE)
    @ParameterSetter(PARAMETER_GENERATOR_PHASE)
    suspend fun setGeneratorPhase(value: ParameterValue) = driver.writeParameter(PARAMETER_GENERATOR_PHASE, value)

    @ParameterGetter(PARAMETER_GENERATOR_PHASE_DEG)
    suspend fun getGeneratorPhaseDeg() = parseStringValue(getGeneratorPhase(), mapOf("DE" to 1.0, "DEG" to 1.0))

    @ParameterGetter(PARAMETER_GENERATOR_HV)
    suspend fun getGeneratorHv(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_GENERATOR_HV)
    @ParameterSetter(PARAMETER_GENERATOR_HV)
    suspend fun setGeneratorHv(value: ParameterValue) = driver.writeParameter(PARAMETER_GENERATOR_HV, value)

    @ParameterGetter(PARAMETER_GENERATOR_ZERO_PHASE)
    suspend fun getGeneratorZeroPhase(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_GENERATOR_ZERO_PHASE)
    @ParameterSetter(PARAMETER_GENERATOR_ZERO_PHASE)
    suspend fun setGeneratorZeroPhase(value: ParameterValue) = driver.writeParameter(PARAMETER_GENERATOR_ZERO_PHASE, value)

    @ParameterGetter(PARAMETER_GENERATOR_OFFSET)
    suspend fun getGeneratorOffset(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_GENERATOR_OFFSET)
    @ParameterSetter(PARAMETER_GENERATOR_OFFSET)
    suspend fun setGeneratorOffset(value: ParameterValue) = driver.writeParameter(PARAMETER_GENERATOR_OFFSET, value)

    @ParameterGetter(PARAMETER_GENERATOR_OFFSET_V)
    suspend fun getGeneratorOffsetV() = parseStringValue(getGeneratorOffset(), voltMultipliers)

    @ParameterGetter(PARAMETER_GENERATOR_OUTPUT_SELECT)
    suspend fun getGeneratorOutputSelect(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_GENERATOR_OUTPUT_SELECT)
    @ParameterSetter(PARAMETER_GENERATOR_OUTPUT_SELECT)
    suspend fun setGeneratorOutputSelect(value: ParameterValue) = driver.writeParameter(PARAMETER_GENERATOR_OUTPUT_SELECT, value)

    @ParameterGetter(PARAMETER_SWEEP_START)
    suspend fun getSweepStart(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_START)
    @ParameterSetter(PARAMETER_SWEEP_START)
    suspend fun setSweepStart(value: ParameterValue) = driver.writeParameter(PARAMETER_SWEEP_START, value)

    @ParameterGetter(PARAMETER_SWEEP_START_HZ)
    suspend fun getSweepStartHz() = parseStringValue(getSweepStart(), freqMultipliers)

    @ParameterGetter(PARAMETER_SWEEP_STOP)
    suspend fun getSweepStop(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_STOP)
    @ParameterSetter(PARAMETER_SWEEP_STOP)
    suspend fun setSweepStop(value: ParameterValue) = driver.writeParameter(PARAMETER_SWEEP_STOP, value)

    @ParameterGetter(PARAMETER_SWEEP_STOP_HZ)
    suspend fun getSweepStopHz() = parseStringValue(getSweepStop(), freqMultipliers)

    @ParameterGetter(PARAMETER_SWEEP_TIME)
    suspend fun getSweepTime(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_TIME)
    @ParameterSetter(PARAMETER_SWEEP_TIME)
    suspend fun setSweepTime(value: ParameterValue) = driver.writeParameter(PARAMETER_SWEEP_TIME, value)

    @ParameterGetter(PARAMETER_SWEEP_TIME_S)
    suspend fun getSweepTimeS() = parseStringValue(getSweepTime(), mapOf("SE" to 1.0, "S" to 1.0))

    @ParameterGetter(PARAMETER_SWEEP_MODE)
    suspend fun getSweepMode(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_MODE)
    @ParameterSetter(PARAMETER_SWEEP_MODE)
    suspend fun setSweepMode(value: ParameterValue) = driver.writeParameter(PARAMETER_SWEEP_MODE, value)

    @ParameterGetter(PARAMETER_SWEEP_DISCRETE_CLEAR)
    suspend fun getSweepDiscreteClear(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_DISCRETE_CLEAR)
    @ParameterSetter(PARAMETER_SWEEP_DISCRETE_CLEAR)
    suspend fun setSweepDiscreteClear(value: ParameterValue) = driver.writeParameter(PARAMETER_SWEEP_DISCRETE_CLEAR, value)

    @ParameterGetter(PARAMETER_SWEEP_DISCRETE_STORE)
    suspend fun getSweepDiscreteStore(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_DISCRETE_STORE)
    @ParameterSetter(PARAMETER_SWEEP_DISCRETE_STORE)
    suspend fun setSweepDiscreteStore(value: ParameterValue) = driver.writeParameter(PARAMETER_SWEEP_DISCRETE_STORE, value)

    @ParameterGetter(PARAMETER_SWEEP_DISCRETE_RECALL)
    suspend fun getSweepDiscreteRecall(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_DISCRETE_RECALL)
    @ParameterSetter(PARAMETER_SWEEP_DISCRETE_RECALL)
    suspend fun setSweepDiscreteRecall(value: ParameterValue) = driver.writeParameter(PARAMETER_SWEEP_DISCRETE_RECALL, value)

    @ParameterGetter(PARAMETER_SWEEP_SINGLE_START)
    suspend fun getSweepSingleStart(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_SINGLE_START)
    @ParameterSetter(PARAMETER_SWEEP_SINGLE_START)
    suspend fun setSweepSingleStart(value: ParameterValue) = driver.writeParameter(PARAMETER_SWEEP_SINGLE_START, value)

    @ParameterGetter(PARAMETER_SWEEP_SINGLE_RESET)
    suspend fun getSweepSingleReset(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_SINGLE_RESET)
    @ParameterSetter(PARAMETER_SWEEP_SINGLE_RESET)
    suspend fun setSweepSingleReset(value: ParameterValue) = driver.writeParameter(PARAMETER_SWEEP_SINGLE_RESET, value)

    @ParameterGetter(PARAMETER_SWEEP_CONTINUOUS_START)
    suspend fun getSweepContinuousStart(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_CONTINUOUS_START)
    @ParameterSetter(PARAMETER_SWEEP_CONTINUOUS_START)
    suspend fun setSweepContinuousStart(value: ParameterValue) = driver.writeParameter(PARAMETER_SWEEP_CONTINUOUS_START, value)

    @ParameterGetter(PARAMETER_MARKER_FREQUENCY)
    suspend fun getMarkerFrequency(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_MARKER_FREQUENCY)
    @ParameterSetter(PARAMETER_MARKER_FREQUENCY)
    suspend fun setMarkerFrequency(value: ParameterValue) = driver.writeParameter(PARAMETER_MARKER_FREQUENCY, value)

    @ParameterGetter(PARAMETER_MARKER_FREQUENCY_HZ)
    suspend fun getMarkerFrequencyHz() = parseStringValue(getMarkerFrequency(), freqMultipliers)

    @ParameterGetter(PARAMETER_MODULATION_SOURCE_AMPLITUDE)
    suspend fun getModulationSourceAmplitude(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_MODULATION_SOURCE_AMPLITUDE)
    @ParameterSetter(PARAMETER_MODULATION_SOURCE_AMPLITUDE)
    suspend fun setModulationSourceAmplitude(value: ParameterValue) = driver.writeParameter(PARAMETER_MODULATION_SOURCE_AMPLITUDE, value)

    @ParameterGetter(PARAMETER_MODULATION_SOURCE_AMPLITUDE_V)
    suspend fun getModulationSourceAmplitudeV() = parseStringValue(getModulationSourceAmplitude(), voltMultipliers)

    @ParameterGetter(PARAMETER_MODULATION_SOURCE_ARBITRARY_DATA)
    suspend fun getModulationSourceArbitraryData(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_MODULATION_SOURCE_ARBITRARY_DATA)

    @ParameterGetter(PARAMETER_MODULATION_SOURCE_FREQUENCY)
    suspend fun getModulationSourceFrequency(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_MODULATION_SOURCE_FREQUENCY)
    @ParameterSetter(PARAMETER_MODULATION_SOURCE_FREQUENCY)
    suspend fun setModulationSourceFrequency(value: ParameterValue) = driver.writeParameter(PARAMETER_MODULATION_SOURCE_FREQUENCY, value)

    @ParameterGetter(PARAMETER_MODULATION_SOURCE_FREQUENCY_HZ)
    suspend fun getModulationSourceFrequencyHz() = parseStringValue(getModulationSourceFrequency(), freqMultipliers)

    @ParameterGetter(PARAMETER_MODULATION_SOURCE_FUNCTION)
    suspend fun getModulationSourceFunction(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_MODULATION_SOURCE_FUNCTION)
    @ParameterSetter(PARAMETER_MODULATION_SOURCE_FUNCTION)
    suspend fun setModulationSourceFunction(value: ParameterValue) = driver.writeParameter(PARAMETER_MODULATION_SOURCE_FUNCTION, value)

    @ParameterGetter(PARAMETER_AMPLITUDE_MODULATION)
    suspend fun getAmplitudeModulation(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_AMPLITUDE_MODULATION)
    @ParameterSetter(PARAMETER_AMPLITUDE_MODULATION)
    suspend fun setAmplitudeModulation(value: ParameterValue) = driver.writeParameter(PARAMETER_AMPLITUDE_MODULATION, value)

    @ParameterGetter(PARAMETER_PHASE_MODULATION)
    suspend fun getPhaseModulation(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_PHASE_MODULATION)
    @ParameterSetter(PARAMETER_PHASE_MODULATION)
    suspend fun setPhaseModulation(value: ParameterValue) = driver.writeParameter(PARAMETER_PHASE_MODULATION, value)

    @ParameterSetter(PARAMETER_STATE_STORE)
    suspend fun setStateStore(value: ParameterValue) = driver.writeParameter(PARAMETER_STATE_STORE, value)

    @ParameterSetter(PARAMETER_STATE_RECALL)
    suspend fun setStateRecall(value: ParameterValue) = driver.writeParameter(PARAMETER_STATE_RECALL, value)

    @ParameterGetter(PARAMETER_CALIBRATION_AMPLITUDE)
    suspend fun getCalibrationAmplitude(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_CALIBRATION_AMPLITUDE)
    @ParameterSetter(PARAMETER_CALIBRATION_AMPLITUDE)
    suspend fun setCalibrationAmplitude(value: ParameterValue) = driver.writeParameter(PARAMETER_CALIBRATION_AMPLITUDE, value)

    @ParameterGetter(PARAMETER_CALIBRATION_MODE)
    suspend fun getCalibrationMode(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_CALIBRATION_MODE)
    @ParameterSetter(PARAMETER_CALIBRATION_MODE)
    suspend fun setCalibrationMode(value: ParameterValue) = driver.writeParameter(PARAMETER_CALIBRATION_MODE, value)

    @ParameterGetter(PARAMETER_ENHANCEMENTS)
    suspend fun getEnhancements(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_ENHANCEMENTS)
    @ParameterSetter(PARAMETER_ENHANCEMENTS)
    suspend fun setEnhancements(value: ParameterValue) = driver.writeParameter(PARAMETER_ENHANCEMENTS, value)

    @ParameterGetter(PARAMETER_ERROR)
    suspend fun getError(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_ERROR)

    @ParameterGetter(PARAMETER_EXTERNAL_REFERENCE_LOCK)
    suspend fun getExternalReferenceLock(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_EXTERNAL_REFERENCE_LOCK)

    @ParameterGetter(PARAMETER_LOCAL_ENABLE)
    suspend fun getLocalEnable(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_LOCAL_ENABLE)
    @ParameterSetter(PARAMETER_LOCAL_ENABLE)
    suspend fun setLocalEnable(value: ParameterValue) = driver.writeParameter(PARAMETER_LOCAL_ENABLE, value)

    @ParameterGetter(PARAMETER_REMOTE_ENABLE)
    suspend fun getRemoteEnable(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_REMOTE_ENABLE)
    @ParameterSetter(PARAMETER_REMOTE_ENABLE)
    suspend fun setRemoteEnable(value: ParameterValue) = driver.writeParameter(PARAMETER_REMOTE_ENABLE, value)

    @ParameterGetter(PARAMETER_RESET_DEVICE)
    suspend fun getResetDevice(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_RESET_DEVICE)
    @ParameterSetter(PARAMETER_RESET_DEVICE)
    suspend fun setResetDevice(value: ParameterValue) = driver.writeParameter(PARAMETER_RESET_DEVICE, value)

    @ParameterGetter(PARAMETER_BYTE_MASK)
    suspend fun getByteMask(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_BYTE_MASK)

    @ParameterGetter(PARAMETER_SRQ_ENABLE)
    suspend fun getSrqEnable(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SRQ_ENABLE)
    @ParameterSetter(PARAMETER_SRQ_ENABLE)
    suspend fun setSrqEnable(value: ParameterValue) = driver.writeParameter(PARAMETER_SRQ_ENABLE, value)

    @ParameterGetter(PARAMETER_STATUS_BYTE)
    suspend fun getStatusByte(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_STATUS_BYTE)

    @ParameterGetter(PARAMETER_IDENTIFICATION_STANDARD)
    suspend fun getIdentificationStandard(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_IDENTIFICATION_STANDARD)

    @ParameterGetter(PARAMETER_IDENTIFICATION_488_2)
    suspend fun getIdentification4882(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_IDENTIFICATION_488_2)

    @ParameterGetter(PARAMETER_OPTIONS)
    suspend fun getOptions(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_OPTIONS)

    @ParameterGetter(PARAMETER_RS232_ECHO)
    suspend fun getRs232Echo(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_RS232_ECHO)
    @ParameterSetter(PARAMETER_RS232_ECHO)
    suspend fun setRs232Echo(value: ParameterValue) = driver.writeParameter(PARAMETER_RS232_ECHO, value)

    @ParameterGetter(PARAMETER_RES_HEAD)
    suspend fun getResHead(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_RES_HEAD)
    @ParameterSetter(PARAMETER_RES_HEAD)
    suspend fun setResHead(value: ParameterValue) = driver.writeParameter(PARAMETER_RES_HEAD, value)

    @ParameterGetter(PARAMETER_DATA_TRANSFER_MODE)
    suspend fun getDataTransferMode(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_DATA_TRANSFER_MODE)
    @ParameterSetter(PARAMETER_DATA_TRANSFER_MODE)
    suspend fun setDataTransferMode(value: ParameterValue) = driver.writeParameter(PARAMETER_DATA_TRANSFER_MODE, value)

    @ParameterGetter(PARAMETER_DISPLAY_ENABLE)
    suspend fun getDisplayEnable(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_DISPLAY_ENABLE)
    @ParameterSetter(PARAMETER_DISPLAY_ENABLE)
    suspend fun setDisplayEnable(value: ParameterValue) = driver.writeParameter(PARAMETER_DISPLAY_ENABLE, value)

    @ParameterGetter(PARAMETER_DISPLAY_STRING)
    suspend fun getDisplayString(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_DISPLAY_STRING)
    @ParameterSetter(PARAMETER_DISPLAY_STRING)
    suspend fun setDisplayString(value: ParameterValue) = driver.writeParameter(PARAMETER_DISPLAY_STRING, value)

}
