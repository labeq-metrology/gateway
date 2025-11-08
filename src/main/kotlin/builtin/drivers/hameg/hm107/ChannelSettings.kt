package com.labeq.builtin.drivers.hameg.hm107

data class ChannelSettings(
    val gnd: Boolean,
    val ac: Boolean,
    val inv: Boolean,
    val on: Boolean,
    val voltPerDivCode: Int,
    val voltPerDivMapped: Double
) {
    companion object {
        private val map = mapOf(
            0 to 0.001, 1 to 0.002, 2 to 0.005, 3 to 0.01, 4 to 0.02,
            5 to 0.05, 6 to 0.1, 7 to 0.2, 8 to 0.5, 9 to 1.0,
            10 to 2.0, 11 to 5.0, 12 to 10.0, 13 to 20.0
        )

        fun fromByte(byte: Byte): ChannelSettings {
            val b = byte.toInt() and 0xFF
            val gnd = (b and 0x80) != 0
            val ac = (b and 0x40) != 0
            val inv = (b and 0x20) != 0
            val on = (b and 0x10) != 0
            val code = b and 0x0F
            val mapped = map[code] ?: Double.NaN
            return ChannelSettings(gnd, ac, inv, on, code, mapped)
        }

        fun toByte(settings: ChannelSettings): Byte {
            var v = settings.voltPerDivCode and 0x0F
            if (settings.on) v = v or 0x10
            if (settings.inv) v = v or 0x20
            if (settings.ac) v = v or 0x40
            if (settings.gnd) v = v or 0x80
            return v.toByte()
        }

        fun codeFromVolt(volt: Double): Int =
            map.entries.minByOrNull { (_, v) -> kotlin.math.abs(v - volt) }?.key ?: 0
    }
}
