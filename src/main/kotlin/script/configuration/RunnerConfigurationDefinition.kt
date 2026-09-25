package com.labeq.script.configuration

data class RunnerConfigurationDefinition(
    val name: String,
    val extensions: List<String>,
    val commandTemplate: List<String>,
    val workingDirectory: String = "{file_dir}",
    val env: Map<String, String> = emptyMap()
)
