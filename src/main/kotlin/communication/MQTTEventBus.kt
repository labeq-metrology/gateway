package com.labeq.communication

import com.labeq.config.MQTTConfig
import com.labeq.device.generic.GenericDevice
import com.labeq.transport.ParameterValue
import io.github.davidepianca98.mqtt.broker.Broker
import io.github.davidepianca98.mqtt.packets.Qos
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Properties
import io.github.davidepianca98.toBase64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUnsignedTypes::class, ExperimentalUuidApi::class)
class MQTTEventBus(configuration: MQTTConfig) {
    companion object {
        // Global topics
        val DEVICE_TOPIC = "device"

        // Device topics
        val DEVICE_PARAMETER_UPDATE_TOPIC = "$DEVICE_TOPIC/{uuid}/parameters/{parameter}/update"
        val DEVICE_PARAMETER_REQUEST_RESOLVE = "$DEVICE_TOPIC/{uuid}/requests/{request}/resolve"
    }

    private lateinit var thread: Thread;
    private val log = LoggerFactory.getLogger("MQTTEventBus")
    private val broker: Broker = Broker(
        port = configuration.port.value,
        host = configuration.host.value,
//        tlsSettings = TLSSettings("1.2", "", "", false)
    )

    fun listen() {
        thread = thread(name = "labeq-mqtt-broker", start = true) {
            log.info("MQTT Broker listening on ${broker.host}:${broker.port}")
            log.debug("MQTT Broker thread: ${Thread.currentThread().name}")
            broker.listen()
        }
    }

    fun publishParameterUpdate(device: GenericDevice, parameter: String, value: ParameterValue) {
        val topic = DEVICE_PARAMETER_UPDATE_TOPIC.expandPlaceholders(
            mapOf(
                "uuid" to device.uuid.toString(),
                "parameter" to parameter
            )
        )

        val transformedValue = when (value) {
            is ParameterValue.IntegerValue -> value.value.toString()
            is ParameterValue.BytesValue -> value.value.toBase64()
            is ParameterValue.StringValue -> value.value
        }

        log.debug("PUBLISH UPDATE: {}, {}", topic, transformedValue)

        publish(
            topic, buildJsonObject {
                put("newValue", transformedValue)
                put(
                    "contentType", when (value) {
                        is ParameterValue.IntegerValue -> "integer"
                        is ParameterValue.BytesValue -> "bytes"
                        is ParameterValue.StringValue -> "string"
                    }
                )
            }.toString().toByteArray().toUByteArray(),
            retain = true
        )
    }

    fun publishParameterResolve(requestId: Uuid, device: GenericDevice, parameter: String, value: ParameterValue) {
        val topic = DEVICE_PARAMETER_REQUEST_RESOLVE.expandPlaceholders(
            mapOf(
                "uuid" to device.uuid.toString(),
                "request" to requestId.toString()
            )
        )

        val transformedValue = when (value) {
            is ParameterValue.IntegerValue -> value.value.toString()
            is ParameterValue.BytesValue -> value.value.toBase64()
            is ParameterValue.StringValue -> value.value
        }

        log.debug("PUBLISH RESOLVE: {}, {}", topic, transformedValue)
        publish(
            topic, buildJsonObject {
                put("request", requestId.toString())
                put("parameter", parameter)
                put("resolvedValue", transformedValue)
                put(
                    "contentType", when (value) {
                        is ParameterValue.IntegerValue -> "integer"
                        is ParameterValue.BytesValue -> "bytes"
                        is ParameterValue.StringValue -> "string"
                    }
                )
            }.toString().toByteArray().toUByteArray(),
        )
    }

    fun publishResolveError(requestId: Uuid, device: GenericDevice, parameter: String, error: String) {
        val topic = DEVICE_PARAMETER_REQUEST_RESOLVE.expandPlaceholders(
            mapOf(
                "uuid" to device.uuid.toString(),
                "request" to requestId.toString()
            )
        )

        log.debug("PUBLISH RESOLVE ERROR: {}, {}", topic, error)

        publish(
            topic, buildJsonObject {
                put("request", requestId.toString())
                put("parameter", parameter)
                put("error", error)
            }.toString().toByteArray().toUByteArray(),
        )
    }

    private fun String.expandPlaceholders(values: Map<String, String>): String {
        var result = this
        for ((key, value) in values) {
            result = result.replace("{$key}", value)
        }
        return result
    }

    private fun publish(topic: String, payload: UByteArray, retain: Boolean = false) {
        broker.publish(
            retain = retain,
            topicName = topic,
            qos = Qos.AT_MOST_ONCE,
            properties = MQTT5Properties(),
            payload
        )
    }
}
