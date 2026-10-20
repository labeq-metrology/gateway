package com.labeq.script

import com.labeq.script.configuration.PreparedExecution
import Heartbeat.WorkerStatus
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ScriptRunner(
    val executionId: Uuid,
    val script: Script,
    val heartbeatId: Int,
    private val preparedCommand: PreparedExecution,
    private val onStatusChange: (ScriptRunner, WorkerStatus) -> Unit,
    private val onComplete: (Uuid) -> Unit
) : Thread("ScriptRunner-$executionId") {

    private val log = LoggerFactory.getLogger("ScriptRunner-${script.name}-$executionId")

    @Volatile
    var status: WorkerStatus = WorkerStatus.STARTING
        private set(value) {
            field = value
            onStatusChange(this, value)
        }

    @Volatile
    var lastHeartbeatAt: Long? = null
        private set

    @Volatile
    private var process: Process? = null

    override fun run() {
        try {
            status = WorkerStatus.INITIALIZING
            val command = preparedCommand.command
            val pb = ProcessBuilder(command)
            pb.directory(File(preparedCommand.workingDirectory))
            pb.environment().putAll(preparedCommand.environment)
            pb.redirectErrorStream(true)

            log.info("Starting script '${script.name}': ${command.joinToString(" ")}")

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
            status = if (exitCode == 0) WorkerStatus.FINISHED else WorkerStatus.CRASHED

        } catch (e: InterruptedException) {
            log.warn("Script '${script.name}' was interrupted. Terminating process...")
            status = WorkerStatus.STOPPED
            stopGracefully()
        } catch (e: Exception) {
            log.error("Failed to execute script '${script.name}'", e)
            status = WorkerStatus.CRASHED
        } finally {
            onComplete(executionId)
        }
    }
    fun stopScript() {
        status = WorkerStatus.STALLED
        interrupt()
        stopGracefully()
    }

    fun handleHeartbeat(status: WorkerStatus) {
        this.status = status
        lastHeartbeatAt = System.currentTimeMillis()
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
        status = WorkerStatus.STOPPED
    }
}