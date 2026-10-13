package com.labeq.script

import kotlinx.serialization.Serializable

@Serializable
data class Script(
    val name: String,
    val description: String,
    val file: String,
    val runner: String
)
