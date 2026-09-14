// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import java.io.InputStream
import java.net.URI
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.WebRequest

@RestController
@RequestMapping("/public/mock-web-push")
class MockWebPushEndpoint {
    private val lock = ReentrantLock()
    private var capturedRequests: MutableMap<String, MutableList<CapturedRequest>> = mutableMapOf()
    private var queuedResponses: MutableMap<String, ArrayDeque<QueuedResponse>> = mutableMapOf()

    class CapturedRequest(val headers: Map<String, String>, val body: ByteArray)

    private class QueuedResponse(val status: Int, val retryAfter: String?)

    /** Answers the next request to [id] with [status] instead of the default 201 Created */
    fun respondOnceWith(id: String, status: Int, retryAfter: String? = null) = lock.withLock {
        queuedResponses.computeIfAbsent(id) { ArrayDeque() }.add(QueuedResponse(status, retryAfter))
    }

    @PostMapping("/subscription/{id}")
    fun postNotification(
        @PathVariable id: String,
        request: WebRequest,
        body: InputStream,
    ): ResponseEntity<Nothing> {
        lock.withLock {
            capturedRequests
                .computeIfAbsent(id) { mutableListOf() }
                .add(
                    CapturedRequest(
                        headers =
                            request.headerNames
                                .asSequence()
                                .mapNotNull { name ->
                                    request.getHeader(name)?.let { name.lowercase() to it }
                                }
                                .toMap(),
                        body = body.readAllBytes(),
                    )
                )
        }
        val queued =
            lock.withLock { queuedResponses[id]?.removeFirstOrNull() }
                ?: return ResponseEntity.created(URI("")).build()
        val response = ResponseEntity.status(queued.status)
        queued.retryAfter?.let { response.header("Retry-After", it) }
        return response.build()
    }

    fun clearData() = lock.withLock {
        capturedRequests.clear()
        queuedResponses.clear()
    }

    fun getCapturedRequests(id: String): List<CapturedRequest> = lock.withLock {
        capturedRequests[id]?.toList() ?: emptyList()
    }
}
