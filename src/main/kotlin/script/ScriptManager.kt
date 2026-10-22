package com.labeq.script

import Heartbeat.WorkerStatus
import com.labeq.communication.MQTTEventBus
import com.labeq.script.configuration.RunnerConfiguration
import kotlinx.io.IOException
import kotlinx.io.files.Path
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ScriptManager(private val eventBus: MQTTEventBus) {
    private val log = LoggerFactory.getLogger(ScriptManager::class.java)

    val configurations = ConcurrentHashMap<String, RunnerConfiguration>()
    val availableScripts = ConcurrentHashMap<String, Script>()
    val activeScripts = ConcurrentHashMap<Uuid, ScriptRunner>()
    val runs = ConcurrentHashMap<Uuid, ScriptRun>()
    private val nextHeartbeatId = AtomicInteger(0)

    /**
     * Registers a configuration that scripts can reference via their `runner` property.
     */
    fun registerConfiguration(configuration: RunnerConfiguration) {
        val name = configuration.definition.name
        configurations[name] = configuration
        log.info("Registered runner configuration: '$name'")
    }

    /**
     * Prepares and starts a Script execution.
     *
     * @return The unique execution ID generated for this run.
     */
    fun startScript(
        script: Script,
        arguments: List<String> = emptyList(),
        runtimeEnv: Map<String, String> = emptyMap(),
        heartbeatId: Int = 0
    ): Uuid {
        val configuration = configurations[script.runner]
            ?: throw IllegalArgumentException("No runner configuration found for '${script.runner}'")

        val executionId = Uuid.random()
        val effectiveHeartbeatId = if (heartbeatId == 0) {
            nextHeartbeatId.getAndUpdate { if (it == Int.MAX_VALUE) 1 else it + 1 }
        } else {
            heartbeatId
        }
        val scriptPath = Path(script.file)
        val preparedCommand = configuration.prepareCommand(
            scriptPath,
            listOf(effectiveHeartbeatId.toString()) + arguments,
            runtimeEnv
        )

        val scriptRunner = ScriptRunner(
            executionId = executionId,
            script = script,
            heartbeatId = effectiveHeartbeatId,
            preparedCommand = preparedCommand,
            onStatusChange = { runner, status ->
                eventBus.publishScriptStatus(runner.executionId, runner.script.name, status.name)
            },
            onComplete = { id, run ->
                removeActiveScript(id)
                runs[id] = run
            }
        )

        activeScripts[executionId] = scriptRunner
        scriptRunner.start()
        eventBus.publishScriptStatus(executionId, script.name, scriptRunner.status.name)

        log.info("Started script '${script.name}' with execution ID $executionId")
        return executionId
    }

    fun startScript(
        scriptId: String,
        arguments: List<String> = emptyList(),
        runtimeEnv: Map<String, String> = emptyMap(),
        heartbeatId: Int = 0
    ): Uuid {
        val script = availableScripts[scriptId]
            ?: throw NoSuchElementException("Script '$scriptId' not found")
        return startScript(script, arguments, runtimeEnv, heartbeatId)
    }

    fun execution(executionId: Uuid): ScriptRunner? = activeScripts[executionId]

    fun executionByHeartbeat(workerId: Int): ScriptRunner? =
        activeScripts.values.firstOrNull { it.heartbeatId == workerId }

    fun beginMeasurementStep(
        workerId: Int,
        stepId: String,
    ): Boolean {
        val runner = executionByHeartbeat(workerId) ?: return false
        runner.beginMeasurementStep(stepId)
        return true
    }

    fun registerMeasurementStep(workerId: Int, stepId: String, title: String, description: String): Boolean {
        val runner = executionByHeartbeat(workerId) ?: return false
        runner.registerMeasurementStep(stepId, title, description)
        return true
    }

    fun saveMeasurementResult(
        workerId: Int,
        stepId: String,
        key: String,
        value: com.labeq.transport.ParameterValue,
        unit: String,
        resultStatus: String,
        requiredValue: com.labeq.transport.ParameterValue?,
        tolerance: Double?,
        condition: String?,
        lowerBound: com.labeq.transport.ParameterValue?,
        upperBound: com.labeq.transport.ParameterValue?
    ): Boolean {
        val runner = executionByHeartbeat(workerId) ?: return false
        runner.saveMeasurementResult(
            stepId, key, value, unit, resultStatus, requiredValue, tolerance,
            condition, lowerBound, upperBound
        )
        return true
    }

    fun endMeasurementStep(
        workerId: Int,
        stepId: String,
        status: MeasurementStepState,
        error: String
    ): Boolean {
        val runner = executionByHeartbeat(workerId) ?: return false
        runner.endMeasurementStep(stepId, status, error)
        return true
    }

    fun handleHeartbeat(workerId: Int, status: WorkerStatus): Boolean {
        val runner = executionByHeartbeat(workerId)

        runner ?: return false
        runner.handleHeartbeat(status)
        eventBus.publishScriptHeartbeat(
            runner.executionId,
            runner.script.name,
            runner.heartbeatId,
            status.name
        )
        return true
    }

    fun stopScript(executionId: Uuid) {
        val runner = activeScripts[executionId]
        if (runner != null) {
            log.info("Stopping script execution $executionId...")
            runner.stopScript()
        } else {
            log.warn("Cannot stop execution $executionId: Not found or already finished.")
        }
    }

    fun stopAll() {
        log.info("Stopping all active scripts...")
        activeScripts.keys().toList().forEach { stopScript(it) }
    }

    fun discoverScripts() {
        val scriptsDir = File("scripts")
        if (!scriptsDir.exists()) {
            try {
                scriptsDir.mkdirs()
            } catch (e: IOException) {
                log.error("Could not create scripts directory: ${e.message}")
                return
            }
        }

        scriptsDir.walkTopDown()
            .maxDepth(2)
            .filter { it.isFile }
            .forEach { file ->
                if (availableScripts.containsKey(file.absolutePath)) return@forEach

                val possibleRunner = configurations.values.firstOrNull { it.supportsExtension(file.extension) }
                val script = Script(
                    file.nameWithoutExtension,
                    "",
                    file.absolutePath,
                    possibleRunner?.definition?.name ?: ""
                )
                availableScripts[file.nameWithoutExtension] = script
                log.info("Script {} discovered. Associated runner: {}", script.name, possibleRunner?.definition?.name)
            }
    }

    private fun removeActiveScript(executionId: Uuid) {
        activeScripts.remove(executionId)
        log.debug("Execution {} removed from active registry.", executionId)
    }
}
