package com.labeq.server

import com.labeq.device.GenericDevice
import com.labeq.script.Script
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@kotlinx.serialization.Serializable
private data class StartScriptRequest(
    val arguments: List<String> = emptyList(),
    val environment: Map<String, String> = emptyMap(),
    val heartbeatId: Int = 0
)

@kotlinx.serialization.Serializable
private data class ScriptExecutionResponse(
    val executionId: String,
    val script: Script,
    val status: String,
    val heartbeatId: Int,
    val lastHeartbeatAt: Long?,
    val startedAt: Long,
    val log: List<String>
)

@OptIn(ExperimentalUuidApi::class)
fun Application.configureRouting(gateway: LabeqGateway) {
    install(AutoHeadResponse)
    routing {
        get("/runners") {
            call.respond(
                gateway.scriptManager.configurations.values.map { it.definition }
            )
        }

        get("/devices/{deviceId}") {
            try {
                val deviceId = Uuid.parse(call.parameters["deviceId"]!!)
                val device: GenericDevice? = gateway.devices[deviceId]

                val json = buildJsonObject {
                    device?.metadata?.let {
                        Json.encodeToJsonElement(it).jsonObject.forEach { (key, value) -> put(key, value) }
                    }
                    device?.parameters?.let { put("parameters", Json.encodeToJsonElement(it)) }
                }

                call.respondText { json.toString() }
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                call.respond(HttpStatusCode.BadRequest)
            }
        }

        get("/devices") {
            val deviceList = gateway.devices.values.map { it ->
                buildJsonObject {
                    it.metadata.let {
                        Json.encodeToJsonElement(it).jsonObject.forEach { (key, value) -> put(key, value) }
                    }
                    put("parameters", buildJsonObject {})
                }
            }
            call.respondText { Json.encodeToString(deviceList) }
        }

        route("/scripts") {
            get {
                val scriptList = gateway.scriptManager.availableScripts.values.map { Json.encodeToJsonElement(it) }
                call.respondText(scriptList.toString())
            }

            get("/{scriptId}") {
                try {
                    val scriptId = call.parameters["scriptId"]!!
                    val script = gateway.scriptManager.availableScripts[scriptId]

                    if (script != null) {
                        call.respondText { Json.encodeToString(script) }
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            get("/{scriptId}/runs") {
                val scriptId = call.parameters["scriptId"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                val script = gateway.scriptManager.availableScripts[scriptId]
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                val activeRuns = gateway.scriptManager.activeScripts.values
                    .filter { it.script.name == script.name }
                    .map { it.toResponse() }
                val completedRuns = gateway.scriptManager.runs.values
                    .filter { it.script.name == script.name }
                    .map { it.toResponse() }
                call.respond((activeRuns + completedRuns).sortedByDescending { it.startedAt })
            }

            post("/{scriptId}/start") {
                val scriptId = call.parameters["scriptId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest)
                try {
                    val request = runCatching { call.receiveNullable<StartScriptRequest>() }.getOrNull() ?: StartScriptRequest()
                    val executionId = gateway.scriptManager.startScript(
                        scriptId,
                        request.arguments,
                        request.environment,
                        request.heartbeatId
                    )
                    call.respond(
                        HttpStatusCode.Accepted,
                        gateway.scriptManager.execution(executionId)!!.toResponse()
                    )
                } catch (e: NoSuchElementException) {
                    call.respond(HttpStatusCode.NotFound)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            route("/executions") {
                get {
                    call.respond(
                        (
                            gateway.scriptManager.activeScripts.values.map { it.toResponse() } +
                                gateway.scriptManager.runs.values.map { it.toResponse() }
                            ).sortedByDescending { it.startedAt }
                    )
                }

                get("/{executionId}") {
                    val executionId = parseUuid(call.parameters["executionId"])
                        ?: return@get call.respond(HttpStatusCode.BadRequest)
                    val execution = gateway.scriptManager.execution(executionId)
                    if (execution != null) {
                        return@get call.respond(execution.toResponse())
                    }
                    val run = gateway.scriptManager.runs[executionId]
                        ?: return@get call.respond(HttpStatusCode.NotFound)
                    call.respond(run.toResponse())
                }

                post("/{executionId}/stop") {
                    val executionId = parseUuid(call.parameters["executionId"])
                        ?: return@post call.respond(HttpStatusCode.BadRequest)
                    if (gateway.scriptManager.execution(executionId) == null) {
                        return@post call.respond(HttpStatusCode.NotFound)
                    }
                    gateway.scriptManager.stopScript(executionId)
                    call.respond(
                        HttpStatusCode.Accepted,
                        gateway.scriptManager.execution(executionId)!!.toResponse()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun parseUuid(value: String?): Uuid? =
    value?.let { runCatching { Uuid.parse(it) }.getOrNull() }

@OptIn(ExperimentalUuidApi::class)
private fun com.labeq.script.ScriptRunner.toResponse() = ScriptExecutionResponse(
    executionId = executionId.toString(),
    script = script,
    status = status.name,
    heartbeatId = heartbeatId,
    lastHeartbeatAt = lastHeartbeatAt,
    startedAt = startedAt,
    log = snapshot().log
)

@OptIn(ExperimentalUuidApi::class)
private fun com.labeq.script.ScriptRun.toResponse() = ScriptExecutionResponse(
    executionId = executionId.toString(),
    script = script,
    status = status,
    heartbeatId = heartbeatId,
    lastHeartbeatAt = lastHeartbeatAt,
    startedAt = startedAt,
    log = log
)
