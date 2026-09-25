package com.labeq.script.configuration

import kotlinx.io.files.Path

/**
 * A fully resolved context containing everything needed to spawn the process.
 */
data class PreparedExecution(
    val command: List<String>,
    val workingDirectory: String,
    val environment: Map<String, String>
)

class RunnerConfiguration(val definition: RunnerConfigurationDefinition) {

    /**
     * Resolves the command template, working directory, and environment variables.
     *
     * @param file The target script file.
     * @param arguments Extra arguments to append to the command.
     * @param runtimeEnv Environment variables specific to this run (overrides definition env vars).
     */
    fun prepareCommand(
        file: Path,
        arguments: List<String> = emptyList(),
        runtimeEnv: Map<String, String> = emptyMap()
    ): PreparedExecution {
        val tokens = buildTokens(file)

        val resolvedCommand = definition.commandTemplate.map { templatePart ->
            replaceTokens(templatePart, tokens)
        }.toMutableList()
        resolvedCommand.addAll(arguments)

        val resolvedWorkingDir = replaceTokens(definition.workingDirectory, tokens)

        val mergedEnv = buildMap {
            putAll(definition.env)
            putAll(runtimeEnv)
        }

        return PreparedExecution(
            command = resolvedCommand,
            workingDirectory = resolvedWorkingDir,
            environment = mergedEnv
        )
    }

    fun supportsExtension(extension: String): Boolean {
        return definition.extensions.any { it.equals(extension, ignoreCase = true) }
    }

    private fun buildTokens(file: Path): Map<String, String> {
        val fileName = file.name
        val fileStem = if (fileName.contains(".")) fileName.substringBeforeLast(".") else fileName
        val fileDir = file.parent?.toString() ?: "."

        return mapOf(
            "{file}" to file.toString(),
            "{file_dir}" to fileDir,
            "{file_name}" to fileName,
            "{file_stem}" to fileStem
        )
    }

    private fun replaceTokens(input: String, tokens: Map<String, String>): String {
        var result = input
        for ((token, value) in tokens) {
            result = result.replace(token, value)
        }
        return result
    }
}
