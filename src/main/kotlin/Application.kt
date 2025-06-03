package com.labeq

import com.charleskorn.kaml.Yaml
import com.labeq.builtin.connections.serial.*
import com.labeq.builtin.drivers.hameg.hm107.HM107Driver
import com.labeq.builtin.drivers.hp.hp3325.HP3325Device
import com.labeq.builtin.drivers.hp.hp3325.HP3325Driver
import com.labeq.communication.DeviceRequest
import com.labeq.config.*
import com.labeq.config.ServerConfig
import com.labeq.device.generic.oscilloscope.GenericOscilloscope
import com.labeq.transport.ParameterValue
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import io.ktor.server.application.*
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
    configureHTTP()
    configureSerialization()
    configureRouting()

    gateway = LabeqGateway(config)
    val scope = GenericOscilloscope(
        gateway.eventBus, "A", "B", "C", Uuid.random(), HM107Driver(
            SerialConnection(
                SerialConfiguration(
                    port = "COM2",
                    baudRate = 9600,
                    stopBits = SerialStopBits.TWO,
                    flowControl = SerialFlowControl.SOFTWARE
                )
            )
        )
    )

    val generator = HP3325Device(
        gateway.eventBus, "F Gen.", "HP", "3325B", Uuid.random(), HP3325Driver(
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
        )
    )

//    Thread.sleep(20000)
    gateway.registerDevices(generator, scope)
    for (i in 1..30) {
        gateway.requestHandler.add(
            generator.uuid,
            DeviceRequest.ParameterWriteRequest(
                generator.uuid,
                "generator.amplitude",
                ParameterValue.StringValue("${(i % 20)}VO")
            )
        )
        gateway.requestHandler.add(
            scope.uuid,
            DeviceRequest.ParameterWriteRequest(
                scope.uuid,
                "scope.autoset",
                ParameterValue.IntegerValue(i % 9)
            )
        )
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
