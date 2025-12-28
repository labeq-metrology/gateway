package com.labeq.device.generic

import com.labeq.communication.MQTTEventBus
import com.labeq.device.DeviceDriver
import com.labeq.transport.MessageResult
import com.labeq.transport.ParameterValue
import com.labeq.transport.ReturnType
import com.labeq.transport.getOrNull
import io.ktor.util.logging.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.slf4j.LoggerFactory
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.typeOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
abstract class GenericDevice @OptIn(ExperimentalUuidApi::class) constructor(
    val eventBus: MQTTEventBus,
    val name: String,
    val manufacturer: String,
    var model: String,
    val uuid: Uuid,
    val driver: DeviceDriver
) {
    protected var logger: Logger = LoggerFactory.getLogger("$name@$uuid");

    /**
     * Abstract map of parameters associated with the device.
     *
     * Each parameter is identified by a unique string key and is represented by a `DeviceParameter` object.
     * These parameters contain the current state or configuration values of the device and may be of
     * various types such as integer, string, or binary, based on the specific implementation of
     * `DeviceParameter`.
     *
     * The `parameters` property can be accessed to retrieve or modify device settings or states.
     * Parameter interaction is generally done through helper functions or annotations (`ParameterSetter`
     * and `ParameterGetter`) to ensure proper validation and type consistency.
     */
    abstract val parameters: MutableMap<String, ParameterValue>

    private fun validateParameter(parameter: String): MessageResult<ParameterValue>? {
        return if (!parameters.containsKey(parameter)) {
            MessageResult.Error("Unknown parameter: $parameter")
        } else null
    }

    private fun notifyParameterUpdate(parameter: String, value: ParameterValue) {
        logger.info("$parameter set to $value")
        eventBus.publishParameterUpdate(this, parameter, value)
    }

    suspend fun setParameter(parameter: String, value: ParameterValue): MessageResult<ParameterValue> {
        validateParameter(parameter)?.let { return it }

        val function = this::class.memberFunctions
            .find { prop ->
                prop.annotations
                    .filterIsInstance<ParameterSetter>()
                    .any { it.identifier == parameter }
                    .and(
                        prop.parameters.size == 2 && prop.parameters[1].type.isSubtypeOf(typeOf<ParameterValue>())
                    )
            } ?: return MessageResult.Error("No setter function found for parameter: $parameter")

        return try {
            function.isAccessible = true
            when (val result = function.callSuspend(this, value) as MessageResult<*>) {
                is MessageResult.Error -> MessageResult.Error("Failed to set parameter $parameter: ${result.error}")
                is MessageResult.Result -> {
                    if (result.result is ParameterValue) {
                        notifyParameterUpdate(parameter, result.result)
                        parameters[parameter] = result.result
                    } else notifyParameterUpdate(parameter, value)
                    MessageResult.Result(value)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to set parameter $parameter", e)
            MessageResult.Error("Failed to set parameter $parameter: ${e.message}")
        }
    }

    /**
     * Read the value of a specified parameter
     * @param parameter the identifier of the parameter
     * @return the value of the parameter
     */
    suspend fun readParameter(parameter: String, returnType: ReturnType): MessageResult<ParameterValue> {
        validateParameter(parameter)?.let { return it }

        val function = this::class.memberFunctions
            .find { prop ->
                prop.annotations
                    .filterIsInstance<ParameterGetter>()
                    .any { it.identifier == parameter }
                    .and(prop.returnType.isSubtypeOf(typeOf<MessageResult<*>>()))
            } ?: return MessageResult.Error("No getter function found for parameter: $parameter")

        return try {
            function.isAccessible = true
            when (val result = function.callSuspend(this) as MessageResult<*>) {
                is MessageResult.Error -> MessageResult.Error("Failed to call parameter $parameter: ${result.error}")
                is MessageResult.Result -> {
                    val value = result.getOrNull() as ParameterValue
                    notifyParameterUpdate(parameter, value)
                    when (returnType) {
                        ReturnType.String, ReturnType.SameAsInput -> MessageResult.Result(value as ParameterValue.StringValue)
                        ReturnType.Bytes -> MessageResult.Result(value as ParameterValue.BytesValue)
                        ReturnType.Integer -> MessageResult.Result(value as ParameterValue.IntegerValue)
                        ReturnType.Double -> MessageResult.Result(value as ParameterValue.DoubleValue)
                        ReturnType.IntegerArray -> MessageResult.Result(value as ParameterValue.IntegerArrayValue)
                        ReturnType.DoubleArray -> MessageResult.Result(value as ParameterValue.DoubleArrayValue)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to read parameter $parameter", e)
            MessageResult.Error("Failed to read parameter $parameter: ${e.message}")
        }
    }


    /**
     * Write the state to the device
     * @param force the behavior varies per device, in general, allows the user to write invalid settings to the device
     * @return whether the operation succeeded or not
     */
    open suspend fun writeState(force: Boolean): Boolean {
        runCatching {
            parameters.forEach { (key, value) ->
                driver.writeParameter(key, value)
            }
        }.onFailure { return false }
        return true
    }

    /**
     * Read the state of the device and update stored parameters accordingly
     * @return whether the operation succeeded or not
     */
    open suspend fun readState(): Boolean {
        runCatching {
            parameters.keys.forEach { key ->
                driver.queryParameter(key).let { result ->
                    if (result is MessageResult.Result) {
                        parameters[key] = result.result
                    }
                }
            }
        }.onFailure { return false }
        return true
    }

    /**
     * Establishes a connection to the device. The exact implementation of the connection logic
     * is determined by the specific subclass of the device.
     *
     * @return `true` if the connection was successfully established, `false` otherwise.
     */
    abstract fun connect(): Boolean

    /**
     * Terminates the connection to the device. The exact implementation of the disconnection logic
     * is determined by the specific subclass of the device.
     *
     * @return `true` if the disconnection was successful, `false` otherwise.
     */
    abstract fun disconnect(): Boolean
};