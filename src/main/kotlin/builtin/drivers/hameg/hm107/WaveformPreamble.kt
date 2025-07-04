package com.labeq.builtin.drivers.hameg.hm107

data class WaveformPreamble(
    val memoryAddress: Int,
    val xResolution: Int = 200,
    val yResolution: Int = 25,
    val y1Position: Int,
    val y2Position: Int,
) {
    companion object {
        fun fromBytes(bytes: ByteArray): WaveformPreamble {
            if (bytes.size != 10) throw IllegalArgumentException("Bytes length must be 10")

            fun getWord(offset: Int): Int {
                return ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
            }

            val memoryAddress = getWord(0)     // WORD 1
            val xResolution = getWord(2)       // WORD 2 (200)
            val yResolution = getWord(4)       // WORD 3 (25)
            val y1Position = getWord(6)        // WORD 4
            val y2Position = getWord(8)        // WORD 5

            return WaveformPreamble(
                memoryAddress,
                xResolution,
                yResolution,
                y1Position,
                y2Position
            )
        }
    }
}
