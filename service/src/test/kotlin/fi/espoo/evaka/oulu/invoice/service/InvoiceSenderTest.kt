// SPDX-FileCopyrightText: 2021 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.invoice.service

import fi.espoo.evaka.oulu.SftpProperties
import java.text.SimpleDateFormat
import java.util.Date
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

internal class InvoiceSenderTest {
    @Test
    fun `should send invoice`() {
        val path = "/some/path"
        val sftpProperties = SftpProperties("", path, "", "")
        val proEInvoice = "one"
        val sftpConnector = mock<SftpConnector>()
        val sftpSender = SftpSender(sftpProperties, sftpConnector)

        sftpSender.send(
            proEInvoice,
            SimpleDateFormat("'proe-'yyyyMMdd-hhmmss'.txt'").format(Date()),
        )

        verify(sftpConnector)
            .connect(sftpProperties.address, sftpProperties.username, sftpProperties.password)
        val fileNamePattern = """$path/proe-\d{8}-\d{6}.txt"""
        verify(sftpConnector)
            .send(
                argThat { filePath -> filePath.matches(Regex(fileNamePattern)) },
                eq("one"),
                eq(Charsets.ISO_8859_1),
            )
        verify(sftpConnector).disconnect()
    }
}
