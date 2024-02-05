// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo.bi

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.put
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.security.BasicAuthCredentials
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MockBiServer(private val credentials: BasicAuthCredentials?, port: Int) : AutoCloseable {
    private val app =
        Javalin.create { config -> config.router.apiBuilder { put("/report", ::putReport) } }
            .start(port)

    private val lock = ReentrantLock()
    private var capturedRequests: MutableMap<String, CapturedRequest> = mutableMapOf()

    val port
        get() = app.port()

    class CapturedRequest(val body: ByteArray)

    private fun putReport(ctx: Context) {
        if (credentials != null && credentials != ctx.basicAuthCredentials()) {
            ctx.status(HttpStatus.UNAUTHORIZED)
            return
        }
        when (val file = ctx.queryParam("filename")) {
            null -> ctx.status(HttpStatus.BAD_REQUEST)
            else -> {
                lock.withLock {
                    val request = CapturedRequest(body = ctx.bodyAsBytes())
                    capturedRequests[file] = request
                }
                ctx.status(HttpStatus.OK)
            }
        }
    }

    fun clearData() = lock.withLock { capturedRequests.clear() }

    fun getCapturedRequests(): Map<String, CapturedRequest> =
        lock.withLock { capturedRequests.toMap() }

    override fun close() {
        app.stop()
    }

    companion object {
        fun start(credentials: BasicAuthCredentials): MockBiServer {
            return MockBiServer(port = 0, credentials = credentials)
        }
    }
}
