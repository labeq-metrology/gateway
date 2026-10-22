package com.labeq.script.rpc.service

import Heartbeat
import HeartbeatServiceGrpcKt
import com.labeq.script.ScriptManager
import io.grpc.Status

class HeartbeatService(private val scriptManager: ScriptManager) :
    HeartbeatServiceGrpcKt.HeartbeatServiceCoroutineImplBase() {
    override suspend fun beat(request: Heartbeat.HeartbeatRequest): Heartbeat.HeartbeatResponse =
        respond(
            action = {
                if (!scriptManager.handleHeartbeat(request.id, request.status)) {
                    throw Status.NOT_FOUND
                        .withDescription("No active script found for worker ID ${request.id}")
                        .asException()
                }
                request.id
            },
            response = { id -> Heartbeat.HeartbeatResponse.newBuilder().setId(id).build() }
        )
}
