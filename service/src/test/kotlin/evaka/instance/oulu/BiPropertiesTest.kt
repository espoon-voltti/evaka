// SPDX-FileCopyrightText: 2026 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.springframework.mock.env.MockEnvironment

class BiPropertiesTest {
    private fun env(vararg overrides: Pair<String, String>): MockEnvironment {
        val base =
            mapOf(
                "evakaoulu.bi.sftp.host" to "localhost",
                "evakaoulu.bi.sftp.host_keys[0]" to "AAAA",
                "evakaoulu.bi.sftp.username" to "user",
                "evakaoulu.bi.remote_path" to "upload/",
            )
        return MockEnvironment().apply {
            (base + overrides).forEach { (k, v) -> setProperty(k, v) }
        }
    }

    @Test
    fun `remote_path without trailing slash is normalized`() {
        val props =
            BiProperties.fromEnvironment(
                env("evakaoulu.bi.sftp.password" to "pw", "evakaoulu.bi.remote_path" to "upload")
            )
        assertEquals("upload/", props.remotePath)
    }

    @Test
    fun `remote_path with trailing slash is unchanged`() {
        val props = BiProperties.fromEnvironment(env("evakaoulu.bi.sftp.password" to "pw"))
        assertEquals("upload/", props.remotePath)
    }

    @Test
    fun `empty remote_path is normalized to root`() {
        val props =
            BiProperties.fromEnvironment(
                env("evakaoulu.bi.sftp.password" to "pw", "evakaoulu.bi.remote_path" to "")
            )
        assertEquals("/", props.remotePath)
    }

    @Test
    fun `both password and private_key fail the SftpEnv credentials invariant`() {
        assertFailsWith<IllegalStateException> {
            BiProperties.fromEnvironment(
                env("evakaoulu.bi.sftp.password" to "pw", "evakaoulu.bi.sftp.private_key" to "key")
            )
        }
    }

    @Test
    fun `neither password nor private_key fail the SftpEnv credentials invariant`() {
        assertFailsWith<IllegalStateException> { BiProperties.fromEnvironment(env()) }
    }

    @Test
    fun `windowMonths defaults to 3 when omitted`() {
        val props = BiProperties.fromEnvironment(env("evakaoulu.bi.sftp.password" to "pw"))
        assertEquals(3, props.windowMonths)
    }

    @Test
    fun `excludedTables defaults to empty set when omitted`() {
        val props = BiProperties.fromEnvironment(env("evakaoulu.bi.sftp.password" to "pw"))
        assertEquals(emptySet(), props.excludedTables)
    }
}
