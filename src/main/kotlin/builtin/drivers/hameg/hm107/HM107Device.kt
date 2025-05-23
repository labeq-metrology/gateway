package com.labeq.builtin.drivers.hameg.hm107

import com.labeq.communication.MQTTEventBus
import com.labeq.device.DeviceDriver
import com.labeq.device.generic.DeviceParameter
import com.labeq.device.generic.IntegerParameter
import com.labeq.device.generic.ParameterGetter
import com.labeq.device.generic.oscilloscope.GenericOscilloscope
import com.labeq.transport.MessageResult
import com.labeq.transport.ParameterValue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class HM107Device(
    eventBus: MQTTEventBus, name: String, manufacturer: String, model: String, uuid: Uuid,
    driver: DeviceDriver
) : GenericOscilloscope(
    eventBus, name,
    manufacturer,
    model,
    uuid, driver
) {
    override val parameters: Map<String, DeviceParameter<*>> = super.parameters.plus(
        mapOf(
            "scope.acquisition.switch" to IntegerParameter(0)
        )
    )

    @ParameterGetter("scope.acquisition.switch")
    suspend fun readAverageNumberOfAcquisitionsSwitch(): MessageResult<ParameterValue> {
        return driver.queryParameter("scope.acquisition.switch")
    }
}