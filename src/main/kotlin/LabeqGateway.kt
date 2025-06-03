package com.labeq

import com.labeq.communication.MQTTEventBus
import com.labeq.communication.RequestHandler
import com.labeq.config.LabeqGatewayConfiguration
import com.labeq.device.generic.GenericDevice
import io.ktor.util.logging.Logger
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class LabeqGateway(configuration: LabeqGatewayConfiguration) {
    private val log: Logger = LoggerFactory.getLogger("LabeqGateway")

    var devices = mutableMapOf<Uuid, GenericDevice>()

    val eventBus = MQTTEventBus(configuration.mqttConfig)
    val requestHandler = RequestHandler(devices, eventBus)

    init {
        log.info("Initializing Labeq Gateway")
        eventBus.listen()
    }

    fun registerDevice(device: GenericDevice): Boolean {
        log.info("Registering new device: ${device.model} by ${device.manufacturer}, ${device.name}@${device.uuid}")
        devices.put(device.uuid, device)
        device.connect()
        return true
    }

    fun registerDevices(vararg devices: GenericDevice) = devices.forEach { registerDevice(it) }

    fun unregisterDevice(device: GenericDevice): Boolean {
        log.info("Unregistering device: ${device.model} by ${device.manufacturer}, ${device.name}@${device.uuid}")
        devices.remove(device.uuid)
        device.disconnect()
        return true
    }

    fun unregisterDevices(vararg devices: GenericDevice) = devices.forEach { unregisterDevice(it) }
}