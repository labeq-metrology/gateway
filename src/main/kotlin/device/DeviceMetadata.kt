package com.labeq.device

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class DeviceMetadata @OptIn(ExperimentalUuidApi::class) constructor(
    val uuid: Uuid,
    val name: String,
    val model: String,
    val manufacturer: String,
    val driver: String,
    val tags: List<String>,
) {
    @OptIn(ExperimentalUuidApi::class)
    val searchableText: String = listOfNotNull(uuid.toString(), name, manufacturer, model, tags.toString(), driver)
        .filter { it.isNotBlank() }
        .joinToString(" ")
}
