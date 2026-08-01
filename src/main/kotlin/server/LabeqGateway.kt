package com.labeq.server

import com.labeq.communication.MQTTEventBus
import com.labeq.config.LabeqGatewayConfiguration
import com.labeq.device.GenericDevice
import io.ktor.util.logging.*
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class LabeqGateway(configuration: LabeqGatewayConfiguration) {
    private val log: Logger = LoggerFactory.getLogger("LabeqGateway")

    var devices = mutableMapOf<Uuid, GenericDevice>()

    val eventBus = MQTTEventBus(configuration.mqttConfig, devices)

    init {
        log.info("Initializing Labeq Gateway")
        eventBus.listen()
    }

    fun registerDevice(device: GenericDevice): Boolean {
        log.info("Registering new device: ${device.metadata.model} by ${device.metadata.manufacturer}, ${device.metadata.name}@${device.metadata.uuid}")
        devices[device.metadata.uuid] = device
        device.connect()
        return true
    }

    fun registerDevices(vararg devices: GenericDevice) = devices.forEach { registerDevice(it) }

    fun unregisterDevice(device: GenericDevice): Boolean {
        log.info("Unregistering device: ${device.metadata.model} by ${device.metadata.manufacturer}, ${device.metadata.name}@${device.metadata.uuid}")
        devices.remove(device.metadata.uuid)
        device.disconnect()
        return true
    }

    fun unregisterDevices(vararg devices: GenericDevice) = devices.forEach { unregisterDevice(it) }
}