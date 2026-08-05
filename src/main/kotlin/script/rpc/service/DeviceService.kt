package com.labeq.script.rpc.service

import com.google.rpc.Code
import com.google.rpc.ErrorInfo
import io.grpc.Status
import com.labeq.device.DeviceMetadata
import com.labeq.device.GenericDevice
import com.labeq.script.rpc.proto.device.Device
import com.labeq.script.rpc.proto.device.DeviceServiceGrpcKt
import com.labeq.script.rpc.proto.device.GetDeviceResponseKt
import me.xdrop.fuzzywuzzy.FuzzySearch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class DeviceService(val devices: MutableMap<Uuid, GenericDevice>) :
    DeviceServiceGrpcKt.DeviceServiceCoroutineImplBase() {

    override suspend fun getDevice(request: Device.GetDeviceRequest): Device.GetDeviceResponse {
        val device = devices.values.map { it.metadata }.find {
            it.uuid.toString() == request.identifier ||
                    it.name == request.identifier
        }

        if (device == null) {
            throw Status.NOT_FOUND
                .withDescription("No device matches the identifier ${request.identifier}")
                .asException()
        }

        return Device.GetDeviceResponse.newBuilder()
            .setUuid(device.uuid.toString())
            .build()
    }

    override suspend fun lookupDevice(request: Device.DeviceLookupRequest): Device.DeviceLookupResponse {
        val query = request.query.trim()
        if (query.isBlank()) {
            throw Status.INVALID_ARGUMENT
                .withDescription("No query provided")
                .asException()
        }

        // Score results and filter noise
        val scoredResults = devices.values.asSequence()
            .map { device -> device.metadata to calculateDeviceScore(query, device.metadata) }
            .filter { it.second >= 60 }
            .sortedByDescending { it.second }
            .toList()

        // Handle unique request constraints
        if (request.unique && scoredResults.size > 1) {
            throw Status.RESOURCE_EXHAUSTED
                .withDescription("More than one device matches the query ${request.query}")
                .asException()
        }

        val finalUuids = if (request.unique) {
            listOf(scoredResults.first().first.uuid.toString())
        } else {
            scoredResults.map { it.first.uuid.toString() }
        }

        if (finalUuids.isEmpty()) {
            throw Status.NOT_FOUND
                .withDescription("No device matches the query ${request.query}")
                .asException()
        }

        return Device.DeviceLookupResponse.newBuilder()
            .addAllUuid(finalUuids)
            .build()
    }

    override suspend fun getDeviceMetadata(request: Device.DeviceMetadataRequest): Device.DeviceMetadataResponse {
        val uuid = try {
            Uuid.parse(request.uuid)
        } catch (e: IllegalArgumentException) {
            throw Status.INVALID_ARGUMENT
                .withDescription("Malformed UUID format: ${request.uuid}")
                .asException()
        }

        val device = devices.get(uuid)
            ?: throw Status.INVALID_ARGUMENT.withDescription("Device not found: $uuid").asException()

        val metadata = device.metadata
        return Device.DeviceMetadataResponse.newBuilder()
            .setUuid(metadata.uuid.toString())
            .setName(metadata.name)
            .setModel(metadata.model)
            .setManufacturer(metadata.manufacturer)
            .setDriver(metadata.driver)
            .setTags(metadata.tags.joinToString(","))
            .build()
    }

    private fun calculateDeviceScore(query: String, metadata: DeviceMetadata): Int {
        val queryTokens = query.lowercase().split("\\s+".toRegex())
        var totalScore = 0

        // Find the best field match for each query token
        for (token in queryTokens) {
            val bestMatchForToken = maxOf(
                scoreField(token, metadata.manufacturer, 1.0),
                scoreField(token, metadata.model, 1.2),
                scoreField(token, metadata.name, 1.1),
                scoreField(token, metadata.tags.toString(), 0.8),
                scoreField(token, metadata.driver, 0.5)
            )
            totalScore += bestMatchForToken
        }

        // Average score across all tokens
        return (totalScore / queryTokens.size).coerceAtMost(100)
    }

    private fun scoreField(token: String, fieldValue: String?, weight: Double): Int {
        if (fieldValue.isNullOrBlank()) return 0

        val targetTokens = fieldValue.lowercase().split("\\s+".toRegex())

        // Exact match
        if (targetTokens.any { it == token }) {
            return (100 * weight).toInt()
        }

        // Substring match
        if (targetTokens.any { it.contains(token) || token.contains(it) }) {
            return (80 * weight).toInt()
        }

        // Fuzzy match for typos
        val fuzzyScore = targetTokens.maxOfOrNull { FuzzySearch.ratio(token, it) } ?: 0
        return if (fuzzyScore > 65) (fuzzyScore * weight).toInt() else 0
    }
}