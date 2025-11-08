package com.labeq.builtin.drivers.hameg.hm107

import com.labeq.transport.ParameterValue
import com.labeq.transport.getOrNull

class WaveformDecoder(private val device: HM107Device) {

    suspend fun decodeVoltages(channel: Int): List<Double> {
        val preambleRv = device.queryWaveformPreamble().getOrNull() ?: return emptyList()
        val chSettingsRv = device.queryChannel1Settings().getOrNull().takeIf { channel == 1 } ?: device.queryChannel2Settings().getOrNull()
        val waveformRv = if (channel == 1) device.queryChannel1Waveform().getOrNull() else device.queryChannel2Waveform().getOrNull()

        if (preambleRv !is ParameterValue.BytesValue) return emptyList()
        if (chSettingsRv !is ParameterValue.BytesValue) return emptyList()
        if (waveformRv !is ParameterValue.BytesValue) return emptyList()

        val preamble = WaveformPreamble.fromBytes(preambleRv.value)
        if (chSettingsRv.value.isEmpty()) return emptyList()
        val chSettings = ChannelSettings.fromByte(chSettingsRv.value.first())

        val yPos = if (channel == 1) preamble.y1Position else preamble.y2Position
        val yRes = preamble.yResolution.toDouble()
        val vPerDiv = chSettings.voltPerDivMapped

        return waveformRv.value.map { b ->
            val raw = b.toInt() and 0xFF
            (raw - 128 - yPos) / yRes * vPerDiv
        }
    }

    suspend fun decodeTimeVoltagePairs(channel: Int): List<Pair<Double, Double>> {
        val voltages = decodeVoltages(channel)
        val preambleRv = device.queryWaveformPreamble().getOrNull() ?: return emptyList()
        if (preambleRv !is ParameterValue.BytesValue) return emptyList()
        val preamble = WaveformPreamble.fromBytes(preambleRv.value)
        val xScale = 1.0 / preamble.xResolution.toDouble()
        return voltages.mapIndexed { i, v -> Pair(i * xScale, v) }
    }
}
