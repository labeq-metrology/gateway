package com.labeq.script.rpc.service

import io.grpc.Status
import com.labeq.device.DeviceMetadata
import com.labeq.device.GenericDevice
import com.labeq.script.rpc.proto.device.Device
import com.labeq.script.rpc.proto.device.DeviceServiceGrpcKt
import me.xdrop.fuzzywuzzy.FuzzySearch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class DeviceService(val devices: MutableMap<Uuid, GenericDevice>) :
    DeviceServiceGrpcKt.DeviceServiceCoroutineImplBase() {

    override suspend fun getDevice(request: Device.GetDeviceRequest): Device.GetDeviceResponse =
        respond {
                val identifier = requireNonBlank(request.identifier, "identifier")
                val device = devices.values.map { it.metadata }.find {
                    it.uuid.toString() == identifier || it.name == identifier
                } ?: throw Status.NOT_FOUND
                    .withDescription("No device matches the identifier ${request.identifier}")
                    .asException()

                Device.GetDeviceResponse.newBuilder()
                    .setUuid(device.uuid.toString())
                    .build()
            }

    override suspend fun lookupDevice(request: Device.DeviceLookupRequest): Device.DeviceLookupResponse =
        respond {
                val query = requireNonBlank(request.query, "query")

                val scoredResults = devices.values.asSequence()
                    .map { device -> device.metadata to calculateDeviceScore(query, device.metadata) }
                    .filter { it.second >= 60 }
                    .sortedByDescending { it.second }
                    .toList()

                if (request.unique && scoredResults.size > 1) {
                    throw Status.RESOURCE_EXHAUSTED
                        .withDescription("More than one device matches the query ${request.query}")
                        .asException()
                }

                if (scoredResults.isEmpty()) {
                    throw Status.NOT_FOUND
                        .withDescription("No device matches the query ${request.query}")
                        .asException()
                }

                val finalUuids = if (request.unique) {
                    listOf(scoredResults.first().first.uuid.toString())
                } else {
                    scoredResults.map { it.first.uuid.toString() }
                }

                Device.DeviceLookupResponse.newBuilder()
                    .addAllUuid(finalUuids)
                    .build()
            }

    override suspend fun getDeviceMetadata(request: Device.DeviceMetadataRequest): Device.DeviceMetadataResponse =
        respond {
                val uuid = parseUuid(request.uuid, "uuid")

                val device = devices[uuid]
                    ?: throw Status.NOT_FOUND.withDescription("Device not found: $uuid").asException()

                val metadata = device.metadata
                Device.DeviceMetadataResponse.newBuilder()
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
                scoreField(token, metadata.tags.joinToString(" "), 0.8),
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