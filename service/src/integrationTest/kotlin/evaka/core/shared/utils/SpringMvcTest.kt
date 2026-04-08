// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.utils

import evaka.core.FullApplicationTest
import evaka.core.shared.EmployeeId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.asUser
import java.util.UUID
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SpringMvcTest : FullApplicationTest(resetDbBeforeEach = false) {
    private val client = OkHttpClient()
    private val employee = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), emptySet())

    @Test
    fun `AuthenticatedUser as a parameter requires authentication`() {
        client
            .newCall(
                Request.Builder()
                    .url("http://localhost:$httpPort/integration-test/require-auth")
                    .asUser(AuthenticatedUser.SystemInternalUser)
                    .build()
            )
            .execute()
            .use { response -> assertThat(response.isSuccessful).isTrue() }

        client
            .newCall(
                Request.Builder()
                    .url("http://localhost:$httpPort/integration-test/require-auth-employee")
                    .asUser(AuthenticatedUser.SystemInternalUser)
                    .build()
            )
            .execute()
            .use { response -> assertThat(response.code).isEqualTo(401) }

        client
            .newCall(
                Request.Builder()
                    .url("http://localhost:$httpPort/integration-test/require-auth")
                    .asUser(employee)
                    .build()
            )
            .execute()
            .use { response -> assertThat(response.isSuccessful).isTrue() }

        client
            .newCall(
                Request.Builder()
                    .url("http://localhost:$httpPort/integration-test/require-auth-employee")
                    .asUser(employee)
                    .build()
            )
            .execute()
            .use { response -> assertThat(response.isSuccessful).isTrue() }
    }
}
