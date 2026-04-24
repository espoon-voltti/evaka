// SPDX-FileCopyrightText: 2026 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu.bi

import evaka.core.Sensitive
import evaka.core.SftpEnv
import evaka.core.bi.CsvInputStream
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.sftp.SftpClient
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
    fun `zips and uploads a CSV to the remote path`() {
        val client = OuluBiSftpExportClient(SftpClient(env), "upload/")
        val clock = MockEvakaClock(2026, 4, 24, 12, 0)
        val csv = sequenceOf("col\r\n", "value\r\n")

        val (returnedPath, returnedName) =
            client.sendBiCsvFile("foo", clock, CsvInputStream(StandardCharsets.UTF_8, csv))

        assertEquals("upload/", returnedPath)
        assertEquals("foo_2026-04-24.zip", returnedName)

        val bytes =
            SftpClient(env)
                .getAsString("upload/foo_2026-04-24.zip", StandardCharsets.ISO_8859_1)
                .toByteArray(StandardCharsets.ISO_8859_1)

        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            val entry = zip.nextEntry
            assertNotNull(entry)
            assertEquals("foo.csv", entry.name)
            val content = zip.readBytes().toString(StandardCharsets.UTF_8)
            assertEquals("col\r\nvalue\r\n", content)
        }
    }
}
