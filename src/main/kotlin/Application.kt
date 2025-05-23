package com.labeq

import com.charleskorn.kaml.Yaml
import com.labeq.builtin.connections.serial.SerialConfiguration
import com.labeq.builtin.connections.serial.SerialConnection
import com.labeq.builtin.connections.serial.SerialFlowControl
import com.labeq.builtin.connections.serial.SerialStopBits
import com.labeq.builtin.drivers.hameg.hm107.HM107Driver
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
import kotlin.concurrent.thread
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

    gateway.registerDevice(
        scope
    )

    thread {
        Thread.sleep(10000)
        for (i in 1..30) {
            gateway.requestHandler.add(
                scope.uuid,
                DeviceRequest.ParameterWriteRequest(
                    scope.uuid,
                    "scope.aquisition.avg",
                    ParameterValue.IntegerValue(i % 9)
                )
            )
//            gateway.requestHandler.add(
//                scope.uuid,
//                DeviceRequest.ParameterReadRequest(scope.uuid, "scope.aquisition.avg", ReturnType.INTEGER)
//            )
        }
    }

//    runBlocking {
//        log.debug("${scope.connect()}")
//        delay(30 * 1000)
//        log.debug("${scope.disconnect()}")
//    }
//    sleep(40000)
//    scope.setParameter("trigger.count", 25)
//    scope.setParameter("scope.test2", byteArrayOf(0x80.toByte(), 0x00.toByte()))
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
