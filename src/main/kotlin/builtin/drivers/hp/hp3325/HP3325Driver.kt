package com.labeq.builtin.drivers.hp.hp3325

import com.labeq.device.DeviceDriver
import com.labeq.transport.*
import kotlinx.coroutines.runBlocking

class HP3325Driver(deviceConnection: DeviceConnection) : DeviceDriver(deviceConnection) {
    private companion object {
        const val REMOTE_ENABLE = "RMT"
        const val LOCAL_ENABLE = "LCL"
        const val RESET_DEVICE = "RST"

        const val AMPLITUDE = "AM"
        const val FREQUENCY = "FR"

        const val DISPLAY_STRING = "DSP"

        const val INTERROGATE = "?"
    }

    override suspend fun queryParameter(
        parameter: String, returnType: ReturnType
    ): MessageResult<ParameterValue> {
        return when (parameter) {
            "generator.frequency" -> readFrequency()
            "generator.amplitude" -> readAmplitude()
            else -> MessageResult.Error("Invalid parameter: $parameter")
        }
    }

    override suspend fun writeParameter(
        parameter: String, value: ParameterValue, returnType: ReturnType
    ): MessageResult<ParameterValue> {
        return when (parameter) {
            "generator.frequency" -> setFrequency(value as ParameterValue.StringValue)
            "generator.amplitude" -> setAmplitude(value as ParameterValue.StringValue)
            else -> MessageResult.Error("Invalid parameter: $parameter")
        }
    }

    override suspend fun writeData(
        data: ByteArray, returnType: ReturnType
    ): MessageResult<ParameterValue> {
        TODO("Not yet implemented")
    }

    override suspend fun writeData(
        data: String, returnType: ReturnType
    ): MessageResult<ParameterValue> {
        TODO("Not yet implemented")
    }

    override fun connect(): Boolean {
        return runBlocking {
            if (!connection.open()) return@runBlocking false
            connection.send(REMOTE_ENABLE)
            connection.send("DISP1")
            connection.send(RESET_DEVICE)
            connection.query(AMPLITUDE + INTERROGATE, -1).onSuccess {
                connection.send("$DISPLAY_STRING'GATEWAY CTL'")
                return@runBlocking true
            }
            false
        }
    }

    override fun disconnect(): Boolean {
        return runBlocking {
            if (!connection.open()) return@runBlocking false
            connection.send("$DISPLAY_STRING'GATEWAY dSC'")
            connection.send(LOCAL_ENABLE)
            connection.close()
        }
    }

    // TODO: Use [ParameterValue] and check for different cases
    suspend fun setFrequency(frequency: ParameterValue.StringValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        val frequencyRegex = Regex.fromLiteral("[0-9]+(HZ|KH|MH)")
        if (!frequencyRegex.matches(frequency.value)) return MessageResult.Error("Invalid frequency format: ${frequency.value}")
        val frequencyValue = frequency.value.dropLast(2).toDouble()
        if (frequencyValue < 0 || frequencyValue > 60999999.999) return MessageResult.Error("Invalid frequency: ${frequency.value}")
        connection.send(FREQUENCY + frequency.value)
        return readFrequency()
    }

    suspend fun readFrequency(): MessageResult<ParameterValue.StringValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(FREQUENCY + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            return MessageResult.Result(
                ParameterValue.StringValue(it.value.removePrefix("FR"))
            )
        }
        return MessageResult.Error("Unknown error")
    }

    suspend fun setAmplitude(amplitude: ParameterValue.StringValue): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        if (!("[0-9]+VO|VR|DB".toRegex() matches amplitude.value)) return MessageResult.Error("Invalid amplitude format: ${amplitude.value}")
        val amplitudeValue = amplitude.value.dropLast(2).toDouble()
        if (amplitudeValue < 0 || amplitudeValue > 40) return MessageResult.Error("Invalid amplitude: ${amplitude.value}")
        connection.send(AMPLITUDE + amplitude.value)
        return readAmplitude()
    }

    suspend fun readAmplitude(): MessageResult<ParameterValue> {
        if (!connection.open()) return MessageResult.Error("Connection is not open")
        connection.query(AMPLITUDE + INTERROGATE, -1).onSuccess {
            if (it !is ParameterValue.StringValue) return MessageResult.Error("Invalid result $it")
            return MessageResult.Result(
                ParameterValue.StringValue(it.value.removePrefix("AM"))
            )
        }
        return MessageResult.Error("Unknown error")
    }

}
