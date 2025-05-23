package com.labeq.builtin.drivers.hp.hp3325

import com.labeq.device.DeviceDriver
import com.labeq.transport.DeviceConnection
import com.labeq.transport.MessageResult
import com.labeq.transport.ParameterValue
import com.labeq.transport.ReturnType

class HP3325Driver(deviceConnection: DeviceConnection) : DeviceDriver(deviceConnection) {
    override suspend fun queryParameter(
        parameter: String,
        returnType: ReturnType
    ): MessageResult<ParameterValue> {
        TODO("Not yet implemented")
    }

    override suspend fun writeParameter(
        parameter: String,
        value: ParameterValue,
        returnType: ReturnType
    ): MessageResult<ParameterValue> {
        TODO("Not yet implemented")
    }

    override suspend fun writeData(
        data: ByteArray,
        returnType: ReturnType
    ): MessageResult<ParameterValue> {
        TODO("Not yet implemented")
    }

    override suspend fun writeData(
        data: String,
        returnType: ReturnType
    ): MessageResult<ParameterValue> {
        TODO("Not yet implemented")
    }

    override fun connect(): Boolean {
        TODO("Not yet implemented")
    }

    override fun disconnect(): Boolean {
        TODO("Not yet implemented")
    }

}
