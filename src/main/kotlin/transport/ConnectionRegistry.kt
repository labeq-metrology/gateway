package com.labeq.transport

fun interface ConnectionFactory {
    fun create(configuration: ConnectionConfiguration): DeviceConnection
}

class ConnectionRegistry {
    private val registrations = mutableMapOf<String, ConnectionFactory>()
    private val reflectiveRegistrations =
        mutableSetOf<String>()

    fun register(connectionPath: String, factory: ConnectionFactory) {
        require(connectionPath.isNotBlank()) { "Connection class path cannot be blank" }
        require(registrations.put(connectionPath, factory) == null) {
            "Connection already registered: $connectionPath"
        }
    }

    fun hasRegistration(connectionPath: String): Boolean =
        registrations.containsKey(connectionPath) || reflectiveRegistrations.contains(connectionPath)

    fun registerReflectively(connectionPath: String) {
        require(connectionPath.isNotBlank()) { "Connection class path cannot be blank" }
        reflectiveRegistrations.add(connectionPath)
    }

    fun create(connectionPath: String, configuration: ConnectionConfiguration): DeviceConnection {
        registrations[connectionPath]?.let { return it.create(configuration) }

        if (reflectiveRegistrations.contains(connectionPath)) {
            val connectionClass = Class.forName(connectionPath)
            val constructor = connectionClass.constructors.firstOrNull { it.parameterCount == 1 }
                ?: throw IllegalArgumentException("Unable to construct connection reflectively: $connectionPath")
            return constructor.newInstance(configuration) as DeviceConnection
        }

        throw IllegalArgumentException("Unknown connection implementation: $connectionPath")
    }
}
