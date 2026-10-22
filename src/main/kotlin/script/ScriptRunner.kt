package com.labeq.script

import com.labeq.script.configuration.PreparedExecution
import Heartbeat.WorkerStatus
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
data class ScriptRun(
    val executionId: Uuid,
    val script: Script,
    val preparedCommand: PreparedExecution,
    val log: List<String>,
    var status: String = WorkerStatus.STARTING.name,
    val heartbeatId: Int,
    val startedAt: Long = System.currentTimeMillis(),
    @Volatile var lastHeartbeatAt: Long? = null,
    val steps: CopyOnWriteArrayList<MeasurementStep> = CopyOnWriteArrayList()
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
            if (status == WorkerStatus.CRASHED || status == WorkerStatus.STOPPED) {
                scriptRun.markAbortedSteps("Script ended before this step completed")
            }
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

    fun beginMeasurementStep(stepId: String): MeasurementStep =
        scriptRun.beginStep(stepId)

    fun registerMeasurementStep(stepId: String, title: String, description: String): MeasurementStep {
        require(stepId.isNotBlank()) { "step_id must not be blank" }
        require(title.isNotBlank()) { "title must not be blank" }
        require(scriptRun.steps.none { it.id == stepId }) { "step '$stepId' already exists" }
        return MeasurementStep(stepId, title, description, status = MeasurementStepState.PENDING)
            .also { scriptRun.steps += it }
    }

    fun saveMeasurementResult(
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
    ) {
        val step = scriptRun.findStep(stepId) ?: error("step '$stepId' does not exist")
        check(step.status == MeasurementStepState.RUNNING) { "step '$stepId' is not running" }
        require(key.isNotBlank()) { "result key must not be blank" }
        step.results += MeasurementResult(
            key, value, unit, resultStatus, requiredValue, tolerance,
            condition, lowerBound, upperBound
        )
    }

    fun endMeasurementStep(stepId: String, stepStatus: MeasurementStepState, error: String) {
        val step = scriptRun.findStep(stepId) ?: error("step '$stepId' does not exist")
        check(step.status == MeasurementStepState.RUNNING) { "step '$stepId' is not running" }
        require(stepStatus == MeasurementStepState.PASSED ||
                stepStatus == MeasurementStepState.FAILED ||
                stepStatus == MeasurementStepState.SKIPPED) {
            "step status must be PASSED, FAILED, or SKIPPED"
        }
        step.status = stepStatus
        step.error = error.ifBlank { null }
        step.completedAt = System.currentTimeMillis()
    }

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