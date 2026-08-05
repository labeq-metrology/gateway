package com.labeq.script.rpc.service

import Heartbeat
import HeartbeatServiceGrpcKt

class HeartbeatService : HeartbeatServiceGrpcKt.HeartbeatServiceCoroutineImplBase() {
    override suspend fun beat(request: Heartbeat.HeartbeatRequest): Heartbeat.HeartbeatResponse {
        return Heartbeat.HeartbeatResponse.newBuilder().setId(request.id).build()
    }
}
