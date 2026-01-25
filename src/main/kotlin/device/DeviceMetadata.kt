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
)