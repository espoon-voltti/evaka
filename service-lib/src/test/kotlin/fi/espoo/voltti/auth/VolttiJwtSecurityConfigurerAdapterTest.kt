// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.LocalDate.now
import java.time.ZoneOffset
import java.util.Date

@RunWith(SpringRunner::class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [
        VolttiJwtSecurityConfigurerAdapterTest.TestApplication::class
    ],
    properties = ["fi.espoo.voltti.auth.jwks.default.url=classpath:jwks.json"]
)
class VolttiJwtSecurityConfigurerAdapterTest {
    @SpringBootApplication
    @RestController
    class TestApplication {
        fun main(args: Array<String>) {}
        @GetMapping("/hello")
        fun hello(): ResponseEntity<String> = ResponseEntity.ok("Hello")

        @GetMapping("/health", produces = ["application/json"])
        fun health(): ResponseEntity<Any> = ResponseEntity.ok(mapOf("STATUS" to "OK"))
    }

    @LocalServerPort
    lateinit var port: String

    @Autowired
    lateinit var rest: TestRestTemplate

    @Autowired
    lateinit var context: ApplicationContext

    @Test
    fun `web application endpoints are protected`() {
        val response: ResponseEntity<Any> = rest.exchange("${localhost()}/hello", HttpMethod.GET, null, Any::class.java)
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `WebSecurityConfigurerAdapter is loaded with web applications`() {
        val httpConfig = context.getBean(WebSecurityConfigurerAdapter::class.java)

        assertNotNull(httpConfig)
    }

    @Test
    fun `User can authenticate with a valid JWT`() {
        val request = createRequest(getValidJwt())
        val response: ResponseEntity<String> =
            rest.exchange("${localhost()}/hello", HttpMethod.GET, request, String::class.java)

        assertEquals("Hello", response.body)
    }

    @Test
    fun `Expired JWT is rejected`() {
        val request = createRequest(getExpiredJwt())
        val response: ResponseEntity<String> =
            rest.exchange("${localhost()}/hello", HttpMethod.GET, request, String::class.java)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `JWT with unknown kid is rejected`() {
        val request = createRequest(getInvalidKidJwt())
        val response: ResponseEntity<String> =
            rest.exchange("${localhost()}/hello", HttpMethod.GET, request, String::class.java)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `Health endpoint with GET is open`() {
        val response: ResponseEntity<Any> = rest.exchange("${localhost()}/health", HttpMethod.GET, null, Any::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
    }

    private fun localhost() = "http://localhost:$port"

    private fun getAlgorithm() = Algorithm.RSA256(null, privateKey())

    private fun privateKey(): RSAPrivateKey {
        val key = VolttiJwtSecurityConfigurerAdapterTest::class.java
            .classLoader
            .getResource("private_key.der")
            .readBytes()
        val pkcs8spec = PKCS8EncodedKeySpec(key)
        return KeyFactory.getInstance("RSA").generatePrivate(pkcs8spec) as RSAPrivateKey
    }

    private fun getValidJwt() = JWT.create()
        .withKeyId("test-key-id")
        .sign(getAlgorithm())!!

    private fun getExpiredJwt() = JWT.create()
        .withKeyId("test-key-id")
        .withExpiresAt(Date.from(now().minusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC)))
        .sign(getAlgorithm())

    private fun getInvalidKidJwt() = JWT.create()
        .withKeyId("invalid-key-id")
        .sign(getAlgorithm())!!

    private fun createRequest(jwt: String) = with(HttpHeaders()) {
        set(HttpHeaders.AUTHORIZATION, "Bearer $jwt")
        HttpEntity<String>(this)
    }
}
