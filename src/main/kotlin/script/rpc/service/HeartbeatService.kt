package com.labeq.script.rpc.service

import Heartbeat
import HeartbeatServiceGrpcKt
import com.labeq.script.ScriptManager

class HeartbeatService(private val scriptManager: ScriptManager) :
    HeartbeatServiceGrpcKt.HeartbeatServiceCoroutineImplBase() {
    override suspend fun beat(request: Heartbeat.HeartbeatRequest): Heartbeat.HeartbeatResponse {
        scriptManager.handleHeartbeat(request.id, request.status)
        return Heartbeat.HeartbeatResponse.newBuilder().setId(request.id).build()
    }
}
