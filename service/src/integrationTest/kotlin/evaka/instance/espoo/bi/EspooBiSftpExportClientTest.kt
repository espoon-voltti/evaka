// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo.bi

import evaka.core.Sensitive
import evaka.core.SftpEnv
import evaka.core.bi.CSV_CHARSET
import evaka.core.bi.CsvInputStream
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.sftp.SftpClient
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
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

class EspooBiSftpExportClientTest {
    private val prefix = "upload/espoo-bi-${UUID.randomUUID()}"
    private val clock = MockEvakaClock(2026, 10, 21, 0, 10)

    @BeforeEach
    fun createPrefix() {
        SftpClient(env).session { it.createDirectoryIfMissing(prefix) }
    }

    @Test
    fun `uploads each table to a file in a folder named by the export date`() {
        val client = EspooBiSftpExportClient(SftpClient(env, prefix))

        client.sendBiCsvFile("first", clock, csv("a\r\n", "1\r\n"))
        // The date folder exists already when the second table is sent
        client.sendBiCsvFile("second", clock, csv("b\r\n", "2\r\n"))

        assertEquals("a\r\n1\r\n", read("$prefix/2026-10-21/first.csv"))
        assertEquals("b\r\n2\r\n", read("$prefix/2026-10-21/second.csv"))
    }

    private fun csv(vararg records: String) = CsvInputStream(CSV_CHARSET, records.asSequence())

    private fun read(path: String) = SftpClient(env).getAsString(path, CSV_CHARSET)
}
