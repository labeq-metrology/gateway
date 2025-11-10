package com.labeq.builtin.drivers.hameg.hm107

import com.labeq.transport.ParameterValue
import com.labeq.transport.MessageResult
import com.labeq.transport.ReturnType
import com.labeq.transport.getOrNull
import kotlin.math.abs

/**
 * Dekoder podstawy czasu (Timebase) dla HAMEG HM107.
 *
 * Wersja: bez użycia DDF – korzysta wyłącznie z komend TB<x>?
 *
 * Komendy:
 *  - TBA? / TBB? → zwraca bajt z zakresem i trybem
 *  - TBA=<code> / TBB=<code> → ustawia nową wartość
 */
class TimebaseDecoder(private val device: HM107Device) {

    /**
     * Tryby pracy oscyloskopu (zależne od bitów TBx)
     */
    enum class Mode {
        ANALOG, STORE, XY, ROLL
    }

    /**
     * Tryby wejść / synchronizacji Timebase (ANA / SING / B-TR / A-ALT / A-ADD)
     */
    enum class InputMode {
        ANA, SING, BTR, AALT, AADD
    }

    // Mapy czasów (s/div) wg dokumentacji HAMEG
    private val analogMap = mapOf(
        0x00 to 50e-9,  0x01 to 100e-9, 0x02 to 200e-9, 0x03 to 500e-9,
        0x04 to 1e-6,   0x05 to 2e-6,   0x06 to 5e-6,   0x07 to 10e-6,
        0x08 to 20e-6,  0x09 to 50e-6,  0x0A to 0.1e-3, 0x0B to 0.2e-3,
        0x0C to 0.5e-3, 0x0D to 1e-3,   0x0E to 2e-3,   0x0F to 5e-3,
        0x10 to 10e-3,  0x11 to 20e-3,  0x12 to 50e-3,  0x13 to 0.1,
        0x14 to 0.2,    0x15 to 0.5
    )

    private val storeMap = mapOf(
        0x03 to 500e-9, 0x04 to 1e-6,   0x05 to 2e-6,   0x06 to 5e-6,
        0x07 to 10e-6,  0x08 to 20e-6,  0x09 to 50e-6,  0x0A to 0.1e-3,
        0x0B to 0.2e-3, 0x0C to 0.5e-3, 0x0D to 1e-3,   0x0E to 2e-3,
        0x0F to 5e-3,   0x10 to 10e-3,  0x11 to 20e-3,  0x12 to 50e-3,
        0x13 to 0.1,    0x14 to 0.2,    0x15 to 0.5,    0x16 to 1.0,
        0x17 to 2.0,    0x18 to 5.0,    0x19 to 10.0,   0x1A to 20.0,
        0x1B to 50.0,   0x1C to 100.0
    )

    // ------------------------------------------------------------------
    // Główne API
    // ------------------------------------------------------------------

    /**
     * Zwraca aktualną wartość podstawy czasu (s/div) dla A lub B.
     */
    suspend fun getTimebaseSecondsPerDiv(id: String): Double? {
        val tb = queryTimebaseRaw(id) ?: return null
        val (code, mode) = decodeTbCode(tb)
        val map = when (mode) {
            Mode.STORE, Mode.ROLL -> storeMap
            Mode.XY -> storeMap
            Mode.ANALOG -> analogMap
        }
        return map[code]
    }

    /**
     * Ustawia podstawę czasu (A/B) w sekundach na działkę.
     */
    suspend fun setTimebaseSecondsPerDiv(id: String, secondsPerDiv: Double): Boolean {
        val tb = queryTimebaseRaw(id) ?: return false
        val (_, mode) = decodeTbCode(tb)
        val map = when (mode) {
            Mode.STORE, Mode.ROLL -> storeMap
            Mode.XY -> storeMap
            Mode.ANALOG -> analogMap
        }
        val code = map.minByOrNull { (_, v) -> abs(v - secondsPerDiv) }?.key ?: return false
        val result = device.driver.writeParameter(
            if (id.equals("A", true)) HM107Device.PARAMETER_TIMEBASE_A else HM107Device.PARAMETER_TIMEBASE_B,
            ParameterValue.BytesValue(byteArrayOf(code.toByte()))
        )
        return result is MessageResult.Result<*>
    }

    /**
     * Zwraca surowy bajt z komendy TBx?
     */
    private suspend fun queryTimebaseRaw(id: String): Int? {
        val result = if (id.equals("A", true))
            device.driver.queryParameter(HM107Device.PARAMETER_TIMEBASE_A, ReturnType.Bytes)
        else
            device.driver.queryParameter(HM107Device.PARAMETER_TIMEBASE_B, ReturnType.Bytes)

        val value = result.getOrNull() as? ParameterValue.BytesValue ?: return null
        return if (value.value.isNotEmpty()) value.value.last().toInt() and 0xFF else null
    }

    /**
     * Dekoduje bajt z komendy TBx? → (kod, tryb, wejście)
     */
    fun decodeTbCode(raw: Int): Pair<Int, Mode> {
        val code = raw and 0x1F   // D0–D4 zakres
        val mode = when {
            raw and 0x80 != 0 -> Mode.ROLL
            raw and 0x40 != 0 -> Mode.XY
            raw and 0x20 != 0 -> Mode.STORE
            else -> Mode.ANALOG
        }
        return code to mode
    }

    /**
     * Dekoduje typ wejścia / synchronizacji (ANA / SING / B-TR / A-ALT / A-ADD)
     * z bitów 5–7 TBx?.
     */
    fun decodeInputMode(raw: Int): InputMode = when {
        raw and 0x10 != 0 -> InputMode.SING
        raw and 0x08 != 0 -> InputMode.BTR
        raw and 0x04 != 0 -> InputMode.AALT
        raw and 0x02 != 0 -> InputMode.AADD
        else -> InputMode.ANA
    }

    /**
     * Pomocnicze: pełny opis TBx?
     */
    suspend fun getTimebaseInfo(id: String): String {
        val tbRaw = queryTimebaseRaw(id) ?: return "No data"
        val (code, mode) = decodeTbCode(tbRaw)
        val input = decodeInputMode(tbRaw)
        val time = getTimebaseSecondsPerDiv(id)
        return "TB$id: code=0x${code.toString(16).padStart(2, '0')}, ${time?.let { "${it * 1e3} ms/div" } ?: "?"}, Mode=$mode, Input=$input"
    }
}
