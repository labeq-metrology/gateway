package com.labeq.server

import com.charleskorn.kaml.Yaml
import com.labeq.builtin.connections.serial.*
import com.labeq.builtin.drivers.hameg.hm107.HM107Device
import com.labeq.builtin.drivers.hp.hp3325.HP3325Device
import com.labeq.builtin.drivers.hp.hp3325.HP3325Driver
import com.labeq.config.*
import com.labeq.config.ServerConfig
import com.labeq.device.DeviceMetadata
import com.labeq.script.configuration.RunnerConfiguration
import com.labeq.script.configuration.RunnerConfigurationDefinition
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import io.ktor.server.application.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths
import kotlin.system.exitProcess
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

val CONFIG_DIR_PATH = "${Paths.get("config").toAbsolutePath()}"
val RUNNER_CONFIG_DIR_PATH = "${Paths.get("config/runners").toAbsolutePath()}"

private lateinit var gateway: LabeqGateway

// default config
private var config: LabeqGatewayConfiguration = LabeqGatewayConfiguration(
    server = ServerConfig(
        Hostname("127.0.0.1"), Port(80)
    ), mqtt = MQTTConfig(
        Hostname("127.0.0.1"), Port(1883), false, FilePath(""), FilePath("")
    ), rpc = RPCConfig(Port(7777))
)

fun main(args: Array<String>) {
    EngineMain.main(args)
}

@OptIn(ExperimentalUuidApi::class)
fun Application.module() {
    ensureConfigurationFiles()
    gateway = LabeqGateway(config)
    ensureRunnerConfigurationFiles()
    gateway.scriptManager.discoverScripts()

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
            Uuid.parse("ce690329-0c10-478e-a15a-70c061b4f8c2"),
            "F. Generator",
            "HP 3325B",
            "Hewlett Packard",
            "com.labeq.builtin.drivers.hp.hp3325.HP3325Driver",
            listOf("generator")
        )
    )

    val scope = HM107Device(
        gateway.eventBus, HP3325Driver(
            SerialConnection(
                SerialConfiguration(
                    port = "COM2",
                    baudRate = 115200,
                    dataBits = 7,
                    stopBits = SerialStopBits.ONE,
                    flowControl = SerialFlowControl.SOFTWARE,
                    parity = SerialParity.EVEN
                )
            )
        ),
        DeviceMetadata(
            Uuid.random(),
            "Oscilloscope",
            "HM1507-3",
            "HAMEG",
            "com.labeq.builtin.drivers.hameg.hm107.HM107Driver",
            listOf("scope")
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

    try {
        config = ConfigLoaderBuilder
            .default()
            .withExplicitSealedTypes()
            .addFileSource(configFile, allowEmpty = true, optional = false)
            .build()
            .loadConfigOrThrow<LabeqGatewayConfiguration>()
    } catch (e: Exception) {
        println(e.message)
        exitProcess(1)
    }
}

@OptIn(ExperimentalHoplite::class)
fun Application.ensureRunnerConfigurationFiles() {
    val runnerConfigDir = File(RUNNER_CONFIG_DIR_PATH)
    if (!runnerConfigDir.exists()) {
        log.info("Runner config directory does not exist: $RUNNER_CONFIG_DIR_PATH, creating it...")
        runnerConfigDir.mkdirs()
    }

    runnerConfigDir.listFiles()?.forEach {
        if (!it.isFile || !it.name.endsWith(".yaml"))
            return@forEach
        try {
            log.info("Runner config file found: $it")
            val runner = ConfigLoaderBuilder
                .default()
                .withExplicitSealedTypes()
                .addFileSource(it, allowEmpty = true, optional = false)
                .build()
                .loadConfigOrThrow<RunnerConfigurationDefinition>()
            gateway.scriptManager.configurations[runner.name] = RunnerConfiguration(runner)
            log.info("Loaded runner config: ${runner.name}")
        } catch (e: Exception) {
            log.error("Failed loading runner config file: $it (${e.message})")
            log.error(e.stackTraceToString())
        }
    }
}
