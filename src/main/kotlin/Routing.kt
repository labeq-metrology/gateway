package com.labeq

import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun Application.configureRouting(gateway: LabeqGateway) {
    install(AutoHeadResponse)
    routing {
        get("/devices") {
            val deviceList = gateway.devices.values.map {
                buildJsonObject {
                    put("uuid", it.uuid.toString())
                    put("name", it.name)
                    put("model", it.model)
                    put("manufacturer", it.manufacturer)
                    put("driver", it.driver.javaClass.canonicalName)
                }
            }
            call.respondText { Json.encodeToString(deviceList) }
        }
    }
}
