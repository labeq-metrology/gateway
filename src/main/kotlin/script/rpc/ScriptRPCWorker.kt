package com.labeq.script.rpc

import com.labeq.script.rpc.service.DeviceService
import com.labeq.script.rpc.service.HeartbeatService
import com.labeq.script.rpc.service.ParameterService
import com.labeq.server.LabeqGateway
import io.grpc.Server
import io.grpc.ServerBuilder
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class ScriptRPCWorker(val gateway: LabeqGateway) {
    private val log: Logger = LoggerFactory.getLogger("ScriptRPCWorker")

    private val workerScope = CoroutineScope(Dispatchers.IO + CoroutineName("ScriptRPCWorker"))

    @Volatile
    private var server: Server? = null
    private var serverJob: Job? = null

    @Synchronized
    fun start() {
        if (server != null && !server!!.isShutdown) {
            log.warn("RPC worker is already running.")
            return
        }

        val port = gateway.configuration.rpc.port.value

        val newServer = ServerBuilder
            .forPort(port)
            .addService(DeviceService(gateway.devices))
            .addService(HeartbeatService())
            .addService(ParameterService(gateway.devices))
            .build()

        server = newServer

        serverJob = workerScope.launch {
            try {
                newServer.start()
                log.info("RPC worker started. Listening on port $port.")
                withContext(Dispatchers.IO) {
                    newServer.awaitTermination()
                }
            } catch (e: Exception) {
                log.error("Error occurred in RPC worker server", e)
            }
        }
    }

    @Synchronized
    fun stop() {
        val currentServer = server ?: return

        log.info("Stopping RPC worker...")

        currentServer.shutdown()

        try {
            if (!currentServer.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("RPC worker did not stop gracefully in 5s. Forcing shutdown...")
                currentServer.shutdownNow()
            }
        } catch (e: InterruptedException) {
            currentServer.shutdownNow()
            Thread.currentThread().interrupt()
        } finally {
            serverJob?.cancel()
            server = null
            log.warn("RPC worker stopped.")
        }
    }

    @Synchronized
    fun restart() {
        stop()
        start()
    }

    fun close() {
        stop()
        workerScope.cancel()
    }
}