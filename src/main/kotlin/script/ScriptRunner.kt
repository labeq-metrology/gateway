package com.labeq.script

import com.labeq.script.configuration.PreparedExecution
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ScriptRunner(
    val executionId: Uuid,
    val script: Script,
    private val preparedCommand: PreparedExecution,
    private val onComplete: (Uuid) -> Unit
) : Thread("ScriptRunner-$executionId") {

    private val log = LoggerFactory.getLogger("ScriptRunner-${script.name}-$executionId")

    @Volatile
    private var process: Process? = null

    override fun run() {
        try {
            val pb = ProcessBuilder(preparedCommand.command)
            pb.directory(File(preparedCommand.workingDirectory))
            pb.environment().putAll(preparedCommand.environment)
            pb.redirectErrorStream(true)

            log.info("Starting script '${script.name}': ${preparedCommand.command.joinToString(" ")}")

            val p = pb.start()
            process = p

            // Consume output stream to prevent buffer lockups
            p.inputStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    log.info(line)
                }
            }

            val exitCode = p.waitFor()
            log.info("Script '${script.name}' finished with exit code $exitCode")

        } catch (e: InterruptedException) {
            log.warn("Script '${script.name}' was interrupted. Terminating process...")
            stopGracefully()
        } catch (e: Exception) {
            log.error("Failed to execute script '${script.name}'", e)
        } finally {
            onComplete(executionId)
        }
    }

    fun stopScript() {
        interrupt()
        stopGracefully()
    }

    private fun stopGracefully() {
        val currentProcess = process ?: return
        if (currentProcess.isAlive) {
            currentProcess.destroy() // SIGTERM
            try {
                if (!currentProcess.waitFor(3, TimeUnit.SECONDS)) {
                    log.warn("Script '${script.name}' did not stop gracefully. Forcing kill...")
                    currentProcess.destroyForcibly() // SIGKILL
                }
            } catch (e: InterruptedException) {
                currentProcess.destroyForcibly()
            }
        }
    }
}