// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient.rest

import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

typealias AccessToken = String

typealias ExternalId = String

typealias FileId = UUID

typealias MessageId = Long

@RestController
@RequestMapping("/public/mock-sfi-messages")
class MockSfiMessagesRestEndpoint {
    class CapturedFile(val name: String?, val content: ByteArray)

    @PostMapping(
        "/v1/token",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun getAccessToken(@RequestBody body: AccessTokenRequestBody): ResponseEntity<Any> =
        lock.withLock {
            if (body.username == USERNAME && body.password == PASSWORD) {
                val token = UUID.randomUUID().toString()
                tokens.add(token)
                ResponseEntity.ok(AccessTokenResponse(token))
            } else ResponseEntity.badRequest().body(ApiError("Invalid credentials"))
        }

    @PostMapping(
        "/v1/files",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun uploadFile(
        @RequestHeader("Authorization") authorization: String?,
        @RequestPart file: MultipartFile,
    ): ResponseEntity<Any> =
        lock.withLock {
            if (!tokens.contains(authorization?.removePrefix("Bearer "))) {
                ResponseEntity.status(401).body(ApiError("Invalid token"))
            } else {
                val id = UUID.randomUUID()
                files[id] = CapturedFile(file.originalFilename, file.bytes)
                ResponseEntity.ok(NewFileResponse(id))
            }
        }

    @PostMapping(
        "/v1/messages",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun sendMessage(
        @RequestHeader("Authorization") authorization: String?,
        @RequestBody body: NewMessageFromClientOrganisation,
    ): ResponseEntity<Any> =
        lock.withLock {
            if (!tokens.contains(authorization?.removePrefix("Bearer "))) {
                ResponseEntity.status(401).body(ApiError("Invalid token"))
            } else if (messages.contains(body.externalId)) {
                ResponseEntity.status(409).body(ApiError("Message already sent"))
            } else {
                val id = nextMessageId++
                messages[body.externalId] = id to body
                ResponseEntity.ok(MessageResponse(id))
            }
        }

    companion object {
        const val USERNAME = "test-user"
        const val PASSWORD = "test-password"

        private val lock = ReentrantLock()

        private val tokens = mutableSetOf<AccessToken>()
        private val files = mutableMapOf<FileId, CapturedFile>()
        private val messages =
            mutableMapOf<ExternalId, Pair<MessageId, NewMessageFromClientOrganisation>>()
        private var nextMessageId: MessageId = 1L

        fun reset() =
            lock.withLock {
                tokens.clear()
                files.clear()
                messages.clear()
                nextMessageId = 1L
            }

        fun clearTokens() = lock.withLock { tokens.clear() }

        fun getCapturedFiles() = lock.withLock { files.toMap() }

        fun getCapturedMessages() = lock.withLock { messages.toMap() }
    }
}
