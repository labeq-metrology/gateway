package com.labeq.script

import com.labeq.script.configuration.PreparedExecution
import Heartbeat.WorkerStatus
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class ScriptRun(
    val executionId: Uuid,
    val script: Script,
    val preparedCommand: PreparedExecution,
    val log: List<String>,
    var status: String = WorkerStatus.STARTING.name,
    val heartbeatId: Int,
    val startedAt: Long = System.currentTimeMillis(),
    @Volatile var lastHeartbeatAt: Long? = null
)

@OptIn(ExperimentalUuidApi::class)
class ScriptRunner(
    val executionId: Uuid,
    val script: Script,
    val heartbeatId: Int,
    val startedAt: Long = System.currentTimeMillis(),
    private val preparedCommand: PreparedExecution,
    private val onStatusChange: (ScriptRunner, WorkerStatus) -> Unit,
    private val onComplete: (Uuid, ScriptRun) -> Unit
) : Thread("ScriptRunner-$executionId") {

    private val loggerName = "ScriptRunner-${script.name}-$executionId"
    private val log = LoggerFactory.getLogger(loggerName)
    private val scriptLog = CopyOnWriteArrayList<String>()
    private val scriptRun = ScriptRun(
        executionId,
        script,
        preparedCommand,
        scriptLog,
        heartbeatId = heartbeatId,
        startedAt = startedAt
    )
    private val logTimestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    @Volatile
    var status: WorkerStatus = WorkerStatus.STARTING
        private set(value) {
            field = value
            scriptRun.status = value.name
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

            recordInfo("Starting script '${script.name}': ${command.joinToString(" ")}")

            val p = pb.start()
            process = p

            // Consume output stream to prevent buffer lockups
            p.inputStream.bufferedReader().use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    recordInfo(line)
                }
            }

            val exitCode = p.waitFor()
            recordInfo("Script '${script.name}' finished with exit code $exitCode")
            status = if (exitCode == 0) WorkerStatus.FINISHED else WorkerStatus.CRASHED

        } catch (e: InterruptedException) {
            recordWarn("Script '${script.name}' was interrupted. Terminating process...")
            status = WorkerStatus.STOPPED
            stopGracefully()
        } catch (e: Exception) {
            recordError("Failed to execute script '${script.name}': ${e.message}", e)
            status = WorkerStatus.CRASHED
        } finally {
            onComplete(executionId, scriptRun)
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
        scriptRun.lastHeartbeatAt = lastHeartbeatAt
    }

    fun snapshot(): ScriptRun = scriptRun

    private fun stopGracefully() {
        val currentProcess = process ?: return
        if (currentProcess.isAlive) {
            currentProcess.destroy() // SIGTERM
            try {
                if (!currentProcess.waitFor(3, TimeUnit.SECONDS)) {
                    recordWarn("Script '${script.name}' did not stop gracefully. Forcing kill...")
                    currentProcess.destroyForcibly() // SIGKILL
                }
            } catch (e: InterruptedException) {
                currentProcess.destroyForcibly()
            }
        }
        status = WorkerStatus.STOPPED
    }

    private fun recordInfo(message: String) {
        record("INFO", message)
        log.info(message)
    }

    private fun recordWarn(message: String) {
        record("WARN", message)
        log.warn(message)
    }

    private fun recordError(message: String, throwable: Throwable) {
        record("ERROR", message, throwable)
        log.error(message, throwable)
    }

    private fun record(level: String, message: String, throwable: Throwable? = null) {
        val timestamp = LocalDateTime.now(ZoneId.systemDefault()).format(logTimestampFormatter)
        val formattedLevel = level.padEnd(5)
        val formatted = buildString {
            append(timestamp)
            append(" ")
            append(formattedLevel)
            append("- ")
            append(message)
            append(System.lineSeparator())
            if (throwable != null) {
                append("[TRACE]")
                append(StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString())
                append("[END TRACE]")
            }
        }
        scriptLog += formatted
    }
}