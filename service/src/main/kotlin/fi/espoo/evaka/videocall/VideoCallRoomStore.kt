// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.videocall

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import java.util.UUID
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import tools.jackson.core.type.TypeReference

private val logger = KotlinLogging.logger {}

/**
 * Cross-JVM store for video-call state backed by S3. POC-grade: read-modify-write of the room state
 * file has no optimistic concurrency (no ETag preconditions), so a rare race where two guardians
 * accept at the exact same moment may resolve to "second write wins". Signal queues are split per
 * sender direction so only one writer touches each file, avoiding lost-append races on the
 * high-frequency path. No DeleteObject calls — ENDED rooms are filtered out on read and eventually
 * age out via the TTL cutoff.
 */
class VideoCallRoomStore(private val s3Client: S3Client, private val bucket: String) {
    private val jsonMapper = defaultJsonMapperBuilder().build()

    private val roomTtl: Duration = Duration.ofHours(2)
    private val maxSignalsPerDirection = 200

    private fun roomKey(id: UUID): String = "video-call/rooms/$id.json"

    private fun signalsKey(roomId: UUID, from: VideoCallRole): String =
        "video-call/signals/$roomId/from-${from.name.lowercase()}.json"

    fun create(
        employeeId: EmployeeId,
        employeeName: String,
        childId: ChildId,
        childName: String,
        guardianIds: Set<PersonId>,
        now: HelsinkiDateTime,
    ): VideoCallRoom {
        val id = UUID.randomUUID()
        val room =
            VideoCallRoom(
                id = id,
                employeeId = employeeId,
                employeeName = employeeName,
                childId = childId,
                childName = childName,
                guardianIds = guardianIds,
                createdAt = now,
                status = VideoCallRoomStatus.RINGING,
                answeredBy = null,
                endedAt = null,
            )
        writeRoom(room)
        return room
    }

    fun get(roomId: UUID): VideoCallRoom? = readRoom(roomId)

    fun listPendingFor(personId: PersonId, now: HelsinkiDateTime): List<VideoCallRoom> {
        val cutoff = now.minus(roomTtl)
        return listRooms().filter {
            it.status == VideoCallRoomStatus.RINGING &&
                it.createdAt >= cutoff &&
                personId in it.guardianIds
        }
    }

    fun accept(roomId: UUID, personId: PersonId): VideoCallRoom? {
        val current = readRoom(roomId) ?: return null
        if (personId !in current.guardianIds) return null
        if (current.status != VideoCallRoomStatus.RINGING) return null
        val next = current.copy(status = VideoCallRoomStatus.ACTIVE, answeredBy = personId)
        writeRoom(next)
        return next
    }

    fun end(roomId: UUID, now: HelsinkiDateTime): VideoCallRoom? {
        val current = readRoom(roomId) ?: return null
        val next = current.copy(status = VideoCallRoomStatus.ENDED, endedAt = now)
        writeRoom(next)
        return next
    }

    fun postSignal(roomId: UUID, from: VideoCallRole, kind: String, data: String): Boolean {
        readRoom(roomId) ?: return false
        val key = signalsKey(roomId, from)
        val existing = readSignals(key) ?: emptyList()
        val nextSeq = (existing.maxOfOrNull { it.seq } ?: 0L) + 1L
        val appended =
            existing + VideoCallSignal(seq = nextSeq, from = from, kind = kind, data = data)
        val bounded =
            if (appended.size > maxSignalsPerDirection) appended.takeLast(maxSignalsPerDirection)
            else appended
        writeSignals(key, bounded)
        return true
    }

    fun drainSignalsFor(
        roomId: UUID,
        recipient: VideoCallRole,
        since: Long,
    ): List<VideoCallSignal> {
        val sourceRole =
            when (recipient) {
                VideoCallRole.CITIZEN -> VideoCallRole.EMPLOYEE
                VideoCallRole.EMPLOYEE -> VideoCallRole.CITIZEN
            }
        val signals = readSignals(signalsKey(roomId, sourceRole)) ?: return emptyList()
        return signals.filter { it.seq > since }
    }

    private fun readRoom(roomId: UUID): VideoCallRoom? {
        val request = GetObjectRequest.builder().bucket(bucket).key(roomKey(roomId)).build()
        return try {
            s3Client.getObject(request).use { stream ->
                jsonMapper.readValue(stream.readAllBytes(), VideoCallRoom::class.java)
            }
        } catch (_: NoSuchKeyException) {
            null
        } catch (e: Exception) {
            logger.warn(e) { "Failed to read video-call room $roomId" }
            null
        }
    }

    private fun writeRoom(room: VideoCallRoom) {
        val bytes = jsonMapper.writeValueAsBytes(room)
        val request =
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(roomKey(room.id))
                .contentType("application/json")
                .build()
        s3Client.putObject(request, RequestBody.fromBytes(bytes))
    }

    private fun listRooms(): List<VideoCallRoom> {
        val request =
            ListObjectsV2Request.builder().bucket(bucket).prefix("video-call/rooms/").build()
        val keys =
            try {
                s3Client.listObjectsV2(request).contents().map { it.key() }
            } catch (e: Exception) {
                logger.warn(e) { "Failed to list video-call rooms" }
                return emptyList()
            }
        return keys.mapNotNull { key ->
            val idStr = key.substringAfterLast('/').removeSuffix(".json")
            val id =
                try {
                    UUID.fromString(idStr)
                } catch (_: IllegalArgumentException) {
                    return@mapNotNull null
                }
            readRoom(id)
        }
    }

    private fun readSignals(key: String): List<VideoCallSignal>? {
        val request = GetObjectRequest.builder().bucket(bucket).key(key).build()
        return try {
            s3Client.getObject(request).use { stream ->
                jsonMapper.readValue(
                    stream.readAllBytes(),
                    object : TypeReference<List<VideoCallSignal>>() {},
                )
            }
        } catch (_: NoSuchKeyException) {
            null
        } catch (e: Exception) {
            logger.warn(e) { "Failed to read video-call signals at $key" }
            null
        }
    }

    private fun writeSignals(key: String, signals: List<VideoCallSignal>) {
        val bytes = jsonMapper.writeValueAsBytes(signals)
        val request =
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("application/json")
                .build()
        s3Client.putObject(request, RequestBody.fromBytes(bytes))
    }
}
