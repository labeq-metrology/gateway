package com.labeq.device.generic.oscilloscope

import com.labeq.communication.MQTTEventBus
import com.labeq.device.DeviceDriver
import com.labeq.device.generic.DeviceParameter
import com.labeq.device.generic.GenericDevice
import com.labeq.device.generic.IntegerParameter
import com.labeq.device.generic.ParameterGetter
import com.labeq.device.generic.ParameterSetter
import com.labeq.device.generic.StringParameter
import com.labeq.transport.MessageResult
import com.labeq.transport.ParameterValue
import com.labeq.transport.onFailure
import com.labeq.transport.onSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlin.reflect.full.memberProperties
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents a generic oscilloscope device, extending the functionality of a [GenericDevice].
 * This class provides methods and properties tailored for managing and interfacing with
 * oscilloscope-specific configurations, such as triggers.
 *
 * @constructor Creates a new instance of `GenericOscilloscope`.
 * @param eventBus The event bus used to communicate parameter updates and other events.
 * @param name The name of the oscilloscope device.
 * @param manufacturer The manufacturer of the oscilloscope.
 * @param model The model of the oscilloscope.
 * @param uuid A unique identifier for the oscilloscope.
 * @param driver The driver responsible for managing the device connection and communication.
 */
open class GenericOscilloscope @OptIn(ExperimentalUuidApi::class) constructor(
    eventBus: MQTTEventBus,
    name: String,
    manufacturer: String,
    model: String,
    uuid: Uuid,
    driver: DeviceDriver
) : GenericDevice(eventBus, name, manufacturer, model, uuid, driver) {
    var triggers = arrayListOf<OscilloscopeTrigger>()
    override val parameters: Map<String, DeviceParameter<*>> = mapOf(
        "triggers" to StringParameter(""),
        "scope.autoset" to IntegerParameter(0),
        "scope.acquisition.avg" to IntegerParameter(0)
    )

    override fun writeState(force: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    override fun readState(): Boolean {
        TODO("Not yet implemented")
    }

    /**
     * Establishes a connection to the oscilloscope device using the underlying driver.
     *
     * @return `true` if the connection was successfully established, `false` otherwise.
     */
    override fun connect(): Boolean = driver.connect()

    /**
     * Terminates the connection to the oscilloscope device using the underlying driver.
     *
     * @return `true` if the disconnection was successful, `false` otherwise.
     */
    override fun disconnect(): Boolean = driver.disconnect()

    /**
     * Sets the value of the "triggers" parameter and updates the associated trigger configurations.
     *
     * @param value A JSON-formatted string representing an array of `OscilloscopeTrigger` objects.
     *              This string is used to update both the internal `parameters` map and the `triggers` field.
     */
    @ParameterSetter("triggers")
    fun setTriggersParameter(value: String) {
        (parameters["triggers"] as StringParameter).value = value
        triggers = Json.decodeFromString<ArrayList<OscilloscopeTrigger>>(value)
    }

    /**
     * Retrieves the serialized JSON representation of the "triggers" parameter.
     *
     * @return A JSON-formatted string representing the current configuration of oscilloscope triggers.
     */
    @ParameterGetter("triggers")
    fun getTriggersParameter(): String {
        return Json.encodeToString(triggers)
    }

    @ParameterSetter("scope.autoset")
    suspend fun autoset(value: Int): Boolean {
        val result = driver.writeParameter("scope.autoset", ParameterValue.IntegerValue(1))
        return result is MessageResult.Result
    }

    @ParameterGetter("scope.acquisition.avg")
    suspend fun getAverageCount(): MessageResult<ParameterValue> {
        val result = driver.queryParameter("scope.aquisition.avg")
        println(result)
        return result
    }

    @ParameterSetter("scope.acquisition.avg")
    suspend fun setAverageCount(value: ParameterValue): MessageResult<ParameterValue> {
        val result = driver.writeParameter("scope.aquisition.avg", value)
        println(result)
        return result
    }
}
