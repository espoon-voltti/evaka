// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MockWebPushServer(port: Int) : AutoCloseable {
    private val app =
        Javalin.create { config ->
                config.router.apiBuilder { post("/subscription/{id}", ::postNotification) }
            }
            .start(port)

    private val lock = ReentrantLock()
    private var capturedRequests: MutableMap<String, MutableList<CapturedRequest>> = mutableMapOf()

    val port
        get() = app.port()

    class CapturedRequest(val headers: Map<String, String>, val body: ByteArray)

    private fun postNotification(ctx: Context) {
        val id = ctx.pathParam("id")
        lock.withLock {
            val request = CapturedRequest(headers = ctx.headerMap(), body = ctx.bodyAsBytes())
            capturedRequests.computeIfAbsent(id) { mutableListOf() }.add(request)
        }
        ctx.status(HttpStatus.CREATED).result("OK")
    }

    fun clearData() = lock.withLock { capturedRequests.clear() }

    fun getCapturedRequests(id: String): List<CapturedRequest> =
        lock.withLock { capturedRequests[id]?.toList() ?: emptyList() }

    override fun close() {
        app.stop()
    }

    companion object {
        fun start(): MockWebPushServer {
            return MockWebPushServer(port = 0)
        }
    }
}
