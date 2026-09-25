package com.labeq.script

import com.labeq.script.configuration.RunnerConfiguration
import kotlinx.io.files.Path
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ScriptManager {
    private val log = LoggerFactory.getLogger(ScriptManager::class.java)

    private val configurations = ConcurrentHashMap<String, RunnerConfiguration>()
    private val activeScripts = ConcurrentHashMap<Uuid, ScriptRunner>()

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
        runtimeEnv: Map<String, String> = emptyMap()
    ): Uuid {
        val configuration = configurations[script.runner]
            ?: throw IllegalArgumentException("No runner configuration found for '${script.runner}'")

        val scriptPath = Path(script.file)
        val preparedCommand = configuration.prepareCommand(scriptPath, arguments, runtimeEnv)
        val executionId = Uuid.random()

        val scriptRunner = ScriptRunner(
            executionId = executionId,
            script = script,
            preparedCommand = preparedCommand,
            onComplete = { id -> removeActiveScript(id) }
        )

        activeScripts[executionId] = scriptRunner
        scriptRunner.start()

        log.info("Started script '${script.name}' with execution ID$executionId")
        return executionId
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

    private fun removeActiveScript(executionId: Uuid) {
        activeScripts.remove(executionId)
        log.debug("Execution $executionId removed from active registry.")
    }

    fun getActiveExecutionIds(): List<Uuid> {
        return activeScripts.keys().toList()
    }
}
