package com.labeq.script

import com.labeq.transport.ParameterValue
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class MeasurementStepState {
    PENDING,
    RUNNING,
    PASSED,
    FAILED,
    SKIPPED
}

data class MeasurementResult(
    val key: String,
    val value: ParameterValue,
    val unit: String,
    val status: String,
    val requiredValue: ParameterValue? = null,
    val tolerance: Double? = null,
    val condition: String? = null,
    val lowerBound: ParameterValue? = null,
    val upperBound: ParameterValue? = null,
    val recordedAt: Long = System.currentTimeMillis()
)

data class MeasurementStep(
    val id: String,
    val title: String,
    val description: String,
    val startedAt: Long = System.currentTimeMillis(),
    @Volatile var completedAt: Long? = null,
    @Volatile var status: MeasurementStepState = MeasurementStepState.RUNNING,
    @Volatile var error: String? = null,
    val results: CopyOnWriteArrayList<MeasurementResult> = CopyOnWriteArrayList()
)

@OptIn(ExperimentalUuidApi::class)
internal fun ScriptRun.findStep(stepId: String): MeasurementStep? =
    steps.firstOrNull { it.id == stepId }

@OptIn(ExperimentalUuidApi::class)
internal fun ScriptRun.beginStep(
    stepId: String,
): MeasurementStep {
    val step = findStep(stepId) ?: error("step '$stepId' does not exist")
    check(step.status == MeasurementStepState.PENDING) { "step '$stepId' is not pending" }
    step.status = MeasurementStepState.RUNNING
    return step
}

internal fun ScriptRun.markAbortedSteps(reason: String) {
    val completedAt = System.currentTimeMillis()
    steps.filter { it.status == MeasurementStepState.RUNNING || it.status == MeasurementStepState.PENDING }
        .forEach { step ->
        step.status = if (step.status == MeasurementStepState.RUNNING) {
            MeasurementStepState.FAILED
        } else {
            MeasurementStepState.SKIPPED
        }
        step.error = reason
        step.completedAt = completedAt
    }
}
