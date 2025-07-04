package com.labeq.builtin.drivers.hameg.hm107

data class ChannelSettings(
    val gnd: Boolean,
    val ac: Boolean,
    val inv: Boolean,
    val on: Boolean,
    val voltPerDiv: Int,
    val voltPerDivMapped: Double
) {
    companion object {
        fun fromByte(byte: Byte): ChannelSettings {
            val b = byte.toInt()

            val gnd = (b shr 7 and 1) != 0
            val ac = (b shr 6 and 1) != 0
            val inv = (b shr 5 and 1) != 0
            val on = (b shr 4 and 1) != 0
            val voltPerDiv = b and 0b00001111

            val voltPerDivMap = mapOf(
                0 to 0.001,
                1 to 0.002,
                2 to 0.005,
                3 to 0.01,
                4 to 0.02,
                5 to 0.05,
                6 to 0.1,
                7 to 0.2,
                8 to 0.5,
                9 to 1.0,
                10 to 2.0,
                11 to 5.0,
                12 to 10.0,
                13 to 20.0
            )

            val voltPerDivMapped = voltPerDivMap[voltPerDiv] ?: -1.0

            return ChannelSettings(
                gnd,
                ac,
                inv,
                on,
                voltPerDiv,
                voltPerDivMapped
            )
        }
    }
}

