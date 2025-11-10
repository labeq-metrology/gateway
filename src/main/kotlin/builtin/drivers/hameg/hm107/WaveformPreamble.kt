package com.labeq.builtin.drivers.hameg.hm107

data class WaveformPreamble(
    val memoryAddress: Int,
    val xResolution: Int = 200,
    val yResolution: Int = 25,
    val y1Position: Int,
    val y2Position: Int
) {
    companion object {
        fun fromBytes(bytes: ByteArray): WaveformPreamble {
            require(bytes.size == 10) { "Bytes length must be 10 for WFMPRE" }
            fun word(off: Int) = ((bytes[off + 1].toInt() and 0xFF) shl 8) or (bytes[off].toInt() and 0xFF)
            val memoryAddress = word(0)
            val xRes = word(2)
            val yRes = word(4)
            val y1 = word(6)
            val y2 = word(8)
            return WaveformPreamble(memoryAddress, xRes, yRes, y1, y2)
        }

        fun toBytes(p: WaveformPreamble): ByteArray {
            fun putWord(v: Int) = byteArrayOf(((v shr 8) and 0xFF).toByte(), (v and 0xFF).toByte())
            return putWord(p.memoryAddress) + putWord(p.xResolution) + putWord(p.yResolution) + putWord(p.y1Position) + putWord(p.y2Position)
        }
    }
}
