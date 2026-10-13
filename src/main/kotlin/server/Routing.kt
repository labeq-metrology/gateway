package com.labeq.server

import com.labeq.device.GenericDevice
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun Application.configureRouting(gateway: LabeqGateway) {
    install(AutoHeadResponse)
    routing {
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
        }

    }
}
