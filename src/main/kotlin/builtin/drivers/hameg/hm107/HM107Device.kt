package com.labeq.builtin.drivers.hameg.hm107

import com.labeq.communication.MQTTEventBus
import com.labeq.device.DeviceDriver
import com.labeq.device.generic.ParameterGetter
import com.labeq.device.generic.ParameterSetter
import com.labeq.device.generic.oscilloscope.GenericOscilloscope
import com.labeq.transport.MessageResult
import com.labeq.transport.ParameterValue
import com.labeq.transport.getOrNull
import kotlinx.coroutines.channels.Channel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class HM107Device(
    eventBus: MQTTEventBus, name: String, manufacturer: String, model: String, uuid: Uuid, driver: DeviceDriver
) : GenericOscilloscope(
    eventBus, name, manufacturer, model, uuid, driver
) {
    companion object {
        // Scope parameters
        const val PARAMETER_AVG_NUMBER_OF_ACQUISITIONS = "scope.acquisition.avg"
        const val PARAMETER_AVG_NUMBER_OF_ACQUISITIONS_SWITCH = "scope.acquisitions.switch"
        const val PARAMETER_WAVEFORM_PREAMBLE = "scope.acquisition.preamble"

        // Channels
        const val PARAMETER_CHANNEL_1_SETTINGS = "scope.channel.1.settings"
        const val PARAMETER_CHANNEL_1_WAVEFORM = "scope.channel.1.waveform"
        const val PARAMETER_CHANNEL_1_WAVEFORM_VOLTAGE = "scope.channel.1.waveform.voltage"

        const val PARAMETER_CHANNEL_2_SETTINGS = "scope.channel.2.settings"
        const val PARAMETER_CHANNEL_2_WAVEFORM = "scope.channel.2.waveform"
        const val PARAMETER_CHANNEL_2_WAVEFORM_VOLTAGE = "scope.channel.2.waveform.voltage"
    }

    override val parameters: MutableMap<String, ParameterValue> = super.parameters.apply {
        putAll(
            arrayOf(
                PARAMETER_AVG_NUMBER_OF_ACQUISITIONS to ParameterValue.IntegerValue(0),
                PARAMETER_AVG_NUMBER_OF_ACQUISITIONS_SWITCH to ParameterValue.IntegerValue(0),
                PARAMETER_WAVEFORM_PREAMBLE to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_CHANNEL_1_SETTINGS to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_CHANNEL_2_SETTINGS to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_CHANNEL_1_WAVEFORM to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_CHANNEL_2_WAVEFORM to ParameterValue.BytesValue(byteArrayOf()),
                PARAMETER_CHANNEL_1_WAVEFORM_VOLTAGE to ParameterValue.DoubleArrayValue(doubleArrayOf()),
                PARAMETER_CHANNEL_2_WAVEFORM_VOLTAGE to ParameterValue.DoubleArrayValue(doubleArrayOf())
            )
        )
    }

    @ParameterGetter(PARAMETER_AVG_NUMBER_OF_ACQUISITIONS)
    suspend fun queryAverageNumberOfAcquisitions(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_AVG_NUMBER_OF_ACQUISITIONS)

    @ParameterSetter(PARAMETER_AVG_NUMBER_OF_ACQUISITIONS)
    suspend fun setAverageNumberOfAcquisitions(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_AVG_NUMBER_OF_ACQUISITIONS, value)

    @ParameterGetter(PARAMETER_AVG_NUMBER_OF_ACQUISITIONS_SWITCH)
    suspend fun queryAverageNumberOfAcquisitionsSwitch(): MessageResult<ParameterValue> =
        driver.queryParameter(PARAMETER_AVG_NUMBER_OF_ACQUISITIONS_SWITCH)

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
    suspend fun queryChannel1Voltages(): MessageResult<ParameterValue> {
        val voltages = queryChannelVoltages(1)
        return if (voltages.isEmpty()) MessageResult.Error("No waveform data")
        else MessageResult.Result(ParameterValue.DoubleArrayValue(voltages.toDoubleArray()))
    }

    @ParameterGetter(PARAMETER_CHANNEL_2_WAVEFORM_VOLTAGE)
    suspend fun queryChannel2Voltages(): MessageResult<ParameterValue> {
        val voltages = queryChannelVoltages(2)
        return if (voltages.isEmpty()) MessageResult.Error("No waveform data")
        else MessageResult.Result(ParameterValue.DoubleArrayValue(voltages.toDoubleArray()))
    }

    suspend fun queryChannelVoltages(channel: Int): List<Double> {
        if (channel != 1 && channel != 2) return emptyList()
        val preamble = queryWaveformPreamble().getOrNull()
        if (preamble == null) return emptyList()

        val channelSettings =
            if (channel == 1) queryChannel1Settings().getOrNull() else queryChannel2Settings().getOrNull()
        if (channelSettings == null) return emptyList()

        val waveform =
            if (channel == 1) queryChannel1Waveform().getOrNull() else queryChannel2Waveform().getOrNull()
        if (waveform == null) return emptyList()

        if (channelSettings !is ParameterValue.BytesValue) return emptyList()
        if (channelSettings.value.size != 1) return emptyList()
        val mappedSettings = ChannelSettings.fromByte(channelSettings.value.first())

        if (preamble !is ParameterValue.BytesValue) return emptyList()
        if (preamble.value.size != 10) return emptyList()
        val mappedPreamble = WaveformPreamble.fromBytes(preamble.value)

        val yPos = if (channel == 1) mappedPreamble.y1Position else mappedPreamble.y2Position

        if (waveform !is ParameterValue.BytesValue) return emptyList()
        return convertToVolts(waveform.value, yPos, mappedSettings.voltPerDivMapped)
    }

    private fun convertToVolts(
        signal: ByteArray, y1Pos: Int, voltsPerDiv: Double
    ) = signal.map { byteN ->
        // Convert Byte to unsigned int (0..255)
        val dataAsInt = byteN.toInt() and 0xFF
        // Apply the formula: UN = (ByteN - 128 - Y1Pos) / 25 * V/Div
        val voltage = (dataAsInt - 128 - y1Pos) / 25.0 * voltsPerDiv
        voltage
    }
}
