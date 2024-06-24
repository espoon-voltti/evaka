// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

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

    class CapturedRequest(
        val headers: Map<String, String>,
        val body: ByteArray
    )

    @PostMapping("/subscription/{id}")
    fun postNotification(
        @PathVariable id: String,
        request: WebRequest,
        body: InputStream
    ): ResponseEntity<Nothing> {
        lock.withLock {
            capturedRequests
                .computeIfAbsent(id) { mutableListOf() }
                .add(
                    CapturedRequest(
                        headers =
                            request.headerNames
                                .asSequence()
                                .mapNotNull { name -> request.getHeader(name)?.let { name to it } }
                                .toMap(),
                        body = body.readAllBytes()
                    )
                )
        }
        return ResponseEntity.created(URI("")).build()
    }

    fun clearData() = lock.withLock { capturedRequests.clear() }

    fun getCapturedRequests(id: String): List<CapturedRequest> = lock.withLock { capturedRequests[id]?.toList() ?: emptyList() }
}
