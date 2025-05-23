package com.labeq.config

import kotlinx.serialization.Serializable

// Config data types
@JvmInline
@Serializable
value class Port(val value: Int) {}

@JvmInline
@Serializable
value class Hostname(val value: String) {}

@JvmInline
@Serializable
value class FilePath(val value: String) {}

// Configuration definitions
@Serializable
data class LabeqGatewayConfiguration(
    val server: ServerConfig,
    val mqttConfig: MQTTConfig
)

@Serializable
data class ServerConfig(
    val host: Hostname,
    val port: Port
);

@Serializable
data class MQTTConfig(
    val host: Hostname,
    val port: Port,
    val ssl: Boolean,
    val sslCertPath: FilePath,
    val sslKeyPath: FilePath
);