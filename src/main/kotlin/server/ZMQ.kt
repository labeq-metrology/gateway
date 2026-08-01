package com.labeq.server

import org.zeromq.SocketType
import org.zeromq.ZContext

fun initializeZMQ(gateway: LabeqGateway): Unit {
    val context = ZContext()
    val socket = context.createSocket(SocketType.REP)
    socket.bind("tcp://*:7777")

    while (!Thread.currentThread().isInterrupted) {}
}
