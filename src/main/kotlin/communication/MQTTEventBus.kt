package com.labeq.communication

import com.labeq.config.MQTTConfig
import com.labeq.device.GenericDevice
import com.labeq.server.LabeqGateway
import com.labeq.transport.ParameterValue
import io.github.davidepianca98.mqtt.broker.Broker
import io.github.davidepianca98.mqtt.broker.interfaces.PacketInterceptor
import io.github.davidepianca98.mqtt.packets.MQTTPacket
import io.github.davidepianca98.mqtt.packets.Qos
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTConnect
import io.github.davidepianca98.mqtt.packets.mqtt.MQTTPublish
import io.github.davidepianca98.mqtt.packets.mqttv5.MQTT5Properties
import io.github.davidepianca98.toBase64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUnsignedTypes::class, ExperimentalUuidApi::class)
class MQTTEventBus(configuration: MQTTConfig, val gateway: LabeqGateway) {
    companion object {
        // Global topics
        val DEVICE_TOPIC = "device"

        // Device topics
        val DEVICE_PARAMETER_UPDATE_TOPIC = "$DEVICE_TOPIC/{uuid}/parameters/{parameter}/update"
        val DEVICE_PARAMETER_READ_TOPIC = "$DEVICE_TOPIC/{uuid}/parameters/{parameter}/read"
        val DEVICE_PARAMETER_WRITE_TOPIC = "$DEVICE_TOPIC/{uuid}/parameters/{parameter}/write"
        val DEVICE_PARAMETER_REQUEST_RESOLVE = "$DEVICE_TOPIC/{uuid}/requests/{request}/resolve"
    }

    private lateinit var thread: Thread
    private val log = LoggerFactory.getLogger("MQTTEventBus")
    private val broker: Broker = Broker(
        port = configuration.port.value,
        host = configuration.host.value,
        packetInterceptor = MQTTInterceptor(gateway)
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
                "uuid" to device.metadata.uuid.toString(),
                "parameter" to parameter
            )
        )

        val transformedValue = when (value) {
            is ParameterValue.IntegerValue -> value.value.toString()
            is ParameterValue.BytesValue -> value.value.toBase64()
            is ParameterValue.StringValue -> value.value
            is ParameterValue.DoubleValue -> value.value.toString()
            is ParameterValue.IntegerArrayValue -> value.value.joinToString(",", "[", "]")
            is ParameterValue.DoubleArrayValue -> value.value.joinToString(",", "[", "]")
        }

        log.debug("PUBLISH UPDATE: {}, {}", topic, transformedValue)

        publish(
            topic, buildJsonObject {
                put("newValue", Json.encodeToJsonElement(value))
            }.toString().toByteArray().toUByteArray(),
            true
        )
    }

    fun publishParameterResolve(requestId: Uuid, device: GenericDevice, parameter: String, value: ParameterValue) {
        if (parameter == "*") {
            device.parameters.keys.forEach {
                if (device.parameters.containsKey(it)) {
                    publishParameterUpdate(device, it, device.parameters[it]!!)
                }
            }
        }

        val topic = DEVICE_PARAMETER_REQUEST_RESOLVE.expandPlaceholders(
            mapOf(
                "uuid" to device.metadata.uuid.toString(),
                "request" to requestId.toString()
            )
        )

        val transformedValue = when (value) {
            is ParameterValue.IntegerValue -> value.value.toString()
            is ParameterValue.BytesValue -> value.value.toBase64()
            is ParameterValue.StringValue -> value.value
            is ParameterValue.DoubleValue -> value.value.toString()
            is ParameterValue.IntegerArrayValue -> value.value.joinToString(",", "[", "]")
            is ParameterValue.DoubleArrayValue -> value.value.joinToString(",", "[", "]")
        }

        log.debug("PUBLISH RESOLVE: {}, {}", topic, transformedValue)
        publish(
            topic, buildJsonObject {
                put("request", requestId.toString())
                put("parameter", parameter)
                put("resolvedValue", Json.encodeToJsonElement(value))
            }.toString().toByteArray().toUByteArray(),
            false
        )
    }

    fun publishResolveError(requestId: Uuid, device: GenericDevice, parameter: String, error: String) {
        val topic = DEVICE_PARAMETER_REQUEST_RESOLVE.expandPlaceholders(
            mapOf(
                "uuid" to device.metadata.uuid.toString(),
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
            false,
            "error"
        )
    }

    private fun String.expandPlaceholders(values: Map<String, String>): String {
        var result = this
        for ((key, value) in values) {
            result = result.replace("{$key}", value)
        }
        return result
    }

    private fun publish(topic: String, payload: UByteArray, retain: Boolean = false, contentType: String = "string") {
        broker.publish(
            retain = retain,
            topicName = topic,
            qos = Qos.AT_LEAST_ONCE,
            properties = MQTT5Properties(
                payloadFormatIndicator = 1u,
                contentType = "labeq/$contentType"
            ),
            payload
        )
    }

    private class MQTTInterceptor(val gateway: LabeqGateway) : PacketInterceptor {
        override fun packetReceived(
            clientId: String,
            username: String?,
            password: UByteArray?,
            packet: MQTTPacket
        ) {
            when (packet) {
                is MQTTPublish -> gateway.requestHandler.handlePublish(packet)
                is MQTTConnect -> println("${packet.clientID} ${packet.protocolName}")
            }
        }
    }
}
