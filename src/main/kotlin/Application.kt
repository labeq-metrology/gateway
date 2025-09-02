package com.labeq

import com.charleskorn.kaml.Yaml
import com.labeq.builtin.connections.serial.*
import com.labeq.builtin.drivers.hameg.hm107.HM107Device
import com.labeq.builtin.drivers.hameg.hm107.HM107Driver
import com.labeq.builtin.drivers.hp.hp3325.HP3325Device
import com.labeq.builtin.drivers.hp.hp3325.HP3325Driver
import com.labeq.communication.DeviceRequest
import com.labeq.config.*
import com.labeq.config.ServerConfig
import com.labeq.transport.ParameterValue
import com.labeq.transport.ReturnType
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

    val scope = HM107Device(
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
        gateway.eventBus, "3325B #1", "Hewlett Packard", "3325B", Uuid.random(), HP3325Driver(
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

    val generator2 = HP3325Device(
        gateway.eventBus, "3325B #2", "Hewlett Packard", "3325B", Uuid.random(), HP3325Driver(
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

    gateway.registerDevices(scope, generator, generator2)

    configureHTTP()
    configureSerialization()
    configureRouting(gateway)

    runBlocking {
        gateway.eventBus.requestHandler.add(
            generator.uuid,
            DeviceRequest.ParameterWriteRequest(
                Uuid.random(), HP3325Device.PARAMETER_GENERATOR_FREQUENCY,
                ParameterValue.StringValue("1KH")
            )
        )
        gateway.eventBus.requestHandler.add(
            generator.uuid,
            DeviceRequest.ParameterWriteRequest(
                Uuid.random(), HP3325Device.PARAMETER_GENERATOR_AMPLITUDE,
                ParameterValue.StringValue("5VO")
            )
        )
        Thread.sleep(5000)
        gateway.eventBus.requestHandler.add(
            scope.uuid,
            DeviceRequest.ParameterReadRequest(Uuid.random(), HM107Device.PARAMETER_CHANNEL_1_WAVEFORM_VOLTAGE,
                ReturnType.DoubleArray)
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
