package com.labeq.script.rpc.service

import Measurement
import MeasurementServiceGrpcKt
import com.labeq.script.MeasurementStepState
import com.labeq.script.ScriptManager
import io.grpc.Status

class MeasurementService(private val scriptManager: ScriptManager) :
    MeasurementServiceGrpcKt.MeasurementServiceCoroutineImplBase() {

    override suspend fun registerStep(request: Measurement.RegisterStepRequest): Measurement.MeasurementResponse =
        respond(
            action = {
                requireNonBlank(request.stepId, "step_id")
                requireNonBlank(request.title, "title")
                scriptManager.registerMeasurementStep(
                    request.workerId,
                    request.stepId,
                    request.title,
                    request.description
                )
            },
            response = Boolean::measurementResponse
        )

    override suspend fun beginStep(request: Measurement.BeginStepRequest): Measurement.MeasurementResponse =
        respond(
            action = {
                requireNonBlank(request.stepId, "step_id")
                scriptManager.beginMeasurementStep(
                    request.workerId,
                    request.stepId
                )
            },
            response = Boolean::measurementResponse
        )

    override suspend fun saveResult(request: Measurement.SaveResultRequest): Measurement.MeasurementResponse =
        respond(
            action = {
                requireNonBlank(request.stepId, "step_id")
                requireNonBlank(request.key, "key")
                scriptManager.saveMeasurementResult(
                    request.workerId,
                    request.stepId,
                    request.key,
                    request.value.toModel(),
                    request.unit,
                    request.status.name,
                    request.requiredValue.takeIf { request.hasRequiredValue() }?.toModel(),
                    request.tolerance.takeIf { request.hasTolerance },
                    request.condition.takeIf { it.isNotBlank() },
                    request.lowerBound.takeIf { request.hasLowerBound }?.toModel(),
                    request.upperBound.takeIf { request.hasUpperBound }?.toModel()
                )
            },
            response = Boolean::measurementResponse
        )

    override suspend fun endStep(request: Measurement.EndStepRequest): Measurement.MeasurementResponse =
        respond(
            action = {
                requireNonBlank(request.stepId, "step_id")
                scriptManager.endMeasurementStep(
                    request.workerId,
                    request.stepId,
                    request.status.toModel(),
                    request.error
                )
            },
            response = Boolean::measurementResponse
        )
}

private fun Measurement.MeasurementStepStatus.toModel(): MeasurementStepState =
    when (this) {
        Measurement.MeasurementStepStatus.MEASUREMENT_STEP_PASSED -> MeasurementStepState.PASSED
        Measurement.MeasurementStepStatus.MEASUREMENT_STEP_FAILED -> MeasurementStepState.FAILED
        Measurement.MeasurementStepStatus.MEASUREMENT_STEP_SKIPPED -> MeasurementStepState.SKIPPED
        else -> throw Status.INVALID_ARGUMENT
            .withDescription("step status must be PASSED, FAILED, or SKIPPED")
            .asException()
    }

private fun Boolean.measurementResponse(): Measurement.MeasurementResponse =
    Measurement.MeasurementResponse.newBuilder()
        .setSuccess(this)
        .setMessage(if (this) "" else "No active script found for worker ID")
        .build()
