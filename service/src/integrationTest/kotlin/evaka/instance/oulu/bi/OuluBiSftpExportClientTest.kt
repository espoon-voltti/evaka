// SPDX-FileCopyrightText: 2026 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu.bi

import evaka.core.Sensitive
import evaka.core.SftpEnv
import evaka.core.bi.CsvInputStream
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.sftp.SftpClient
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

private val sftpPort = System.getenv("EVAKA_SFTP_PORT")?.toIntOrNull() ?: 2222

private val env =
    SftpEnv(
        host = "localhost",
        port = sftpPort,
        hostKeys = listOf("AAAAC3NzaC1lZDI1NTE5AAAAICADdlntyAKbOUGQDkdzdhQBu12jZjb0KmxLyrklMXTq"),
        username = "foo",
        password = Sensitive("pass"),
        privateKey = null,
    )

class OuluBiSftpExportClientTest {

    @Test
    fun `uploads a CSV to the remote path`() {
        val client = OuluBiSftpExportClient(SftpClient(env), "upload/")
        val clock = MockEvakaClock(2026, 4, 24, 12, 0)
        val csv = sequenceOf("col\r\n", "Hämäläinen\r\n")

        client.sendBiCsvFile("foo", clock, CsvInputStream(StandardCharsets.UTF_8, csv))

        val content =
            SftpClient(env).getAsString("upload/foo_2026-04-24.csv", StandardCharsets.UTF_8)
        assertEquals("col\r\nHämäläinen\r\n", content)
    }
}
