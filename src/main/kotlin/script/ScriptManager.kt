package com.labeq.script

import com.labeq.script.configuration.RunnerConfiguration
import kotlinx.io.IOException
import kotlinx.io.files.Path
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ScriptManager {
    private val log = LoggerFactory.getLogger(ScriptManager::class.java)

    val configurations = ConcurrentHashMap<String, RunnerConfiguration>()
    val availableScripts = HashMap<String, Script>()
    val activeScripts = ConcurrentHashMap<Uuid, ScriptRunner>()

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
