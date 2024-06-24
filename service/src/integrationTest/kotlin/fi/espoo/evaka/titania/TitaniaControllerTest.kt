// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.titania

import com.github.kittinunf.fuel.core.extensions.jsonBody
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import java.nio.charset.StandardCharsets
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert.assertEquals
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.core.io.ClassPathResource
import org.springframework.util.StreamUtils

@ExtendWith(OutputCaptureExtension::class)
internal class TitaniaControllerTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Test
    fun `put working time events with system user should respond 403`() {
        val (_, res, _) =
            http
                .put("/integration/titania/working-time-events")
                .asUser(AuthenticatedUser.SystemInternalUser)
                .jsonBody(
                    ClassPathResource("titania/titania-update-request-valid-example-data.json")
                        .inputStream
                        .use { StreamUtils.copyToString(it, StandardCharsets.UTF_8) }
                ).response()

        assertThat(res).returns(403) { it.statusCode }
    }

    @Test
    fun `put working time events without employee id should respond 400`() {
        val (_, res, _) =
            http
                .put("/integration/titania/working-time-events")
                .asUser(AuthenticatedUser.Integration)
                .jsonBody(
                    ClassPathResource("titania/titania-update-request-without-employee-id.json")
                        .inputStream
                        .use { StreamUtils.copyToString(it, StandardCharsets.UTF_8) }
                ).response()

        assertThat(res).returns(400) { it.statusCode }
    }

    @Test
    fun `put working time events with titania exception should respond 400`() {
        val (_, res, _) =
            http
                .put("/integration/titania/working-time-events")
                .asUser(AuthenticatedUser.Integration)
                .jsonBody(jsonMapper.writeValueAsString(titaniaUpdateRequestInvalidExampleData))
                .response()

        assertThat(res).returns(400) { it.statusCode }
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
            res.body().asString("application/json"),
            JSONCompareMode.STRICT
        )
    }

    @Test
    fun `get stamped working time events with system user should respond 403`() {
        val (_, res, _) =
            http
                .post("/integration/titania/stamped-working-time-events")
                .asUser(AuthenticatedUser.SystemInternalUser)
                .jsonBody(
                    ClassPathResource("titania/titania-get-request-valid-example-data.json")
                        .inputStream
                        .use { StreamUtils.copyToString(it, StandardCharsets.UTF_8) }
                ).response()

        assertThat(res).returns(403) { it.statusCode }
    }

    @Test
    fun `get stamped working time events without employee id should respond 400`() {
        val (_, res, _) =
            http
                .post("/integration/titania/stamped-working-time-events")
                .asUser(AuthenticatedUser.Integration)
                .jsonBody(
                    ClassPathResource("titania/titania-get-request-without-employee-id.json")
                        .inputStream
                        .use { StreamUtils.copyToString(it, StandardCharsets.UTF_8) }
                ).response()

        assertThat(res).returns(400) { it.statusCode }
    }

    @Test
    fun `should log created employees`(capturedOutput: CapturedOutput) {
        http
            .put("/integration/titania/working-time-events")
            .asUser(AuthenticatedUser.Integration)
            .jsonBody(jsonMapper.writeValueAsString(titaniaUpdateRequestValidExampleData))
            .response()

        assertThat(capturedOutput).contains("\"eventCode\":\"EmployeeCreate\"")
    }
}
