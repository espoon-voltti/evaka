// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo.bi

import fi.espoo.evaka.EspooBiEnv
import fi.espoo.evaka.Sensitive
import java.io.InputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.WebRequest

@Configuration
class MockBiConfiguration {
    @Bean
    fun espooBiEnv(): EspooBiEnv =
        EspooBiEnv(url = "", username = "user", password = Sensitive("password"))
}

@RestController
@RequestMapping("/public/mock-espoo-bi")
class MockBiEndpoint(env: EspooBiEnv) {
    private val lock = ReentrantLock()
    private var capturedRequests: MutableMap<String, CapturedRequest> = mutableMapOf()

    private val authorizationHeader =
        "Basic ${HttpHeaders.encodeBasicAuth(env.username, env.password.value, Charsets.UTF_8)}"

    class CapturedRequest(val body: ByteArray)

    @PutMapping("/report")
    fun putReport(
        @RequestParam filename: String,
        request: WebRequest,
        body: InputStream
    ): ResponseEntity<Nothing> =
        if (request.getHeader("authorization") != authorizationHeader)
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        else
            lock.withLock {
                capturedRequests[filename] = CapturedRequest(body = body.readAllBytes())
                ResponseEntity.ok().build()
            }

    fun clearData() = lock.withLock { capturedRequests.clear() }

    fun getCapturedRequests(): Map<String, CapturedRequest> =
        lock.withLock { capturedRequests.toMap() }
}
