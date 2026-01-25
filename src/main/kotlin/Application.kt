package com.labeq

import com.charleskorn.kaml.Yaml
import com.labeq.builtin.connections.serial.SerialConfiguration
import com.labeq.builtin.connections.serial.SerialConnection
import com.labeq.builtin.connections.serial.SerialFlowControl
import com.labeq.builtin.connections.serial.SerialParity
import com.labeq.builtin.connections.serial.SerialStopBits
import com.labeq.builtin.drivers.hp.hp3325.HP3325Device
import com.labeq.builtin.drivers.hp.hp3325.HP3325Driver
import com.labeq.config.*
import com.labeq.config.ServerConfig
import com.labeq.device.DeviceMetadata
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

val CONFIG_DIR_PATH = "${Paths.get("config").toAbsolutePath()}"

private lateinit var gateway: LabeqGateway

// default config
private var config: LabeqGatewayConfiguration = LabeqGatewayConfiguration(
    server = ServerConfig(
        Hostname("127.0.0.1"), Port(80)
    ), mqttConfig = MQTTConfig(
        Hostname("127.0.0.1"), Port(1883), false, FilePath(""), FilePath("")
    )
)


fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

@OptIn(ExperimentalUuidApi::class)
fun Application.module() {
    ensureConfigurationFiles()

    gateway = LabeqGateway(config)

    val generator = HP3325Device(
        gateway.eventBus, HP3325Driver(
            SerialConnection(
                SerialConfiguration(
                    port = "COM3",
                    baudRate = 300,
                    dataBits = 7,
                    stopBits = SerialStopBits.ONE,
                    flowControl = SerialFlowControl.SOFTWARE,
                    parity = SerialParity.EVEN
                )
            )
        ),
        DeviceMetadata(
            Uuid.random(),
            "F. Generator",
            "HP 3325B",
            "Hewlett Packard",
            "com.labeq.builtin.drivers.hp.hp3325.HP3325Driver",
            listOf("generator")
        )
    )

    gateway.registerDevices(generator)

    configureHTTP()
    configureSerialization()
    configureRouting(gateway)

    runBlocking {
//        gateway.eventBus.requestHandler.add(
//            generator.uuid,
//            DeviceRequest.ParameterReadRequest(
//                Uuid.random(), "*"
//            )
//        )
    }
}

@OptIn(ExperimentalHoplite::class)
fun Application.ensureConfigurationFiles() {
    val configDir = File(CONFIG_DIR_PATH)
    if (!configDir.exists()) {
        log.info("Config file does not exist: $CONFIG_DIR_PATH, creating it...")
        configDir.mkdirs()
    }

    val configFile = Paths.get(CONFIG_DIR_PATH, "config.yaml").toFile()
    if (!configFile.exists()) {
        log.info("Config file does not exist: $CONFIG_DIR_PATH, creating it...")
        configFile.createNewFile()

        configFile.writeText(
            Yaml.default.encodeToString(LabeqGatewayConfiguration.serializer(), config), Charsets.UTF_8
        )
    }

    config = ConfigLoaderBuilder
        .default()
        .withExplicitSealedTypes()
        .addFileSource(configFile, allowEmpty = true, optional = false)
        .build()
        .loadConfigOrThrow<LabeqGatewayConfiguration>()
}
