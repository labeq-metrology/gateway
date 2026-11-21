package com.labeq.device

import com.labeq.communication.MQTTEventBus
import com.labeq.transport.ConnectionConfiguration
import com.labeq.transport.DeviceConnection

fun interface DeviceFactory {
    fun create(eventBus: MQTTEventBus, driver: DeviceDriver, metadata: DeviceMetadata): GenericDevice
}

fun interface DeviceDriverFactory {
    fun create(connection: DeviceConnection): DeviceDriver
}

class DeviceRegistry {
    private val registrations =
        mutableMapOf<String, Pair<DeviceDriverFactory, DeviceFactory>>()
    private val reflectiveRegistrations =
        mutableMapOf<String, String>()

    fun register(
        driverPath: String,
        driverFactory: DeviceDriverFactory,
        deviceFactory: DeviceFactory
    ) {
        require(driverPath.isNotBlank()) { "Driver class path cannot be blank" }
        require(registrations.put(driverPath, driverFactory to deviceFactory) == null) {
            "Device driver already registered: $driverPath"
        }
    }

    fun hasRegistration(driverPath: String): Boolean =
        registrations.containsKey(driverPath) || reflectiveRegistrations.containsKey(driverPath)

    fun registerReflectively(driverPath: String, devicePath: String) {
        require(driverPath.isNotBlank()) { "Driver class path cannot be blank" }
        require(devicePath.isNotBlank()) { "Device class path cannot be blank" }
        reflectiveRegistrations[driverPath] = devicePath
    }

    fun create(
        eventBus: MQTTEventBus,
        driverPath: String,
        connection: DeviceConnection,
        metadata: DeviceMetadata
    ): GenericDevice {
        reflectiveRegistrations[driverPath]?.let { devicePath ->
            val driverClass = Class.forName(driverPath)
            val driver = driverClass.constructors
                .firstOrNull { it.parameterCount == 1 && it.parameterTypes[0].isInstance(connection) }
                ?.newInstance(connection) as? DeviceDriver
                ?: throw IllegalArgumentException("Unable to construct device driver reflectively: $driverPath")

            val deviceClass = Class.forName(devicePath)
            val deviceConstructor = deviceClass.constructors.firstOrNull { constructor ->
                constructor.parameterCount == 3 &&
                    MQTTEventBus::class.java.isAssignableFrom(constructor.parameterTypes[0]) &&
                    DeviceDriver::class.java.isAssignableFrom(constructor.parameterTypes[1]) &&
                    DeviceMetadata::class.java.isAssignableFrom(constructor.parameterTypes[2])
            } ?: throw IllegalArgumentException("Unable to construct device reflectively: $devicePath")

            return deviceConstructor.newInstance(eventBus, driver, metadata) as GenericDevice
        }

        val registration = registrations[driverPath]
            ?: throw IllegalArgumentException("Unknown device driver: $driverPath")
        val driver = registration.first.create(connection)
        return registration.second.create(eventBus, driver, metadata)
    }
}
