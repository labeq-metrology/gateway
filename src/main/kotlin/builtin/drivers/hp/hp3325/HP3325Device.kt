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
    override val parameters: Map<String, DeviceParameter<*>> = mapOf(
        "generator.frequency" to StringParameter("0HZ"),
        "generator.amplitude" to StringParameter(".001VO"),
    )

    override fun writeState(force: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    override fun readState(): Boolean {
        TODO("Not yet implemented")
    }

    override fun connect(): Boolean = driver.connect()
    override fun disconnect(): Boolean = driver.disconnect()

    @ParameterSetter("generator.frequency")
    suspend fun setFrequency(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter("generator.frequency", value)

    @ParameterGetter("generator.frequency")
    suspend fun getFrequency(): MessageResult<ParameterValue> = driver.queryParameter("generator.frequency")

    @ParameterSetter("generator.amplitude")
    suspend fun setAmplitude(value: ParameterValue): MessageResult<ParameterValue> =
        driver.writeParameter("generator.amplitude", value)

    @ParameterSetter("generator.amplitude")
    suspend fun getAmplitude(): MessageResult<ParameterValue> = driver.queryParameter("generator.amplitude")
}
