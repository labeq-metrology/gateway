package com.labeq.builtin.drivers.hameg.hm107

import com.labeq.device.DeviceDriver
import com.labeq.transport.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.math.pow

class HM107Driver(override val connection: DeviceConnection) : DeviceDriver(connection) {

    private companion object {
        const val CRLF = "\r\n"
        const val REMOTE_ENABLE = " \r"
        const val REMOTE_DISABLE = "RM0$CRLF"
        const val AUTOSET = "AUTOSET$CRLF"
        const val RES = "RES$CRLF"

        const val AVG_ACQUISITIONS = "AVRNM"
        const val AVG_ACQUISITIONS_SET = "$AVG_ACQUISITIONS="
        const val AVG_ACQUISITIONS_GET = "$AVG_ACQUISITIONS?$CRLF"
        const val AVG_ACQ_SWITCH = "${AVG_ACQUISITIONS}SW"
        const val AVG_ACQ_SWITCH_GET = "$AVG_ACQ_SWITCH?"
        const val AVG_ACQ_SWITCH_SET = "$AVG_ACQ_SWITCH="

        const val CH1 = "CH1"
        const val CH2 = "CH2"

        const val WFMPRE = "WFMPRE"
        const val RDWFM = "RDWFM"

        const val ID = "ID?"
        const val VERS = "VERS?"

        const val BELL = "BELL"
        const val CTRLBP = "CTRLBP"
        const val ERRBP = "ERRBP"
        const val ERRMSGE = "ERRMSGE"

        const val READOUT = "READOUT"
        const val DCREF = "DCREF"
        const val MEANVAL = "MEANVAL"

        const val TRGS = "TRGS"
        const val TRGVAL = "TRGVAL?"
        const val TRGLEV1 = "TRGLEV1"
        const val TRGLEV2 = "TRGLEV2"
        const val TRGSTA = "TRGSTA"
        const val TRGMODE = "TRIG"

        const val XPOS = "XPOS"
        const val Y1POS = "Y1POS"
        const val Y2POS = "Y2POS"
        const val TB = "TB"

        const val HLDWFM = "HLDWFM"
        const val HLD1POS = "HLD1POS"
        const val HLD2POS = "HLD2POS"
        const val STRMODE = "STRMODE"
        const val SAVEDF = "SAVEDF"
        const val SAVREF1 = "SAVREF1"
        const val SAVREF2 = "SAVREF2"

        const val HOLDOFF = "HOLDOFF"

        // Additional / corrected according to PDF
        const val CHVAR = "CH"
        const val DELPOS = "DELPOS"
        const val INTRO = "INTRO"
        const val TRSEP = "TRSEP"
        const val VERMODE = "VERMODE"
        const val HORMODE = "HORMODE"
        const val LK = "LK"
        const val QUICKST = "QUICKST"
        const val CURCFG = "CURCFG"
        const val CURMOD1 = "CURMOD1"
        const val DDF = "DDF"
        const val DDF1 = "DDF1"
        const val PSINT = "PSINT"
        const val PS1CH = "PS1CH"
        const val PS2CH = "PS2CH"
        const val PSY1POS = "PSY1POS"
        const val PSY2POS = "PSY2POS"
        const val PSTB = "PSTB"
        const val FCCMD = "FCCMD"
        const val INT = "INT"
        const val REFPRE = "REFPRE"
        const val REFPRE1 = "${REFPRE}1"
        const val REFPRE2 = "${REFPRE}2"
        const val RDREF = "RDREF"
        const val RDREF1 = "RDREF1"
        const val RDREF2 = "RDREF2"
    }

    override suspend fun queryParameter(parameter: String, returnType: ReturnType): MessageResult<ParameterValue> {
        return when (parameter) {
            HM107Device.PARAMETER_TIMEBASE_A -> queryTimebase("A")
            HM107Device.PARAMETER_TIMEBASE_B -> queryTimebase("B")
            HM107Device.PARAMETER_AVG_NUMBER_OF_ACQUISITIONS -> queryAvgAcquisitions()
            HM107Device.PARAMETER_AVG_NUMBER_OF_ACQUISITIONS_SWITCH -> queryAvgAcquisitionsSwitch()
            HM107Device.PARAMETER_CHANNEL_1_SETTINGS -> queryChannelSettings(1)
            HM107Device.PARAMETER_CHANNEL_2_SETTINGS -> queryChannelSettings(2)
            HM107Device.PARAMETER_WAVEFORM_PREAMBLE -> queryWaveformPreamble()
            HM107Device.PARAMETER_CHANNEL_1_WAVEFORM -> queryWaveform(1)
            HM107Device.PARAMETER_CHANNEL_2_WAVEFORM -> queryWaveform(2)
            HM107Device.PARAMETER_ID -> queryId()
            HM107Device.PARAMETER_VERSION -> queryVersion()
            HM107Device.PARAMETER_CTRL_BEEP -> queryControlBeep()
            HM107Device.PARAMETER_ERR_BEEP -> queryErrorBeep()
            HM107Device.PARAMETER_ERR_MESSAGE -> queryErrorMessage()
            HM107Device.PARAMETER_READOUT -> queryReadout()
            HM107Device.PARAMETER_DC_REF -> queryDcRef()
            HM107Device.PARAMETER_MEAN_VALUE -> queryMeanValue()
            HM107Device.PARAMETER_TRIGGER_SYMBOL -> queryTriggerSymbol()
            HM107Device.PARAMETER_TRIGGER_VALUE -> queryTriggerValue()
            HM107Device.PARAMETER_X_POSITION -> queryXPosition()
            HM107Device.PARAMETER_Y1_POSITION -> queryYPosition(1)
            HM107Device.PARAMETER_Y2_POSITION -> queryYPosition(2)
            HM107Device.PARAMETER_HOLD_WAVEFORM -> queryHoldWaveform()
            HM107Device.PARAMETER_STORE_MODE -> queryStoreMode()
            HM107Device.PARAMETER_HOLDOFF -> queryHoldoff()
            HM107Device.PARAMETER_CHANNEL_1_VAR -> queryChannelVar(1)
            HM107Device.PARAMETER_CHANNEL_2_VAR -> queryChannelVar(2)
            HM107Device.PARAMETER_DELAY_POSITION -> queryDelayPosition()
            HM107Device.PARAMETER_INTENSITY_A -> queryIntensity("A")
            HM107Device.PARAMETER_INTENSITY_B -> queryIntensity("B")
            HM107Device.PARAMETER_INTRO -> queryIntro()
            HM107Device.PARAMETER_TRACE_SEPARATION -> queryTraceSeparation()
            HM107Device.PARAMETER_VERTICAL_MODE -> queryVerticalMode()
            HM107Device.PARAMETER_HORIZONTAL_MODE -> queryHorizontalMode()
            HM107Device.PARAMETER_LOCAL_LOCK -> queryLocalLock()
            HM107Device.PARAMETER_QUICK_START -> queryQuickStart()
            HM107Device.PARAMETER_HOLD1_POS -> queryHoldPos(1)
            HM107Device.PARAMETER_HOLD2_POS -> queryHoldPos(2)
            HM107Device.PARAMETER_CURSOR_CFG -> queryCursorCfg()
            HM107Device.PARAMETER_CURSOR_MODE -> queryCursorMode()
            HM107Device.PARAMETER_REF1_POS -> queryRef1Pos()
            HM107Device.PARAMETER_REF2_POS -> queryRef2Pos()
            HM107Device.PARAMETER_REF1_DATA -> queryRef1Data()
            HM107Device.PARAMETER_REF2_DATA -> queryRef2Data()
            HM107Device.PARAMETER_DDF -> queryDDF()
            HM107Device.PARAMETER_DDF1 -> queryDDF1()
            HM107Device.PARAMETER_PSINT -> queryPsInt()
            HM107Device.PARAMETER_PSCH1 -> queryPsCh1()
            HM107Device.PARAMETER_PSCH2 -> queryPsCh2()
            HM107Device.PARAMETER_PSY1POS -> queryPsY1Pos()
            HM107Device.PARAMETER_PSY2POS -> queryPsY2Pos()
            HM107Device.PARAMETER_PSTB -> queryPstb()
            HM107Device.PARAMETER_MANUAL_OPERATION -> queryManualOperation()
            HM107Device.PARAMETER_CHANNEL_1_COUPLING -> queryChannelCoupling(1)
            HM107Device.PARAMETER_CHANNEL_2_COUPLING -> queryChannelCoupling(2)
            HM107Device.PARAMETER_CHANNEL_1_INVERT -> queryChannelInvert(1)
            HM107Device.PARAMETER_CHANNEL_2_INVERT -> queryChannelInvert(2)
            HM107Device.PARAMETER_CHANNEL_1_ON -> queryChannelOn(1)
            HM107Device.PARAMETER_CHANNEL_2_ON -> queryChannelOn(2)
            HM107Device.PARAMETER_CHANNEL_1_VOLTPERDIV -> queryChannelVoltPerDiv(1)
            HM107Device.PARAMETER_CHANNEL_2_VOLTPERDIV -> queryChannelVoltPerDiv(2)
            else -> MessageResult.Error("Invalid parameter: $parameter")
        }
    }

    override suspend fun writeParameter(
        parameter: String, value: ParameterValue, returnType: ReturnType
    ): MessageResult<ParameterValue> {
        return when (parameter) {
            HM107Device.PARAMETER_TIMEBASE_A -> setTimebase("A", value as ParameterValue.BytesValue)
            HM107Device.PARAMETER_TIMEBASE_B -> setTimebase("B", value as ParameterValue.BytesValue)
            HM107Device.PARAMETER_AUTOSET -> autoset()
            HM107Device.PARAMETER_RESET -> resetScope()
            HM107Device.PARAMETER_AVG_NUMBER_OF_ACQUISITIONS -> setAvgAcquisitions(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_AVG_NUMBER_OF_ACQUISITIONS_SWITCH -> setAvgAcquisitionsSwitch(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_CHANNEL_1_SETTINGS -> setChannelSettings(1, value)
            HM107Device.PARAMETER_CHANNEL_2_SETTINGS -> setChannelSettings(2, value)
            HM107Device.PARAMETER_BELL -> setBell(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_CTRL_BEEP -> setControlBeep(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_ERR_BEEP -> setErrorBeep(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_ERR_MESSAGE -> setErrorMessage(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_READOUT -> setReadout(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_DC_REF -> setDcRef(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_MEAN_VALUE -> setMeanValue(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_TRIGGER_SYMBOL -> setTriggerSymbol(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_X_POSITION -> setXPosition(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_Y1_POSITION -> setYPosition(1, value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_Y2_POSITION -> setYPosition(2, value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_HOLD_WAVEFORM -> setHoldWaveform(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_STORE_MODE -> setStoreMode(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_HOLDOFF -> setHoldoff(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_CHANNEL_1_VAR -> setChannelVar(1, value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_CHANNEL_2_VAR -> setChannelVar(2, value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_DELAY_POSITION -> setDelayPosition(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_INTENSITY_A -> setIntensity("A", value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_INTENSITY_B -> setIntensity("B", value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_INTRO -> setIntro(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_TRACE_SEPARATION -> setTraceSeparation(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_VERTICAL_MODE -> setVerticalMode(value)
            HM107Device.PARAMETER_HORIZONTAL_MODE -> setHorizontalMode(value)
            HM107Device.PARAMETER_LOCAL_LOCK -> setLocalLock(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_QUICK_START -> setQuickStart(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_HOLD1_POS -> setHoldPos(1, value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_HOLD2_POS -> setHoldPos(2, value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_CURSOR_CFG -> setCursorCfg(value)
            HM107Device.PARAMETER_CURSOR_MODE -> setCursorMode(value)
            HM107Device.PARAMETER_REF1_POS -> setRef1Pos(value)
            HM107Device.PARAMETER_REF2_POS -> setRef2Pos(value)
            HM107Device.PARAMETER_REF1_DATA -> setRef1Data(value)
            HM107Device.PARAMETER_REF2_DATA -> setRef2Data(value)
            HM107Device.PARAMETER_DDF -> setDDF(value)
            HM107Device.PARAMETER_DDF1 -> setDDF1(value)
            HM107Device.PARAMETER_PSINT -> setPsInt(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_PSCH1 -> setPsCh1(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_PSCH2 -> setPsCh2(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_PSY1POS -> setPsY1Pos(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_PSY2POS -> setPsY2Pos(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_PSTB -> setPstb(value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_CHANNEL_1_COUPLING -> setChannelCoupling(1, value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_CHANNEL_2_COUPLING -> setChannelCoupling(2, value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_CHANNEL_1_INVERT -> setChannelInvert(1, value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_CHANNEL_2_INVERT -> setChannelInvert(2, value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_CHANNEL_1_ON -> setChannelOn(1, value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_CHANNEL_2_ON -> setChannelOn(2, value as ParameterValue.IntegerValue)
            HM107Device.PARAMETER_CHANNEL_1_VOLTPERDIV -> setChannelVoltPerDiv(1, value as ParameterValue.DoubleValue)
            HM107Device.PARAMETER_CHANNEL_2_VOLTPERDIV -> setChannelVoltPerDiv(2, value as ParameterValue.DoubleValue)
            else -> MessageResult.Error("Invalid parameter: $parameter")
        }
    }

    override suspend fun writeData(data: ByteArray, returnType: ReturnType): MessageResult<ParameterValue> =
        connection.query(data, -1, returnType)

    override suspend fun writeData(data: String, returnType: ReturnType): MessageResult<ParameterValue> =
        connection.query(data, -1, returnType)

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

    private suspend fun resetRemote(): Boolean {
        val result = connection.query(REMOTE_ENABLE, -1)
        return when (result) {
            is MessageResult.Error -> {
                connection.query(REMOTE_DISABLE, -1)
//                delay(5)
                return when (connection.query(REMOTE_ENABLE, -1)) {
                    is MessageResult.Error -> false
                    is MessageResult.Result<*> -> true
                }
            }

            is MessageResult.Result -> true
        }
    }

    private suspend fun autoset(): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query(AUTOSET, -1).onSuccessAsync { return MessageResult.Result(ParameterValue.IntegerValue(1)) }
        return MessageResult.Error("Autoset failed")
    }

    private suspend fun resetScope(): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query(RES, -1).onSuccessAsync { return MessageResult.Result(ParameterValue.IntegerValue(1)) }
        return MessageResult.Error("Reset failed")
    }

    private suspend fun queryId(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query(ID, -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) return MessageResult.Result(it)
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryVersion(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query(VERS, -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) return MessageResult.Result(it)
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryAvgAcquisitions(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query(AVG_ACQUISITIONS_GET, 7, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue) {
                if (it.value.size < 7) return MessageResult.Error("Invalid response")
                val exponent = it.value[6].toInt() and 0xFF
                val count = (2.0.pow(exponent)).toInt()
                return MessageResult.Result(ParameterValue.IntegerValue(count))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setAvgAcquisitions(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        if (value.value !in 1..9) return MessageResult.Error("Invalid value: AVRNM must be 1..9")
        connection.send(AVG_ACQUISITIONS_SET)
        connection.query(byteArrayOf(value.value.toByte()), -1).onSuccessAsync { return queryAvgAcquisitions() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryAvgAcquisitionsSwitch(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query(AVG_ACQ_SWITCH_GET, -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setAvgAcquisitionsSwitch(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        if (value.value !in 0..1) return MessageResult.Error("Invalid value")
        connection.query("$AVG_ACQ_SWITCH_SET${value.value}$CRLF", -1)
            .onSuccessAsync { return queryAvgAcquisitionsSwitch() }
        return MessageResult.Error("Invalid response")
    }

    // ─────────────────────────────── TIMEBASE ───────────────────────────────

    private suspend fun queryTimebase(id: String): MessageResult<ParameterValue.BytesValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        val cmd = "TB${id.uppercase()}?"
        val result = connection.query(cmd, 5, ReturnType.Bytes).getOrNull()
        return if (result is ParameterValue.BytesValue && result.value.isNotEmpty())
            MessageResult.Result(ParameterValue.BytesValue(byteArrayOf(result.value.last())))
        else MessageResult.Error("Invalid response")
    }

    private suspend fun setTimebase(id: String, value: ParameterValue.BytesValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        val cmd = "TB${id.uppercase()}=${value.value.decodeToString()}$CRLF"
        connection.query(cmd, -1)
        return queryTimebase(id)
    }

    private suspend fun queryChannelSettings(channel: Int): MessageResult<ParameterValue.BytesValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        val cmd = if (channel == 1) "$CH1?" else "$CH2?"
        connection.query(cmd, 5, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue) {
                val data = if (it.value.size > 4) it.value.drop(4).toByteArray() else it.value
                return MessageResult.Result(ParameterValue.BytesValue(data))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setChannelSettings(channel: Int, value: ParameterValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        val cmd = if (channel == 1) "$CH1=" else "$CH2="
        connection.send(cmd)
        when (value) {
            is ParameterValue.BytesValue -> {
                connection.query(value.value, -1).onSuccessAsync { return queryChannelSettings(channel) }
                return MessageResult.Error("Invalid response")
            }

            is ParameterValue.IntegerValue -> {
                connection.query(byteArrayOf(value.value.toByte()), -1)
                    .onSuccessAsync { return queryChannelSettings(channel) }
                return MessageResult.Error("Invalid response")
            }

            else -> return MessageResult.Error("Invalid value type for channel settings")
        }
    }

    private suspend fun queryChannelVar(channel: Int): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        val cmd = "CH${channel}VAR?"
        connection.query(cmd, 5, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue) {
                val last = it.value.lastOrNull()?.toInt() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last and 0xFF))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setChannelVar(channel: Int, value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        val cmd = "CH${channel}VAR="
        connection.query("$cmd${value.value}$CRLF", -1).onSuccessAsync { return queryChannelVar(channel) }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryChannelCoupling(channel: Int): MessageResult<ParameterValue.IntegerValue> {
        val settingsRv = queryChannelSettings(channel).getOrNull() ?: return MessageResult.Error("No response")
        if (settingsRv !is ParameterValue.BytesValue || settingsRv.value.isEmpty())
            return MessageResult.Error("Invalid data")
        val settings = ChannelSettings.fromByte(settingsRv.value.first())
        val mode = when {
            settings.gnd -> 2
            settings.ac -> 1
            else -> 0
        }
        return MessageResult.Result(ParameterValue.IntegerValue(mode))
    }

    private suspend fun setChannelCoupling(
        channel: Int,
        value: ParameterValue.IntegerValue
    ): MessageResult<ParameterValue> {
        val settingsRv = queryChannelSettings(channel).getOrNull() ?: return MessageResult.Error("No response")
        if (settingsRv.value.isEmpty())
            return MessageResult.Error("Invalid data")
        val current = ChannelSettings.fromByte(settingsRv.value.first())
        val updated = when (value.value) {
            2 -> current.copy(gnd = true, ac = false)
            1 -> current.copy(gnd = false, ac = true)
            else -> current.copy(gnd = false, ac = false)
        }
        return setChannelSettings(channel, ParameterValue.BytesValue(byteArrayOf(ChannelSettings.toByte(updated))))
    }

    private suspend fun queryChannelInvert(channel: Int): MessageResult<ParameterValue.IntegerValue> {
        val settingsRv = queryChannelSettings(channel).getOrNull() ?: return MessageResult.Error("No response")
        if (settingsRv.value.isEmpty())
            return MessageResult.Error("Invalid data")
        val settings = ChannelSettings.fromByte(settingsRv.value.first())
        return MessageResult.Result(ParameterValue.IntegerValue(if (settings.inv) 1 else 0))
    }

    private suspend fun setChannelInvert(
        channel: Int,
        value: ParameterValue.IntegerValue
    ): MessageResult<ParameterValue> {
        val settingsRv = queryChannelSettings(channel).getOrNull() ?: return MessageResult.Error("No response")
        if (settingsRv.value.isEmpty())
            return MessageResult.Error("Invalid data")
        val current = ChannelSettings.fromByte(settingsRv.value.first())
        val updated = current.copy(inv = value.value != 0)
        return setChannelSettings(channel, ParameterValue.BytesValue(byteArrayOf(ChannelSettings.toByte(updated))))
    }

    private suspend fun queryChannelOn(channel: Int): MessageResult<ParameterValue.IntegerValue> {
        val settingsRv = queryChannelSettings(channel).getOrNull() ?: return MessageResult.Error("No response")
        if (settingsRv.value.isEmpty())
            return MessageResult.Error("Invalid data")
        val settings = ChannelSettings.fromByte(settingsRv.value.first())
        return MessageResult.Result(ParameterValue.IntegerValue(if (settings.on) 1 else 0))
    }

    private suspend fun setChannelOn(channel: Int, value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        val settingsRv = queryChannelSettings(channel).getOrNull() ?: return MessageResult.Error("No response")
        if (settingsRv.value.isEmpty())
            return MessageResult.Error("Invalid data")
        val current = ChannelSettings.fromByte(settingsRv.value.first())
        val updated = current.copy(on = value.value != 0)
        return setChannelSettings(channel, ParameterValue.BytesValue(byteArrayOf(ChannelSettings.toByte(updated))))
    }

    private suspend fun queryChannelVoltPerDiv(channel: Int): MessageResult<ParameterValue.DoubleValue> {
        val settingsRv = queryChannelSettings(channel).getOrNull() ?: return MessageResult.Error("No response")
        if (settingsRv.value.isEmpty())
            return MessageResult.Error("Invalid data")
        val settings = ChannelSettings.fromByte(settingsRv.value.first())
        return MessageResult.Result(ParameterValue.DoubleValue(settings.voltPerDivMapped))
    }

    private suspend fun setChannelVoltPerDiv(
        channel: Int,
        value: ParameterValue.DoubleValue
    ): MessageResult<ParameterValue> {
        val settingsRv = queryChannelSettings(channel).getOrNull() ?: return MessageResult.Error("No response")
        if (settingsRv.value.isEmpty())
            return MessageResult.Error("Invalid data")
        val current = ChannelSettings.fromByte(settingsRv.value.first())
        val code = ChannelSettings.codeFromVolt(value.value)
        val updated = current.copy(
            voltPerDivCode = code,
            voltPerDivMapped = ChannelSettings.fromByte(ChannelSettings.toByte(current.copy(voltPerDivCode = code))).voltPerDivMapped
        )
        return setChannelSettings(channel, ParameterValue.BytesValue(byteArrayOf(ChannelSettings.toByte(updated))))
    }

    private suspend fun queryWaveformPreamble(): MessageResult<ParameterValue.BytesValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$WFMPRE?", 17, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue) {
                val payload = if (it.value.size > 7) it.value.drop(7).toByteArray() else it.value
                return MessageResult.Result(ParameterValue.BytesValue(payload))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryWaveform(channel: Int): MessageResult<ParameterValue.BytesValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        if (channel != 1 && channel != 2) return MessageResult.Error("Invalid channel")
        val cmdText = "$RDWFM$channel:"
        connection.send(cmdText)
        connection.send(byteArrayOf(0, 0, 0, 8))
        connection.query(CRLF, 2048 + 11, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue) {
                // Hameg's SP107E skips 13 bytes, no idea why, possibly firmware compatibility issue
                val payload = if (it.value.size > 11) it.value.drop(11).toByteArray() else byteArrayOf()
                return MessageResult.Result(ParameterValue.BytesValue(payload))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setBell(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        if (value.value !in 0..6) return MessageResult.Error("Invalid BELL value")
        connection.query("$BELL=${value.value}$CRLF", -1)
            .onSuccessAsync { return MessageResult.Result(ParameterValue.IntegerValue(1)) }
        return MessageResult.Error("Bell failed")
    }

    private suspend fun queryControlBeep(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$CTRLBP?", -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setControlBeep(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        if (value.value !in 0..1) return MessageResult.Error("Invalid CTRLBP value")
        connection.query("$CTRLBP=${value.value}$CRLF", -1)
            .onSuccessAsync { return MessageResult.Result(ParameterValue.IntegerValue(value.value)) }
        return MessageResult.Error("Failed to set CTRLBP")
    }

    private suspend fun queryErrorBeep(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$ERRBP?", -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setErrorBeep(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        if (value.value !in 0..6) return MessageResult.Error("Invalid ERRBP value")
        connection.query("$ERRBP=${value.value}$CRLF", -1)
            .onSuccessAsync { return MessageResult.Result(ParameterValue.IntegerValue(value.value)) }
        return MessageResult.Error("Failed to set ERRBP")
    }

    private suspend fun queryErrorMessage(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$ERRMSGE?", -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setErrorMessage(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        if (value.value !in 0..1) return MessageResult.Error("Invalid ERRMSGE value")
        connection.query("$ERRMSGE=${value.value}$CRLF", -1)
            .onSuccessAsync { return MessageResult.Result(ParameterValue.IntegerValue(value.value)) }
        return MessageResult.Error("Failed to set ERRMSGE")
    }

    private suspend fun queryReadout(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$READOUT?", -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setReadout(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        if (value.value !in 0..1) return MessageResult.Error("Invalid READOUT value")
        connection.query("$READOUT=${value.value}$CRLF", -1)
            .onSuccessAsync { return MessageResult.Result(ParameterValue.IntegerValue(value.value)) }
        return MessageResult.Error("Failed to set READOUT")
    }

    private suspend fun queryDcRef(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$DCREF?", -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setDcRef(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        if (value.value !in 0..1) return MessageResult.Error("Invalid DCREF value")
        connection.query("$DCREF=${value.value}$CRLF", -1)
            .onSuccessAsync { return MessageResult.Result(ParameterValue.IntegerValue(value.value)) }
        return MessageResult.Error("Failed to set DCREF")
    }

    private suspend fun queryMeanValue(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$MEANVAL?", -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setMeanValue(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        if (value.value !in 0..1) return MessageResult.Error("Invalid MEANVAL value")
        connection.query("$MEANVAL=${value.value}$CRLF", -1)
            .onSuccessAsync { return MessageResult.Result(ParameterValue.IntegerValue(value.value)) }
        return MessageResult.Error("Failed to set MEANVAL")
    }

    private suspend fun queryIntro(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$INTRO?", -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setIntro(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.send("$INTRO=")
        connection.query(byteArrayOf(value.value.toByte()), -1).onSuccessAsync { return queryIntro() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryIntensity(which: String): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        val cmd = "INT${which.uppercase()}?"
        connection.query(cmd, 5, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue && it.value.isNotEmpty()) {
                return MessageResult.Result(ParameterValue.IntegerValue(it.value.last().toInt() and 0xFF))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setIntensity(which: String, value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.send("INT${which.uppercase()}=")
        connection.query(byteArrayOf(value.value.toByte()), -1).onSuccessAsync { return queryIntensity(which) }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryTraceSeparation(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$TRSEP?", -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setTraceSeparation(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$TRSEP=${value.value}$CRLF", -1).onSuccessAsync { return queryTraceSeparation() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryTriggerSymbol(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$TRGS?", -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setTriggerSymbol(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        if (value.value !in 0..1) return MessageResult.Error("Invalid TRGS value")
        connection.query("$TRGS=${value.value}$CRLF", -1)
            .onSuccessAsync { return MessageResult.Result(ParameterValue.IntegerValue(value.value)) }
        return MessageResult.Error("Failed to set TRGS")
    }

    private suspend fun queryTriggerValue(): MessageResult<ParameterValue.BytesValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query(TRGVAL, 15, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue) return MessageResult.Result(it)
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryXPosition(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$XPOS?", 5, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue) {
                val b = it.value
                if (b.size >= 2) {
                    val low = b[b.size - 2].toInt() and 0xFF
                    val high = b[b.size - 1].toInt() and 0xFF
                    val signed = toSigned16(low, high)
                    return MessageResult.Result(ParameterValue.IntegerValue(signed))
                }
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setXPosition(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.send("$XPOS=")
        connection.query(wordToBytes(value.value), -1).onSuccessAsync { return queryXPosition() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryYPosition(channel: Int): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        val cmd = if (channel == 1) "$Y1POS?" else "$Y2POS?"
        connection.query(cmd, 5, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue) {
                val b = it.value
                if (b.size >= 2) {
                    val low = b[b.size - 2].toInt() and 0xFF
                    val high = b[b.size - 1].toInt() and 0xFF
                    val signed = toSigned16(low, high)
                    return MessageResult.Result(ParameterValue.IntegerValue(signed))
                }
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setYPosition(channel: Int, value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        val cmd = if (channel == 1) "$Y1POS=" else "$Y2POS="
        connection.send(cmd)
        connection.query(wordToBytes(value.value), -1).onSuccessAsync { return queryYPosition(channel) }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryDelayPosition(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$DELPOS?", 5, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue && it.value.size >= 2) {
                val b = it.value
                val low = b[b.size - 2].toInt() and 0xFF
                val high = b[b.size - 1].toInt() and 0xFF
                return MessageResult.Result(ParameterValue.IntegerValue(toSigned16(low, high)))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setDelayPosition(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.send("$DELPOS=")
        connection.query(wordToBytes(value.value), -1).onSuccessAsync { return queryDelayPosition() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryVerticalMode(): MessageResult<ParameterValue.BytesValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$VERMODE?", 4, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue) return MessageResult.Result(it)
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setVerticalMode(value: ParameterValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.send("$VERMODE=")
        val data = (value as? ParameterValue.BytesValue)?.value ?: byteArrayOf()
        connection.query(data, -1).onSuccessAsync { return queryVerticalMode() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryHorizontalMode(): MessageResult<ParameterValue.BytesValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$HORMODE?", 4, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue) return MessageResult.Result(it)
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setHorizontalMode(value: ParameterValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.send("$HORMODE=")
        val data = (value as? ParameterValue.BytesValue)?.value ?: byteArrayOf()
        connection.query(data, -1).onSuccessAsync { return queryHorizontalMode() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryHoldWaveform(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$HLDWFM?", -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setHoldWaveform(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$HLDWFM=${value.value}$CRLF", -1).onSuccessAsync { return queryHoldWaveform() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryStoreMode(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$STRMODE?", -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setStoreMode(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$STRMODE=${value.value}$CRLF", -1).onSuccessAsync { return queryStoreMode() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryHoldPos(channel: Int): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        val cmd = if (channel == 1) "$HLD1POS?" else "$HLD2POS?"
        connection.query(cmd, 5, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue && it.value.size >= 2) {
                val b = it.value
                val low = b[b.size - 2].toInt() and 0xFF
                val high = b[b.size - 1].toInt() and 0xFF
                return MessageResult.Result(ParameterValue.IntegerValue(toSigned16(low, high)))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setHoldPos(channel: Int, value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        val cmd = if (channel == 1) "$HLD1POS=" else "$HLD2POS="
        connection.send(cmd)
        connection.query(wordToBytes(value.value), -1).onSuccessAsync { return queryHoldPos(channel) }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryHoldoff(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$HOLDOFF?", -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setHoldoff(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$HOLDOFF=${value.value}$CRLF", -1).onSuccessAsync { return queryHoldoff() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryCursorCfg(): MessageResult<ParameterValue.BytesValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$CURCFG?", 6, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue) return MessageResult.Result(it)
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setCursorCfg(value: ParameterValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        val bytes = (value as? ParameterValue.BytesValue)?.value ?: byteArrayOf()
        connection.send("$CURCFG=")
        connection.query(bytes, -1).onSuccessAsync { return queryCursorCfg() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryCursorMode(): MessageResult<ParameterValue.BytesValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$CURMOD1?", 6, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue) return MessageResult.Result(it)
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setCursorMode(value: ParameterValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        val bytes = (value as? ParameterValue.BytesValue)?.value ?: byteArrayOf()
        connection.send("$CURMOD1=")
        connection.query(bytes, -1).onSuccessAsync { return queryCursorMode() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryRef1Pos(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$REFPRE1?", 5, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue && it.value.size >= 2) {
                val b = it.value
                val low = b[b.size - 2].toInt() and 0xFF
                val high = b[b.size - 1].toInt() and 0xFF
                return MessageResult.Result(ParameterValue.IntegerValue(toSigned16(low, high)))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setRef1Pos(value: ParameterValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.send("$REFPRE1=")
        val bytes = wordToBytes((value as? ParameterValue.IntegerValue)?.value ?: 0)
        connection.query(bytes, -1).onSuccessAsync { return queryRef1Pos() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryRef2Pos(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$REFPRE2?", 5, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue && it.value.size >= 2) {
                val b = it.value
                val low = b[b.size - 2].toInt() and 0xFF
                val high = b[b.size - 1].toInt() and 0xFF
                return MessageResult.Result(ParameterValue.IntegerValue(toSigned16(low, high)))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setRef2Pos(value: ParameterValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.send("$REFPRE2=")
        val bytes = wordToBytes((value as? ParameterValue.IntegerValue)?.value ?: 0)
        connection.query(bytes, -1).onSuccessAsync { return queryRef2Pos() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryRef1Data(): MessageResult<ParameterValue.BytesValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$RDREF1:", 2056, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue) return MessageResult.Result(it)
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setRef1Data(value: ParameterValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        val bytes = (value as? ParameterValue.BytesValue)?.value ?: byteArrayOf()
        connection.send("$SAVREF1:")
        connection.query(bytes, -1).onSuccessAsync { return queryRef1Data() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryRef2Data(): MessageResult<ParameterValue.BytesValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$RDREF2:", 2056, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue) return MessageResult.Result(it)
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setRef2Data(value: ParameterValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        val bytes = (value as? ParameterValue.BytesValue)?.value ?: byteArrayOf()
        connection.send("$SAVREF2:")
        connection.query(bytes, -1).onSuccessAsync { return queryRef2Data() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryDDF(): MessageResult<ParameterValue.BytesValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$DDF?", 10, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue) return MessageResult.Result(it)
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setDDF(value: ParameterValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        val bytes = (value as? ParameterValue.BytesValue)?.value ?: byteArrayOf()
        connection.send("$DDF=")
        connection.query(bytes, -1).onSuccessAsync { return queryDDF() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryDDF1(): MessageResult<ParameterValue.BytesValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$DDF1?", 10, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue) return MessageResult.Result(it)
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setDDF1(value: ParameterValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        val bytes = (value as? ParameterValue.BytesValue)?.value ?: byteArrayOf()
        connection.send("$DDF1=")
        connection.query(bytes, -1).onSuccessAsync { return queryDDF1() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryPsInt(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$PSINT?", -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setPsInt(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$PSINT=${value.value}$CRLF", -1).onSuccessAsync { return queryPsInt() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryPsCh1(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$PS1CH?", -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setPsCh1(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$PS1CH=${value.value}$CRLF", -1).onSuccessAsync { return queryPsCh1() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryPsCh2(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$PS2CH?", -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setPsCh2(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$PS2CH=${value.value}$CRLF", -1).onSuccessAsync { return queryPsCh2() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryPsY1Pos(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$PSY1POS?", 5, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue && it.value.size >= 2) {
                val b = it.value
                val low = b[b.size - 2].toInt() and 0xFF
                val high = b[b.size - 1].toInt() and 0xFF
                return MessageResult.Result(ParameterValue.IntegerValue(toSigned16(low, high)))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setPsY1Pos(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.send("$PSY1POS=")
        connection.query(wordToBytes(value.value), -1).onSuccessAsync { return queryPsY1Pos() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryPsY2Pos(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$PSY2POS?", 5, ReturnType.Bytes).onSuccessAsync {
            if (it is ParameterValue.BytesValue && it.value.size >= 2) {
                val b = it.value
                val low = b[b.size - 2].toInt() and 0xFF
                val high = b[b.size - 1].toInt() and 0xFF
                return MessageResult.Result(ParameterValue.IntegerValue(toSigned16(low, high)))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setPsY2Pos(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.send("$PSY2POS=")
        connection.query(wordToBytes(value.value), -1).onSuccessAsync { return queryPsY2Pos() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryPstb(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$PSTB?", -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setPstb(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$PSTB=${value.value}$CRLF", -1).onSuccessAsync { return queryPstb() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryLocalLock(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$LK?", -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setLocalLock(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$LK=${value.value}$CRLF", -1).onSuccessAsync { return queryLocalLock() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryQuickStart(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$QUICKST?", -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun setQuickStart(value: ParameterValue.IntegerValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$QUICKST=${value.value}$CRLF", -1).onSuccessAsync { return queryQuickStart() }
        return MessageResult.Error("Invalid response")
    }

    private suspend fun queryManualOperation(): MessageResult<ParameterValue.IntegerValue> {
        if (!connection.open()) return MessageResult.Error("Connection closed!")
        connection.query("$FCCMD?", -1, ReturnType.String).onSuccessAsync {
            if (it is ParameterValue.StringValue) {
                val last = it.value.trim().lastOrNull() ?: return MessageResult.Error("Invalid response")
                return MessageResult.Result(ParameterValue.IntegerValue(last.digitToInt()))
            }
        }
        return MessageResult.Error("Invalid response")
    }

    private fun wordToBytes(value: Int): ByteArray {
        val v = if (value < 0) 65536 + value else value
        val low = (v and 0xFF).toByte()
        val high = ((v shr 8) and 0xFF).toByte()
        return byteArrayOf(low, high)
    }

    private fun toSigned16(low: Int, high: Int): Int {
        val combined = (high shl 8) or low
        return if (combined and 0x8000 != 0) combined - 0x10000 else combined
    }
}
