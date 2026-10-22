package com.labeq.script.rpc.service

import io.grpc.Status
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal fun requireNonBlank(value: String, field: String): String =
    value.trim().ifEmpty {
        throw Status.INVALID_ARGUMENT
            .withDescription("$field must not be blank")
            .asException()
    }

@OptIn(ExperimentalUuidApi::class)
internal fun parseUuid(value: String, field: String): Uuid =
    try {
        Uuid.parse(requireNonBlank(value, field))
    } catch (_: IllegalArgumentException) {
        throw Status.INVALID_ARGUMENT
            .withDescription("$field must be a valid UUID")
            .asException()
    }

internal fun mapDomainFailure(error: RuntimeException): Nothing =
    when (error) {
        is IllegalArgumentException -> throw Status.INVALID_ARGUMENT
            .withDescription(error.message ?: "Invalid request")
            .asException()
        is IllegalStateException -> throw Status.FAILED_PRECONDITION
            .withDescription(error.message ?: "Invalid state transition")
            .asException()
        else -> throw error
    }

internal inline fun <T, R> respond(
    action: () -> T,
    response: (T) -> R
): R =
    try {
        response(action())
    } catch (error: RuntimeException) {
        mapDomainFailure(error)
    }

internal inline fun <R> respond(action: () -> R): R =
    try {
        action()
    } catch (error: RuntimeException) {
        mapDomainFailure(error)
    }
