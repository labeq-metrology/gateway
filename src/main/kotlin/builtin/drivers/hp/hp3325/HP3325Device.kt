package com.labeq.builtin.drivers.hp.hp3325

import com.labeq.communication.MQTTEventBus
import com.labeq.device.DeviceDriver
import com.labeq.device.generic.*
import com.labeq.transport.MessageResult
import com.labeq.transport.ParameterValue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class HP3325Device(
    eventBus: MQTTEventBus, name: String, manufacturer: String, model: String, uuid: Uuid,
    driver: DeviceDriver,
) : GenericDevice(eventBus, name, manufacturer, model, uuid, driver) {
    companion object {
        const val PARAMETER_GENERATOR_FREQUENCY = "generator.frequency"
        const val PARAMETER_GENERATOR_AMPLITUDE = "generator.amplitude"
        const val PARAMETER_GENERATOR_FUNCTION = "generator.function"
    }

    override val parameters: MutableMap<String, ParameterValue> = mutableMapOf(
        PARAMETER_GENERATOR_FREQUENCY to ParameterValue.StringValue("0HZ"),
        PARAMETER_GENERATOR_AMPLITUDE to ParameterValue.StringValue(".001VO"),
        PARAMETER_GENERATOR_FUNCTION to ParameterValue.IntegerValue(0)
    )

    override fun writeState(force: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    override fun readState(): Boolean {
        TODO("Not yet implemented")
    }

    override fun connect(): Boolean = driver.connect()
    override fun disconnect(): Boolean = driver.disconnect()

    @ParameterSetter(PARAMETER_GENERATOR_FREQUENCY)
    suspend fun setFrequency(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_GENERATOR_FREQUENCY, value)

    @ParameterGetter(PARAMETER_GENERATOR_FREQUENCY)
    suspend fun getFrequency(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_GENERATOR_FREQUENCY)

    @ParameterSetter(PARAMETER_GENERATOR_AMPLITUDE)
    suspend fun setAmplitude(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_GENERATOR_AMPLITUDE, value)

    @ParameterGetter(PARAMETER_GENERATOR_AMPLITUDE)
    suspend fun getAmplitude(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_GENERATOR_AMPLITUDE)

    @ParameterSetter(PARAMETER_GENERATOR_FUNCTION)
    suspend fun setFunction(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter(PARAMETER_GENERATOR_FUNCTION, value)

    @ParameterGetter(PARAMETER_GENERATOR_FUNCTION)
    suspend fun getFunction(): MessageResult<ParameterValue> = driver.queryParameter(PARAMETER_GENERATOR_FUNCTION)
}
