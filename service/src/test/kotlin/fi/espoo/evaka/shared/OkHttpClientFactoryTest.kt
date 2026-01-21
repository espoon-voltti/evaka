// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class OkHttpClientFactoryTest {

    @Test
    fun `builder creates a client with custom timeouts`() {
        val client =
            buildHttpClient(
                    timeouts =
                        TimeoutConfig(
                            connectTimeout = Duration.ofSeconds(10),
                            readTimeout = Duration.ofSeconds(20),
                            writeTimeout = Duration.ofSeconds(15),
                            callTimeout = Duration.ofMinutes(1),
                        )
                )
                .client

        assertEquals(10_000, client.connectTimeoutMillis)
        assertEquals(20_000, client.readTimeoutMillis)
        assertEquals(15_000, client.writeTimeoutMillis)
        assertEquals(60_000, client.callTimeoutMillis)
    }

    @Test
    fun `builder can add multiple interceptors`() {
        val interceptor1 = okhttp3.Interceptor { chain -> chain.proceed(chain.request()) }
        val interceptor2 = okhttp3.Interceptor { chain -> chain.proceed(chain.request()) }

        val client = buildHttpClient(interceptors = listOf(interceptor1, interceptor2)).client

        // Verify both interceptors were added
        assertTrue(client.interceptors.contains(interceptor1))
        assertTrue(client.interceptors.contains(interceptor2))
        assertEquals(2, client.interceptors.size)
    }
}
