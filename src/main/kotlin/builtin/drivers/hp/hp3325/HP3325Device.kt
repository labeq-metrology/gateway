package com.labeq.builtin.drivers.hp.hp3325

import com.labeq.communication.DeviceRequest
import com.labeq.communication.MQTTEventBus
import com.labeq.device.DeviceDriver
import com.labeq.device.generic.GenericDevice
import com.labeq.device.generic.ParameterGetter
import com.labeq.transport.MessageResult
import com.labeq.transport.ParameterValue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class HP3325Device(
    eventBus: MQTTEventBus,
    name: String,
    manufacturer: String,
    uuid: Uuid,
    driver: DeviceDriver,
) : GenericDevice(eventBus, name, manufacturer, "HP3325", uuid, driver) {

    companion object {
        // Signal generation
        const val PARAMETER_GENERATOR_FREQUENCY = "generator.frequency"
        const val PARAMETER_GENERATOR_AMPLITUDE = "generator.amplitude"
        const val PARAMETER_GENERATOR_FUNCTION = "generator.function"
        const val PARAMETER_GENERATOR_PHASE = "generator.phase"
        const val PARAMETER_GENERATOR_HV = "generator.hv"
        const val PARAMETER_GENERATOR_ZERO_PHASE = "generator.zero_phase"
        const val PARAMETER_GENERATOR_OFFSET = "generator.offset"
        const val PARAMETER_GENERATOR_OUTPUT_SELECT = "generator.output_select"

        // Sweep parameters
        const val PARAMETER_SWEEP_START = "sweep.start"
        const val PARAMETER_SWEEP_STOP = "sweep.stop"
        const val PARAMETER_SWEEP_TIME = "sweep.time"
        const val PARAMETER_SWEEP_MODE = "sweep.mode"

        const val PARAMETER_SWEEP_DISCRETE_CLEAR = "sweep.discrete.clear"
        const val PARAMETER_SWEEP_DISCRETE_STORE = "sweep.discrete.store"
        const val PARAMETER_SWEEP_DISCRETE_RECALL = "sweep.discrete.recall"

        const val PARAMETER_SWEEP_SINGLE_START = "sweep.single.start"
        const val PARAMETER_SWEEP_SINGLE_RESET = "sweep.single.reset"

        const val PARAMETER_SWEEP_CONTINUOUS_START = "sweep.continuous.start"

        // Marker
        const val PARAMETER_MARKER_FREQUENCY = "marker.frequency"

        // Modulation
        const val PARAMETER_MODULATION_SOURCE_AMPLITUDE = "modulation.source.amplitude"
        const val PARAMETER_MODULATION_SOURCE_ARBITRARY_DATA = "modulation.source.arbitrary_data"
        const val PARAMETER_MODULATION_SOURCE_FREQUENCY = "modulation.source.frequency"
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

    override val parameters: MutableMap<String, ParameterValue> = mutableMapOf(
        PARAMETER_GENERATOR_FREQUENCY to ParameterValue.StringValue("0HZ"),
        PARAMETER_GENERATOR_AMPLITUDE to ParameterValue.StringValue("0VO"),
        PARAMETER_GENERATOR_FUNCTION to ParameterValue.IntegerValue(0),
        PARAMETER_GENERATOR_HV to ParameterValue.StringValue(""),
        PARAMETER_GENERATOR_PHASE to ParameterValue.StringValue("0DEG"),
        PARAMETER_GENERATOR_ZERO_PHASE to ParameterValue.IntegerValue(0),
        PARAMETER_GENERATOR_OFFSET to ParameterValue.StringValue("0VO"),
        PARAMETER_GENERATOR_OUTPUT_SELECT to ParameterValue.IntegerValue(1),
        PARAMETER_SWEEP_START to ParameterValue.StringValue("0HZ"),
        PARAMETER_SWEEP_STOP to ParameterValue.StringValue("0HZ"),
        PARAMETER_SWEEP_TIME to ParameterValue.StringValue("0SE"),
        PARAMETER_SWEEP_MODE to ParameterValue.IntegerValue(1),
        PARAMETER_SWEEP_DISCRETE_CLEAR to ParameterValue.IntegerValue(0),
        PARAMETER_SWEEP_DISCRETE_STORE to ParameterValue.IntegerValue(0),
        PARAMETER_SWEEP_DISCRETE_RECALL to ParameterValue.IntegerValue(0),
        PARAMETER_SWEEP_SINGLE_START to ParameterValue.IntegerValue(0),
        PARAMETER_SWEEP_SINGLE_RESET to ParameterValue.IntegerValue(0),
        PARAMETER_SWEEP_CONTINUOUS_START to ParameterValue.IntegerValue(0),
        PARAMETER_MARKER_FREQUENCY to ParameterValue.StringValue("0HZ"),
        PARAMETER_MODULATION_SOURCE_AMPLITUDE to ParameterValue.StringValue("0VO"),
        PARAMETER_MODULATION_SOURCE_ARBITRARY_DATA to ParameterValue.DoubleArrayValue(doubleArrayOf()),
        PARAMETER_MODULATION_SOURCE_FREQUENCY to ParameterValue.StringValue("0HZ"),
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

    // -----------------------------
    // Device connectivity
    // -----------------------------
    override fun connect(): Boolean = driver.connect()
    override fun disconnect(): Boolean = driver.disconnect()

    @ParameterGetter(PARAMETER_GENERATOR_AMPLITUDE)
    suspend fun getGeneratorAmplitude(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_GENERATOR_AMPLITUDE)

    @ParameterGetter(PARAMETER_GENERATOR_FREQUENCY)
    suspend fun getGeneratorFrequency(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_GENERATOR_FREQUENCY)

    @ParameterGetter(PARAMETER_GENERATOR_FUNCTION)
    suspend fun getGeneratorFunction(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_GENERATOR_FUNCTION)

    @ParameterGetter(PARAMETER_GENERATOR_PHASE)
    suspend fun getGeneratorPhase(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_GENERATOR_PHASE)

    @ParameterGetter(PARAMETER_GENERATOR_HV)
    suspend fun getGeneratorHv(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_GENERATOR_HV)

    @ParameterGetter(PARAMETER_GENERATOR_ZERO_PHASE)
    suspend fun getGeneratorZeroPhase(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_GENERATOR_ZERO_PHASE)

    @ParameterGetter(PARAMETER_GENERATOR_OFFSET)
    suspend fun getGeneratorOffset(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_GENERATOR_OFFSET)

    @ParameterGetter(PARAMETER_GENERATOR_OUTPUT_SELECT)
    suspend fun getGeneratorOutputSelect(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_GENERATOR_OUTPUT_SELECT)

    @ParameterGetter(PARAMETER_SWEEP_START)
    suspend fun getSweepStart(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_START)

    @ParameterGetter(PARAMETER_SWEEP_STOP)
    suspend fun getSweepStop(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_STOP)

    @ParameterGetter(PARAMETER_SWEEP_TIME)
    suspend fun getSweepTime(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_TIME)

    @ParameterGetter(PARAMETER_SWEEP_MODE)
    suspend fun getSweepMode(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_MODE)

    @ParameterGetter(PARAMETER_SWEEP_DISCRETE_CLEAR)
    suspend fun getSweepDiscreteClear(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_DISCRETE_CLEAR)

    @ParameterGetter(PARAMETER_SWEEP_DISCRETE_STORE)
    suspend fun getSweepDiscreteStore(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_DISCRETE_STORE)

    @ParameterGetter(PARAMETER_SWEEP_DISCRETE_RECALL)
    suspend fun getSweepDiscreteRecall(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_DISCRETE_RECALL)

    @ParameterGetter(PARAMETER_SWEEP_SINGLE_START)
    suspend fun getSweepSingleStart(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_SINGLE_START)

    @ParameterGetter(PARAMETER_SWEEP_SINGLE_RESET)
    suspend fun getSweepSingleReset(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_SINGLE_RESET)

    @ParameterGetter(PARAMETER_SWEEP_CONTINUOUS_START)
    suspend fun getSweepContinuousStart(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SWEEP_CONTINUOUS_START)

    @ParameterGetter(PARAMETER_MARKER_FREQUENCY)
    suspend fun getMarkerFrequency(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_MARKER_FREQUENCY)

    @ParameterGetter(PARAMETER_MODULATION_SOURCE_AMPLITUDE)
    suspend fun getModulationSourceAmplitude(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_MODULATION_SOURCE_AMPLITUDE)

    @ParameterGetter(PARAMETER_MODULATION_SOURCE_ARBITRARY_DATA)
    suspend fun getModulationSourceArbitraryData(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_MODULATION_SOURCE_ARBITRARY_DATA)

    @ParameterGetter(PARAMETER_MODULATION_SOURCE_FREQUENCY)
    suspend fun getModulationSourceFrequency(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_MODULATION_SOURCE_FREQUENCY)

    @ParameterGetter(PARAMETER_MODULATION_SOURCE_FUNCTION)
    suspend fun getModulationSourceFunction(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_MODULATION_SOURCE_FUNCTION)

    @ParameterGetter(PARAMETER_AMPLITUDE_MODULATION)
    suspend fun getAmplitudeModulation(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_AMPLITUDE_MODULATION)

    @ParameterGetter(PARAMETER_PHASE_MODULATION)
    suspend fun getPhaseModulation(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_PHASE_MODULATION)

    @ParameterGetter(PARAMETER_STATE_STORE)
    suspend fun getStateStore(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_STATE_STORE)

    @ParameterGetter(PARAMETER_STATE_RECALL)
    suspend fun getStateRecall(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_STATE_RECALL)

    @ParameterGetter(PARAMETER_CALIBRATION_AMPLITUDE)
    suspend fun getCalibrationAmplitude(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_CALIBRATION_AMPLITUDE)

    @ParameterGetter(PARAMETER_CALIBRATION_MODE)
    suspend fun getCalibrationMode(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_CALIBRATION_MODE)

    @ParameterGetter(PARAMETER_ENHANCEMENTS)
    suspend fun getEnhancements(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_ENHANCEMENTS)

    @ParameterGetter(PARAMETER_ERROR)
    suspend fun getError(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_ERROR)

    @ParameterGetter(PARAMETER_EXTERNAL_REFERENCE_LOCK)
    suspend fun getExternalReferenceLock(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_EXTERNAL_REFERENCE_LOCK)

    @ParameterGetter(PARAMETER_LOCAL_ENABLE)
    suspend fun getLocalEnable(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_LOCAL_ENABLE)

    @ParameterGetter(PARAMETER_REMOTE_ENABLE)
    suspend fun getRemoteEnable(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_REMOTE_ENABLE)

    @ParameterGetter(PARAMETER_RESET_DEVICE)
    suspend fun getResetDevice(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_RESET_DEVICE)

    @ParameterGetter(PARAMETER_BYTE_MASK)
    suspend fun getByteMask(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_BYTE_MASK)

    @ParameterGetter(PARAMETER_SRQ_ENABLE)
    suspend fun getSrqEnable(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_SRQ_ENABLE)

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

    @ParameterGetter(PARAMETER_RES_HEAD)
    suspend fun getResHead(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_RES_HEAD)

    @ParameterGetter(PARAMETER_DATA_TRANSFER_MODE)
    suspend fun getDataTransferMode(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_DATA_TRANSFER_MODE)

    @ParameterGetter(PARAMETER_DISPLAY_ENABLE)
    suspend fun getDisplayEnable(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_DISPLAY_ENABLE)

    @ParameterGetter(PARAMETER_DISPLAY_STRING)
    suspend fun getDisplayString(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_DISPLAY_STRING)

}
