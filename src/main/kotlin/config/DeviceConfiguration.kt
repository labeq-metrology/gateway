package com.labeq.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class DevicesConfiguration(
    val schemaVersion: Int = 1,
    val devices: List<DeviceInstanceConfiguration> = emptyList()
)

@Serializable
data class DeviceInstanceConfiguration(
    val uuid: String = "",
    val name: String,
    val model: String,
    val manufacturer: String,
    val driver: String,
    val tags: List<String> = emptyList(),
    val enabled: Boolean = true,
    val connection: DeviceConnectionConfiguration
)

@Serializable
data class DeviceConnectionConfiguration(
    val implementation: String,
    val configuration: JsonObject
)
