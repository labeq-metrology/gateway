package com.labeq.builtin.drivers.hameg.hm107

import com.labeq.communication.MQTTEventBus
import com.labeq.device.DeviceDriver
import com.labeq.device.generic.ParameterGetter
import com.labeq.device.generic.ParameterSetter
import com.labeq.device.generic.oscilloscope.GenericOscilloscope
import com.labeq.transport.MessageResult
import com.labeq.transport.ParameterValue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class HM107Device(
    eventBus: MQTTEventBus, name: String, manufacturer: String, model: String, uuid: Uuid, driver: DeviceDriver
) : GenericOscilloscope(
    eventBus, name, manufacturer, model, uuid, driver
) {

    private val timebaseDecoder by lazy { TimebaseDecoder(this) }
    private val waveformDecoder by lazy { WaveformDecoder(this) }

    companion object {
        // Scope parameters
        const val PARAMETER_AVG_NUMBER_OF_ACQUISITIONS = "scope.acquisition.avg"
        const val PARAMETER_AVG_NUMBER_OF_ACQUISITIONS_SWITCH = "scope.acquisitions.switch"
        const val PARAMETER_WAVEFORM_PREAMBLE = "scope.acquisition.preamble"

        // Timebase
        const val PARAMETER_TIMEBASE_A = "scope.timebase.a"
        const val PARAMETER_TIMEBASE_B = "scope.timebase.b"
        const val PARAMETER_TIMEBASE_A_SECONDS = "scope.timebase.a.seconds"
        const val PARAMETER_TIMEBASE_B_SECONDS = "scope.timebase.b.seconds"

        // Channels
        const val PARAMETER_CHANNEL_1_SETTINGS = "scope.channel.1.settings"
        const val PARAMETER_CHANNEL_2_SETTINGS = "scope.channel.2.settings"
        const val PARAMETER_CHANNEL_1_WAVEFORM = "scope.channel.1.waveform"
        const val PARAMETER_CHANNEL_2_WAVEFORM = "scope.channel.2.waveform"
        const val PARAMETER_CHANNEL_1_WAVEFORM_VOLTAGE = "scope.channel.1.waveform.voltage"
        const val PARAMETER_CHANNEL_2_WAVEFORM_VOLTAGE = "scope.channel.2.waveform.voltage"
        const val PARAMETER_CHANNEL_1_COUPLING = "scope.channel.1.coupling"
        const val PARAMETER_CHANNEL_2_COUPLING = "scope.channel.2.coupling"
        const val PARAMETER_CHANNEL_1_INVERT = "scope.channel.1.invert"
        const val PARAMETER_CHANNEL_2_INVERT = "scope.channel.2.invert"
        const val PARAMETER_CHANNEL_1_ON = "scope.channel.1.on"
        const val PARAMETER_CHANNEL_2_ON = "scope.channel.2.on"
        const val PARAMETER_CHANNEL_1_VOLTPERDIV = "scope.channel.1.vdiv"
        const val PARAMETER_CHANNEL_2_VOLTPERDIV = "scope.channel.2.vdiv"

        // Additional channel/controls
        const val PARAMETER_CHANNEL_1_VAR = "scope.channel.1.var"
        const val PARAMETER_CHANNEL_2_VAR = "scope.channel.2.var"
        const val PARAMETER_DELAY_POSITION = "scope.delaypos"
        const val PARAMETER_INTENSITY_A = "scope.inta"
        const val PARAMETER_INTENSITY_B = "scope.intb"
        const val PARAMETER_INTRO = "scope.intro"
        const val PARAMETER_TRACE_SEPARATION = "scope.trsep"
        const val PARAMETER_VERTICAL_MODE = "scope.vermode"
        const val PARAMETER_HORIZONTAL_MODE = "scope.hormode"
        const val PARAMETER_LOCAL_LOCK = "scope.lk"
        const val PARAMETER_QUICK_START = "scope.quickst"
        const val PARAMETER_HOLD1_POS = "scope.hold1pos"
        const val PARAMETER_HOLD2_POS = "scope.hold2pos"
        const val PARAMETER_CURSOR_CFG = "scope.cursorcfg"
        const val PARAMETER_CURSOR_MODE = "scope.cursormode"
        const val PARAMETER_REF1_POS = "scope.ref1pos"
        const val PARAMETER_REF2_POS = "scope.ref2pos"
        const val PARAMETER_REF1_DATA = "scope.ref1data"
        const val PARAMETER_REF2_DATA = "scope.ref2data"
        const val PARAMETER_DDF = "scope.ddf"
        const val PARAMETER_DDF1 = "scope.ddf1"
        const val PARAMETER_PSINT = "scope.psint"
        const val PARAMETER_PSCH1 = "scope.psch1"
        const val PARAMETER_PSCH2 = "scope.psch2"
        const val PARAMETER_PSY1POS = "scope.psy1pos"
        const val PARAMETER_PSY2POS = "scope.psy2pos"
        const val PARAMETER_PSTB = "scope.pstb"
        const val PARAMETER_MANUAL_OPERATION = "scope.manualop"

        // ID / VERSION
        const val PARAMETER_ID = "scope.id"
        const val PARAMETER_VERSION = "scope.version"

        // Other controls
        const val PARAMETER_BELL = "scope.bell"
        const val PARAMETER_CTRL_BEEP = "scope.ctrlbeep"
        const val PARAMETER_ERR_BEEP = "scope.errbp"
        const val PARAMETER_ERR_MESSAGE = "scope.errmsge"

        const val PARAMETER_READOUT = "scope.readout"
        const val PARAMETER_DC_REF = "scope.dcref"
        const val PARAMETER_MEAN_VALUE = "scope.meanval"
        const val PARAMETER_TRIGGER_SYMBOL = "scope.trgs"
        const val PARAMETER_TRIGGER_VALUE = "scope.trgval"
        const val PARAMETER_X_POSITION = "scope.xpos"
        const val PARAMETER_Y1_POSITION = "scope.y1pos"
        const val PARAMETER_Y2_POSITION = "scope.y2pos"
        const val PARAMETER_HOLD_WAVEFORM = "scope.hldwfm"
        const val PARAMETER_STORE_MODE = "scope.strmode"
        const val PARAMETER_AUTOSET = "scope.autoset"
        const val PARAMETER_RESET = "scope.reset"
        const val PARAMETER_HOLDOFF = "scope.holdoff"
    }

    override val parameters: MutableMap<String, ParameterValue> = super.parameters.apply {
        putAll(
            arrayOf(
                PARAMETER_AVG_NUMBER_OF_ACQUISITIONS to ParameterValue.IntegerValue(0),
                PARAMETER_AVG_NUMBER_OF_ACQUISITIONS_SWITCH to ParameterValue.IntegerValue(0),
                PARAMETER_WAVEFORM_PREAMBLE to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_TIMEBASE_A to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_TIMEBASE_B to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_TIMEBASE_A_SECONDS to ParameterValue.DoubleValue(0.0),
                PARAMETER_TIMEBASE_B_SECONDS to ParameterValue.DoubleValue(0.0),
                PARAMETER_CHANNEL_1_SETTINGS to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_CHANNEL_2_SETTINGS to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_CHANNEL_1_WAVEFORM to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_CHANNEL_2_WAVEFORM to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_CHANNEL_1_WAVEFORM_VOLTAGE to ParameterValue.DoubleArrayValue(doubleArrayOf()),
                PARAMETER_CHANNEL_2_WAVEFORM_VOLTAGE to ParameterValue.DoubleArrayValue(doubleArrayOf()),
                PARAMETER_CHANNEL_1_VAR to ParameterValue.IntegerValue(0),
                PARAMETER_CHANNEL_2_VAR to ParameterValue.IntegerValue(0),
                PARAMETER_DELAY_POSITION to ParameterValue.IntegerValue(0),
                PARAMETER_INTENSITY_A to ParameterValue.IntegerValue(0),
                PARAMETER_INTENSITY_B to ParameterValue.IntegerValue(0),
                PARAMETER_INTRO to ParameterValue.IntegerValue(0),
                PARAMETER_TRACE_SEPARATION to ParameterValue.IntegerValue(0),
                PARAMETER_VERTICAL_MODE to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_HORIZONTAL_MODE to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_LOCAL_LOCK to ParameterValue.IntegerValue(0),
                PARAMETER_QUICK_START to ParameterValue.IntegerValue(0),
                PARAMETER_HOLD1_POS to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_HOLD2_POS to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_CURSOR_CFG to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_CURSOR_MODE to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_REF1_POS to ParameterValue.IntegerValue(0),
                PARAMETER_REF2_POS to ParameterValue.IntegerValue(0),
                PARAMETER_REF1_DATA to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_REF2_DATA to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_DDF to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_DDF1 to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_PSINT to ParameterValue.IntegerValue(0),
                PARAMETER_PSCH1 to ParameterValue.IntegerValue(0),
                PARAMETER_PSCH2 to ParameterValue.IntegerValue(0),
                PARAMETER_PSY1POS to ParameterValue.IntegerValue(0),
                PARAMETER_PSY2POS to ParameterValue.IntegerValue(0),
                PARAMETER_PSTB to ParameterValue.IntegerValue(0),
                PARAMETER_MANUAL_OPERATION to ParameterValue.IntegerValue(0),
                PARAMETER_ID to ParameterValue.StringValue(""),
                PARAMETER_VERSION to ParameterValue.StringValue(""),
                PARAMETER_BELL to ParameterValue.IntegerValue(0),
                PARAMETER_CTRL_BEEP to ParameterValue.IntegerValue(0),
                PARAMETER_ERR_BEEP to ParameterValue.IntegerValue(0),
                PARAMETER_ERR_MESSAGE to ParameterValue.IntegerValue(0),
                PARAMETER_READOUT to ParameterValue.IntegerValue(0),
                PARAMETER_DC_REF to ParameterValue.IntegerValue(0),
                PARAMETER_MEAN_VALUE to ParameterValue.IntegerValue(0),
                PARAMETER_TRIGGER_SYMBOL to ParameterValue.IntegerValue(0),
                PARAMETER_TRIGGER_VALUE to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_X_POSITION to ParameterValue.IntegerValue(0),
                PARAMETER_Y1_POSITION to ParameterValue.IntegerValue(0),
                PARAMETER_Y2_POSITION to ParameterValue.IntegerValue(0),
                PARAMETER_HOLD_WAVEFORM to ParameterValue.IntegerValue(0),
                PARAMETER_STORE_MODE to ParameterValue.IntegerValue(0),
                PARAMETER_HOLDOFF to ParameterValue.IntegerValue(0)
            )
        )
    }

    // Averaging
    @ParameterGetter(PARAMETER_AVG_NUMBER_OF_ACQUISITIONS)
    suspend fun queryAverageNumberOfAcquisitions(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_AVG_NUMBER_OF_ACQUISITIONS)

    @ParameterSetter(PARAMETER_AVG_NUMBER_OF_ACQUISITIONS)
    suspend fun setAverageNumberOfAcquisitions(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_AVG_NUMBER_OF_ACQUISITIONS, value)

    @ParameterGetter(PARAMETER_AVG_NUMBER_OF_ACQUISITIONS_SWITCH)
    suspend fun queryAverageNumberOfAcquisitionsSwitch(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_AVG_NUMBER_OF_ACQUISITIONS_SWITCH)

    @ParameterSetter(PARAMETER_AVG_NUMBER_OF_ACQUISITIONS_SWITCH)
    suspend fun setAverageNumberOfAcquisitionsSwitch(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_AVG_NUMBER_OF_ACQUISITIONS_SWITCH, value)

    // ─────────────────────────────── TIMEBASE ───────────────────────────────

    @ParameterGetter(PARAMETER_TIMEBASE_A_SECONDS)
    suspend fun getTimebaseASeconds(): MessageResult<ParameterValue> {
        val value = timebaseDecoder.getTimebaseSecondsPerDiv("A") ?: return MessageResult.Error("No data")
        return MessageResult.Result(ParameterValue.DoubleValue(value))
    }

    @ParameterSetter(PARAMETER_TIMEBASE_A_SECONDS)
    suspend fun setTimebaseASeconds(value: ParameterValue): MessageResult<ParameterValue> {
        if (value !is ParameterValue.DoubleValue) return MessageResult.Error("Invalid value")
        val ok = timebaseDecoder.setTimebaseSecondsPerDiv("A", value.value)
        return if (ok) MessageResult.Result(value) else MessageResult.Error("Failed to set A")
    }

    @ParameterGetter(PARAMETER_TIMEBASE_B_SECONDS)
    suspend fun getTimebaseBSeconds(): MessageResult<ParameterValue> {
        val value = timebaseDecoder.getTimebaseSecondsPerDiv("B") ?: return MessageResult.Error("No data")
        return MessageResult.Result(ParameterValue.DoubleValue(value))
    }

    @ParameterSetter(PARAMETER_TIMEBASE_B_SECONDS)
    suspend fun setTimebaseBSeconds(value: ParameterValue): MessageResult<ParameterValue> {
        if (value !is ParameterValue.DoubleValue) return MessageResult.Error("Invalid value")
        val ok = timebaseDecoder.setTimebaseSecondsPerDiv("B", value.value)
        return if (ok) MessageResult.Result(value) else MessageResult.Error("Failed to set B")
    }

    // Waveform / Channel settings
    @ParameterGetter(PARAMETER_WAVEFORM_PREAMBLE)
    suspend fun queryWaveformPreamble(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_WAVEFORM_PREAMBLE)

    @ParameterGetter(PARAMETER_CHANNEL_1_SETTINGS)
    suspend fun queryChannel1Settings(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_CHANNEL_1_SETTINGS)

    @ParameterGetter(PARAMETER_CHANNEL_2_SETTINGS)
    suspend fun queryChannel2Settings(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_CHANNEL_2_SETTINGS)

    @ParameterGetter(PARAMETER_CHANNEL_1_WAVEFORM)
    suspend fun queryChannel1Waveform(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_CHANNEL_1_WAVEFORM)

    @ParameterGetter(PARAMETER_CHANNEL_2_WAVEFORM)
    suspend fun queryChannel2Waveform(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_CHANNEL_2_WAVEFORM)

    @ParameterGetter(PARAMETER_CHANNEL_1_WAVEFORM_VOLTAGE)
    suspend fun queryChannel1WaveformVoltage(): MessageResult<ParameterValue.DoubleArrayValue> =
        MessageResult.Result(ParameterValue.DoubleArrayValue(waveformDecoder.decodeVoltages(1).toDoubleArray()))

    @ParameterGetter(PARAMETER_CHANNEL_2_WAVEFORM_VOLTAGE)
    suspend fun queryChannel2WaveformVoltage(): MessageResult<ParameterValue> =
        MessageResult.Result(ParameterValue.DoubleArrayValue(waveformDecoder.decodeVoltages(2).toDoubleArray()))

    @ParameterGetter(PARAMETER_CHANNEL_1_VAR)
    suspend fun queryChannel1Var(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_CHANNEL_1_VAR)

    @ParameterSetter(PARAMETER_CHANNEL_1_VAR)
    suspend fun setChannel1Var(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_CHANNEL_1_VAR, value)

    @ParameterGetter(PARAMETER_CHANNEL_2_VAR)
    suspend fun queryChannel2Var(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_CHANNEL_2_VAR)

    @ParameterSetter(PARAMETER_CHANNEL_2_VAR)
    suspend fun setChannel2Var(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_CHANNEL_2_VAR, value)

    @ParameterGetter(PARAMETER_CHANNEL_1_COUPLING)
    suspend fun queryChannel1Coupling(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_CHANNEL_1_COUPLING)

    @ParameterSetter(PARAMETER_CHANNEL_1_COUPLING)
    suspend fun setChannel1Coupling(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_CHANNEL_1_COUPLING, value)

    @ParameterGetter(PARAMETER_CHANNEL_2_COUPLING)
    suspend fun queryChannel2Coupling(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_CHANNEL_2_COUPLING)

    @ParameterSetter(PARAMETER_CHANNEL_2_COUPLING)
    suspend fun setChannel2Coupling(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_CHANNEL_2_COUPLING, value)

    @ParameterGetter(PARAMETER_CHANNEL_1_INVERT)
    suspend fun queryChannel1Invert(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_CHANNEL_1_INVERT)

    @ParameterSetter(PARAMETER_CHANNEL_1_INVERT)
    suspend fun setChannel1Invert(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_CHANNEL_1_INVERT, value)

    @ParameterGetter(PARAMETER_CHANNEL_2_INVERT)
    suspend fun queryChannel2Invert(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_CHANNEL_2_INVERT)

    @ParameterSetter(PARAMETER_CHANNEL_2_INVERT)
    suspend fun setChannel2Invert(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_CHANNEL_2_INVERT, value)

    @ParameterGetter(PARAMETER_CHANNEL_1_ON)
    suspend fun queryChannel1On(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_CHANNEL_1_ON)

    @ParameterSetter(PARAMETER_CHANNEL_1_ON)
    suspend fun setChannel1On(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_CHANNEL_1_ON, value)

    @ParameterGetter(PARAMETER_CHANNEL_2_ON)
    suspend fun queryChannel2On(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_CHANNEL_2_ON)

    @ParameterSetter(PARAMETER_CHANNEL_2_ON)
    suspend fun setChannel2On(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_CHANNEL_2_ON, value)

    @ParameterGetter(PARAMETER_CHANNEL_1_VOLTPERDIV)
    suspend fun queryChannel1VoltPerDiv(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_CHANNEL_1_VOLTPERDIV)

    @ParameterSetter(PARAMETER_CHANNEL_1_VOLTPERDIV)
    suspend fun setChannel1VoltPerDiv(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_CHANNEL_1_VOLTPERDIV, value)

    @ParameterGetter(PARAMETER_CHANNEL_2_VOLTPERDIV)
    suspend fun queryChannel2VoltPerDiv(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_CHANNEL_2_VOLTPERDIV)

    @ParameterSetter(PARAMETER_CHANNEL_2_VOLTPERDIV)
    suspend fun setChannel2VoltPerDiv(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_CHANNEL_2_VOLTPERDIV, value)

    // Positions / intensity / misc
    @ParameterGetter(PARAMETER_DELAY_POSITION)
    suspend fun queryDelayPosition(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_DELAY_POSITION)

    @ParameterSetter(PARAMETER_DELAY_POSITION)
    suspend fun setDelayPosition(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_DELAY_POSITION, value)

    @ParameterGetter(PARAMETER_INTENSITY_A)
    suspend fun queryIntensityA(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_INTENSITY_A)

    @ParameterSetter(PARAMETER_INTENSITY_A)
    suspend fun setIntensityA(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_INTENSITY_A, value)

    @ParameterGetter(PARAMETER_INTENSITY_B)
    suspend fun queryIntensityB(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_INTENSITY_B)

    @ParameterSetter(PARAMETER_INTENSITY_B)
    suspend fun setIntensityB(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_INTENSITY_B, value)

    @ParameterGetter(PARAMETER_INTRO)
    suspend fun queryIntro(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_INTRO)

    @ParameterSetter(PARAMETER_INTRO)
    suspend fun setIntro(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_INTRO, value)

    @ParameterGetter(PARAMETER_TRACE_SEPARATION)
    suspend fun queryTraceSeparation(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_TRACE_SEPARATION)

    @ParameterSetter(PARAMETER_TRACE_SEPARATION)
    suspend fun setTraceSeparation(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_TRACE_SEPARATION, value)

    @ParameterGetter(PARAMETER_VERTICAL_MODE)
    suspend fun queryVerticalMode(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_VERTICAL_MODE)

    @ParameterSetter(PARAMETER_VERTICAL_MODE)
    suspend fun setVerticalMode(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_VERTICAL_MODE, value)

    @ParameterGetter(PARAMETER_HORIZONTAL_MODE)
    suspend fun queryHorizontalMode(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_HORIZONTAL_MODE)

    @ParameterSetter(PARAMETER_HORIZONTAL_MODE)
    suspend fun setHorizontalMode(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_HORIZONTAL_MODE, value)

    @ParameterGetter(PARAMETER_LOCAL_LOCK)
    suspend fun queryLocalLock(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_LOCAL_LOCK)

    @ParameterSetter(PARAMETER_LOCAL_LOCK)
    suspend fun setLocalLock(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_LOCAL_LOCK, value)

    @ParameterGetter(PARAMETER_QUICK_START)
    suspend fun queryQuickStart(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_QUICK_START)

    @ParameterSetter(PARAMETER_QUICK_START)
    suspend fun setQuickStart(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_QUICK_START, value)

    @ParameterGetter(PARAMETER_HOLD1_POS)
    suspend fun queryHold1Pos(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_HOLD1_POS)

    @ParameterSetter(PARAMETER_HOLD1_POS)
    suspend fun setHold1Pos(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_HOLD1_POS, value)

    @ParameterGetter(PARAMETER_HOLD2_POS)
    suspend fun queryHold2Pos(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_HOLD2_POS)

    @ParameterSetter(PARAMETER_HOLD2_POS)
    suspend fun setHold2Pos(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_HOLD2_POS, value)

    @ParameterGetter(PARAMETER_CURSOR_CFG)
    suspend fun queryCursorCfg(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_CURSOR_CFG)

    @ParameterSetter(PARAMETER_CURSOR_CFG)
    suspend fun setCursorCfg(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_CURSOR_CFG, value)

    @ParameterGetter(PARAMETER_CURSOR_MODE)
    suspend fun queryCursorMode(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_CURSOR_MODE)

    @ParameterSetter(PARAMETER_CURSOR_MODE)
    suspend fun setCursorMode(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_CURSOR_MODE, value)

    @ParameterGetter(PARAMETER_REF1_POS)
    suspend fun queryRef1Pos(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_REF1_POS)

    @ParameterSetter(PARAMETER_REF1_POS)
    suspend fun setRef1Pos(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_REF1_POS, value)

    @ParameterGetter(PARAMETER_REF2_POS)
    suspend fun queryRef2Pos(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_REF2_POS)

    @ParameterSetter(PARAMETER_REF2_POS)
    suspend fun setRef2Pos(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_REF2_POS, value)

    @ParameterGetter(PARAMETER_REF1_DATA)
    suspend fun queryRef1Data(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_REF1_DATA)

    @ParameterSetter(PARAMETER_REF1_DATA)
    suspend fun setRef1Data(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_REF1_DATA, value)

    @ParameterGetter(PARAMETER_REF2_DATA)
    suspend fun queryRef2Data(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_REF2_DATA)

    @ParameterSetter(PARAMETER_REF2_DATA)
    suspend fun setRef2Data(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_REF2_DATA, value)

    @ParameterGetter(PARAMETER_DDF)
    suspend fun queryDDF(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_DDF)

    @ParameterSetter(PARAMETER_DDF)
    suspend fun setDDF(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_DDF, value)

    @ParameterGetter(PARAMETER_DDF1)
    suspend fun queryDDF1(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_DDF1)

    @ParameterSetter(PARAMETER_DDF1)
    suspend fun setDDF1(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_DDF1, value)

    @ParameterGetter(PARAMETER_PSINT)
    suspend fun queryPsInt(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_PSINT)

    @ParameterSetter(PARAMETER_PSINT)
    suspend fun setPsInt(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_PSINT, value)

    @ParameterGetter(PARAMETER_PSCH1)
    suspend fun queryPsCh1(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_PSCH1)

    @ParameterSetter(PARAMETER_PSCH1)
    suspend fun setPsCh1(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_PSCH1, value)

    @ParameterGetter(PARAMETER_PSCH2)
    suspend fun queryPsCh2(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_PSCH2)

    @ParameterSetter(PARAMETER_PSCH2)
    suspend fun setPsCh2(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_PSCH2, value)

    @ParameterGetter(PARAMETER_PSY1POS)
    suspend fun queryPsY1Pos(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_PSY1POS)

    @ParameterSetter(PARAMETER_PSY1POS)
    suspend fun setPsY1Pos(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_PSY1POS, value)

    @ParameterGetter(PARAMETER_PSY2POS)
    suspend fun queryPsY2Pos(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_PSY2POS)

    @ParameterSetter(PARAMETER_PSY2POS)
    suspend fun setPsY2Pos(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_PSY2POS, value)

    @ParameterGetter(PARAMETER_PSTB)
    suspend fun queryPstb(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_PSTB)

    @ParameterSetter(PARAMETER_PSTB)
    suspend fun setPstb(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_PSTB, value)

    @ParameterGetter(PARAMETER_MANUAL_OPERATION)
    suspend fun queryManualOperation(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_MANUAL_OPERATION)

    // ID / version
    @ParameterGetter(PARAMETER_ID)
    suspend fun queryId(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_ID)

    @ParameterGetter(PARAMETER_VERSION)
    suspend fun queryVersion(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_VERSION)

    // Beeps / control setters
    @ParameterSetter(PARAMETER_BELL)
    suspend fun setBell(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_BELL, value)

    @ParameterGetter(PARAMETER_CTRL_BEEP)
    suspend fun queryControlBeep(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_CTRL_BEEP)

    @ParameterSetter(PARAMETER_CTRL_BEEP)
    suspend fun setControlBeep(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_CTRL_BEEP, value)

    @ParameterGetter(PARAMETER_ERR_BEEP)
    suspend fun queryErrorBeep(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_ERR_BEEP)

    @ParameterSetter(PARAMETER_ERR_BEEP)
    suspend fun setErrorBeep(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_ERR_BEEP, value)

    @ParameterGetter(PARAMETER_ERR_MESSAGE)
    suspend fun queryErrorMessage(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_ERR_MESSAGE)

    @ParameterSetter(PARAMETER_ERR_MESSAGE)
    suspend fun setErrorMessage(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_ERR_MESSAGE, value)

    // Readout / display setters
    @ParameterGetter(PARAMETER_READOUT)
    suspend fun queryReadout(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_READOUT)

    @ParameterSetter(PARAMETER_READOUT)
    suspend fun setReadout(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_READOUT, value)

    @ParameterGetter(PARAMETER_DC_REF)
    suspend fun queryDcRef(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_DC_REF)

    @ParameterSetter(PARAMETER_DC_REF)
    suspend fun setDcRef(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_DC_REF, value)

    @ParameterGetter(PARAMETER_MEAN_VALUE)
    suspend fun queryMeanValue(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_MEAN_VALUE)

    @ParameterSetter(PARAMETER_MEAN_VALUE)
    suspend fun setMeanValue(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_MEAN_VALUE, value)

    // Trigger-related
    @ParameterGetter(PARAMETER_TRIGGER_SYMBOL)
    suspend fun queryTriggerSymbol(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_TRIGGER_SYMBOL)

    @ParameterSetter(PARAMETER_TRIGGER_SYMBOL)
    suspend fun setTriggerSymbol(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_TRIGGER_SYMBOL, value)

    @ParameterGetter(PARAMETER_TRIGGER_VALUE)
    suspend fun queryTriggerValueBytes(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_TRIGGER_VALUE)

    @ParameterGetter(PARAMETER_X_POSITION)
    suspend fun queryXpos(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_X_POSITION)

    @ParameterSetter(PARAMETER_X_POSITION)
    suspend fun setXpos(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_X_POSITION, value)

    @ParameterGetter(PARAMETER_Y1_POSITION)
    suspend fun queryY1pos(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_Y1_POSITION)

    @ParameterSetter(PARAMETER_Y1_POSITION)
    suspend fun setY1pos(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_Y1_POSITION, value)

    @ParameterGetter(PARAMETER_Y2_POSITION)
    suspend fun queryY2pos(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_Y2_POSITION)

    @ParameterSetter(PARAMETER_Y2_POSITION)
    suspend fun setY2pos(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_Y2_POSITION, value)

    @ParameterGetter(PARAMETER_HOLD_WAVEFORM)
    suspend fun queryHoldWaveform(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_HOLD_WAVEFORM)

    @ParameterSetter(PARAMETER_HOLD_WAVEFORM)
    suspend fun setHoldWaveform(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_HOLD_WAVEFORM, value)

    @ParameterGetter(PARAMETER_STORE_MODE)
    suspend fun queryStoreMode(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_STORE_MODE)

    @ParameterSetter(PARAMETER_STORE_MODE)
    suspend fun setStoreMode(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_STORE_MODE, value)

    @ParameterSetter(PARAMETER_AUTOSET)
    override suspend fun autoset(@Suppress("UNUSED_PARAMETER") value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_AUTOSET, value)

    @ParameterSetter(PARAMETER_RESET)
    suspend fun resetScope(@Suppress("UNUSED_PARAMETER") value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_RESET, value)
}
