package com.labeq.config

import com.charleskorn.kaml.Yaml
import com.labeq.device.DeviceMetadata
import com.labeq.device.DeviceRegistry
import com.labeq.server.LabeqGateway
import com.labeq.transport.ConnectionRegistry
import io.ktor.util.logging.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.contentOrNull
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class DeviceConfigLoader(
    private val gateway: LabeqGateway,
    private val deviceRegistry: DeviceRegistry,
    private val connectionRegistry: ConnectionRegistry
) {
    private val log: Logger = LoggerFactory.getLogger("DeviceConfigLoader")

    fun loadDirectory(directory: Path): Int {
        Files.createDirectories(directory)
        var loaded = 0

        directory.toFile()
            .listFiles()
            ?.filter { it.isFile && it.extension.equals("yaml", ignoreCase = true) }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                try {
                    val text = file.readText(Charsets.UTF_8)
                    val config = Yaml.default.decodeFromString(DevicesConfiguration.serializer(), text)
                    if (config.schemaVersion != 1) {
                        throw IllegalArgumentException("Unsupported device config schema: ${config.schemaVersion}")
                    }
                    config.devices.forEachIndexed { index, deviceConfig ->
                        if (loadDevice(file, config, index, deviceConfig)) {
                            loaded++
                        }
                    }
                } catch (e: Exception) {
                    log.error("Failed loading device config ${file.absolutePath}: ${e.message}", e)
                }
            }

        return loaded
    }

    private fun loadDevice(
        file: File,
        fileConfig: DevicesConfiguration,
        deviceIndex: Int,
        config: DeviceInstanceConfiguration
    ): Boolean {
        if (!config.enabled) {
            log.info("Skipping disabled device '${config.name}'")
            return false
        }

        if (!deviceRegistry.hasRegistration(config.driver)) {
            val devicePath = inferDevicePath(config.driver)
            try {
                Class.forName(config.driver)
                Class.forName(devicePath)
            } catch (e: ClassNotFoundException) {
                throw IllegalArgumentException("Unknown device driver or implementation: ${config.driver}")
            }
            deviceRegistry.registerReflectively(config.driver, devicePath)
        }
        if (!connectionRegistry.hasRegistration(config.connection.implementation)) {
            try {
                Class.forName(config.connection.implementation)
            } catch (e: ClassNotFoundException) {
                throw IllegalArgumentException(
                    "Unknown connection implementation: ${config.connection.implementation}"
                )
            }
            connectionRegistry.registerReflectively(config.connection.implementation)
        }

        val uuid = if (config.uuid.isBlank()) {
            Uuid.random()
        } else {
            try {
                Uuid.parse(config.uuid)
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid device UUID '${config.uuid}'")
            }
        }

        if (gateway.devices.containsKey(uuid)) {
            throw IllegalArgumentException("Duplicate device UUID: $uuid")
        }

        val configurationClass = inferConnectionConfigurationPath(config.connection.implementation)
        val configuration = decodeConnectionConfiguration(
            config.connection.configuration,
            configurationClass
        )
        val connection = connectionRegistry.create(config.connection.implementation, configuration)
        val metadata = DeviceMetadata(
            uuid = uuid,
            name = config.name,
            model = config.model,
            manufacturer = config.manufacturer,
            driver = config.driver,
            tags = config.tags
        )
        val device = deviceRegistry.create(
            gateway.eventBus,
            config.driver,
            connection,
            metadata
        )

        gateway.registerDevice(device)

        if (config.uuid.isBlank()) {
            val updatedDevices = fileConfig.devices.toMutableList()
            updatedDevices[deviceIndex] = config.copy(uuid = uuid.toString())
            persistDevicesFile(
                file,
                DevicesConfiguration(fileConfig.schemaVersion, updatedDevices)
            )
            log.info("Assigned UUID $uuid to device '${config.name}'")
        }

        return true
    }

    private fun loadDevicesFile(file: File): DevicesConfiguration =
        Yaml.default.decodeFromString(
            DevicesConfiguration.serializer(),
            file.readText(Charsets.UTF_8)
        )

    private fun persistDevicesFile(file: File, config: DevicesConfiguration) {
        file.writeText(
            Yaml.default.encodeToString(DevicesConfiguration.serializer(), config),
            Charsets.UTF_8
        )
    }

    private fun inferDevicePath(driverPath: String): String {
        val separator = driverPath.lastIndexOf('.')
        val packageName = if (separator >= 0) driverPath.substring(0, separator) else ""
        val simpleName = if (separator >= 0) driverPath.substring(separator + 1) else driverPath
        val deviceName = if (simpleName.endsWith("Driver")) {
            simpleName.removeSuffix("Driver") + "Device"
        } else {
            simpleName + "Device"
        }
        return if (packageName.isEmpty()) deviceName else "$packageName.$deviceName"
    }

    private fun inferConnectionConfigurationPath(connectionPath: String): String {
        val separator = connectionPath.lastIndexOf('.')
        val packageName = if (separator >= 0) connectionPath.substring(0, separator) else ""
        val simpleName = if (separator >= 0) connectionPath.substring(separator + 1) else connectionPath
        val configurationName = if (simpleName.endsWith("Connection")) {
            simpleName.removeSuffix("Connection") + "Configuration"
        } else {
            simpleName + "Configuration"
        }
        return if (packageName.isEmpty()) configurationName else "$packageName.$configurationName"
    }

    private fun decodeConnectionConfiguration(
        json: JsonObject,
        configurationPath: String
    ): com.labeq.transport.ConnectionConfiguration {
        val kClass = try {
            Class.forName(configurationPath).kotlin
        } catch (e: ClassNotFoundException) {
            throw IllegalArgumentException(
                "Unknown connection configuration implementation: $configurationPath"
            )
        }
        val constructor = kClass.primaryConstructor
            ?: throw IllegalArgumentException(
                "Connection configuration has no primary constructor: $configurationPath"
            )

        val args = mutableMapOf<KParameter, Any?>()
        constructor.parameters.forEach { parameter ->
            val name = parameter.name ?: return@forEach
            val element = json[name]
            if (element != null) {
                args[parameter] = convertJsonValue(element, parameter)
            }
        }

        val configuration = constructor.callBy(args)
        return configuration as? com.labeq.transport.ConnectionConfiguration
            ?: throw IllegalArgumentException(
                "Connection configuration does not implement ConnectionConfiguration: $configurationPath"
            )
    }

    private fun convertJsonValue(element: Any, parameter: KParameter): Any? {
        val primitive = element as? JsonPrimitive
        val target = parameter.type.jvmErasure

        if (target.java.isEnum) {
            val text = primitive?.contentOrNull
                ?: throw IllegalArgumentException("Expected enum value for ${parameter.name}")
            val enumType = target.java
            val constants = enumType.enumConstants as Array<out Enum<*>>
            return constants.firstOrNull { it.name.equals(text, ignoreCase = true) }
                ?: throw IllegalArgumentException(
                    "Unknown ${target.simpleName} value '$text' for ${parameter.name}"
                )
        }

        return when (target) {
            String::class -> primitive?.contentOrNull
            Int::class -> primitive?.intOrNull
            Long::class -> primitive?.longOrNull
            Double::class -> primitive?.doubleOrNull
            Float::class -> primitive?.contentOrNull?.toFloatOrNull()
            Boolean::class -> primitive?.booleanOrNull
            ByteArray::class -> primitive?.contentOrNull?.toByteArray(Charsets.UTF_8)
            else -> throw IllegalArgumentException(
                "Unsupported connection configuration value '${parameter.name}' of type ${target.qualifiedName}"
            )
        }
    }
}
