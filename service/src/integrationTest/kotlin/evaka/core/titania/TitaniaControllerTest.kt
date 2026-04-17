// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.titania

import evaka.core.FullApplicationTest
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.asUser
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.insert
import java.nio.charset.StandardCharsets
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert.assertEquals
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.core.io.ClassPathResource
import org.springframework.util.StreamUtils

internal class TitaniaControllerTest : FullApplicationTest(resetDbBeforeEach = true) {

    private val client = OkHttpClient()

    private val jsonMediaType = "application/json".toMediaType()

    private fun readResource(path: String): String =
        ClassPathResource(path).inputStream.use {
            StreamUtils.copyToString(it, StandardCharsets.UTF_8)
        }

    @Test
    fun `put working time events with system user should respond 403`() {
        val request =
            Request.Builder()
                .url("http://localhost:$httpPort/integration/titania/working-time-events")
                .put(
                    readResource("titania/titania-update-request-valid-example-data.json")
                        .toRequestBody(jsonMediaType)
                )
                .asUser(AuthenticatedUser.SystemInternalUser)
                .build()

        client.newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(403)
        }
    }

    @Test
    fun `put working time events without employee id should respond 400`() {
        val request =
            Request.Builder()
                .url("http://localhost:$httpPort/integration/titania/working-time-events")
                .put(
                    readResource("titania/titania-update-request-without-employee-id.json")
                        .toRequestBody(jsonMediaType)
                )
                .asUser(AuthenticatedUser.Integration)
                .build()

        client.newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(400)
        }
    }

    @Test
    fun `put working time events with titania exception should respond 400`() {
        val request =
            Request.Builder()
                .url("http://localhost:$httpPort/integration/titania/working-time-events")
                .put(
                    jsonMapper
                        .writeValueAsString(titaniaUpdateRequestInvalidExampleData)
                        .toRequestBody(jsonMediaType)
                )
                .asUser(AuthenticatedUser.Integration)
                .build()

        client.newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(400)
            assertEquals(
                """{
    "faultcode": "Server",
    "faultstring": "multiple",
    "faultactor": "/integration/titania/working-time-events",
    "detail": [
        {
            "errorcode": "102",
            "message": "Event date 2011-01-26 is out of period (2011-01-03 - 2011-01-23)"
        }
    ]
}""",
                response.body.string(),
                JSONCompareMode.STRICT,
            )
        }
    }

    @Test
    fun `get stamped working time events with system user should respond 403`() {
        val request =
            Request.Builder()
                .url("http://localhost:$httpPort/integration/titania/stamped-working-time-events")
                .post(
                    readResource("titania/titania-get-request-valid-example-data.json")
                        .toRequestBody(jsonMediaType)
                )
                .asUser(AuthenticatedUser.SystemInternalUser)
                .build()

        client.newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(403)
        }
    }

    @Test
    fun `get stamped working time events without employee id should respond 400`() {
        val request =
            Request.Builder()
                .url("http://localhost:$httpPort/integration/titania/stamped-working-time-events")
                .post(
                    readResource("titania/titania-get-request-without-employee-id.json")
                        .toRequestBody(jsonMediaType)
                )
                .asUser(AuthenticatedUser.Integration)
                .build()

        client.newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(400)
        }
    }

    @Test
    fun `put working time events with unknown employee number should respond 200`() {
        val request =
            Request.Builder()
                .url("http://localhost:$httpPort/integration/titania/working-time-events")
                .put(
                    jsonMapper
                        .writeValueAsString(titaniaUpdateRequestValidExampleData)
                        .toRequestBody(jsonMediaType)
                )
                .asUser(AuthenticatedUser.Integration)
                .build()

        client.newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(200)
        }
    }

    @Test
    fun `put working time events with conflicting shifts should respond 400`() {
        db.transaction { tx -> tx.insert(DevEmployee(employeeNumber = "176716")) }
        val request =
            Request.Builder()
                .url("http://localhost:$httpPort/integration/titania/working-time-events")
                .put(
                    jsonMapper
                        .writeValueAsString(titaniaUpdateRequestConflictingExampleData)
                        .toRequestBody(jsonMediaType)
                )
                .asUser(AuthenticatedUser.Integration)
                .build()

        client.newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(400)
            assertEquals(
                """{
    "faultcode": "Server",
    "faultstring": "multiple",
    "faultactor": "/integration/titania/working-time-events",
    "detail": [
        {
            "errorcode": "103",
            "message": "Conflicting working time events found"
        }
    ]
}""",
                response.body.string(),
                JSONCompareMode.STRICT,
            )
        }
    }
}
