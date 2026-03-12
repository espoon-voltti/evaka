// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.invoice.service

import com.jcraft.jsch.JSchException
import fi.espoo.evaka.turku.SftpProperties
import java.text.SimpleDateFormat
import java.util.Date
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

internal class SftpSenderTest {
    private val sftpConnector = mock<SftpConnector>()

    @Test
    fun `should send SAP material with same name`() {
        val path = "/some/path"
        val sftpProperties = SftpProperties("", 22, path, "", "")
        val sapMaterial = "one"
        val filename = SimpleDateFormat("'LAVAK_1002'yyMMdd-hhmmss'.xml'").format(Date())
        val sftpSender = SftpSender(sftpProperties, sftpConnector)

        sftpSender.send(sapMaterial, filename)

        verify(sftpConnector)
            .connect(
                sftpProperties.address,
                sftpProperties.port,
                sftpProperties.username,
                sftpProperties.password,
            )
        val fileNamePattern = """$path/LAVAK_1002\d{6}-\d{6}.xml"""
        verify(sftpConnector)
            .send(argThat { filePath -> filePath.matches(Regex(fileNamePattern)) }, eq("one"))
        verify(sftpConnector).disconnect()
    }

    @Test
    fun `should send multiple materials with same names`() {
        val path = "/some/path"
        val sftpProperties = SftpProperties("", 22, path, "", "")
        val sapMaterials =
            mapOf(
                "${SimpleDateFormat("'OLVAK_1002_0000001_'yyMMdd-hhmmss").format(Date())}-1.xml" to
                    "one",
                "${SimpleDateFormat("'OLVAK_1002_0000001_'yyMMdd-hhmmss").format(Date())}-2.xml" to
                    "two",
                "${SimpleDateFormat("'OLVAK_1002_0000001_'yyMMdd-hhmmss").format(Date())}-3.xml" to
                    "three",
            )
        val sftpSender = SftpSender(sftpProperties, sftpConnector)

        sftpSender.sendAll(sapMaterials)

        verify(sftpConnector)
            .connect(
                sftpProperties.address,
                sftpProperties.port,
                sftpProperties.username,
                sftpProperties.password,
            )
        val fileNamePattern1 = """$path/OLVAK_1002_0000001_\d{6}-\d{6}-1.xml"""
        val fileNamePattern2 = """$path/OLVAK_1002_0000001_\d{6}-\d{6}-2.xml"""
        val fileNamePattern3 = """$path/OLVAK_1002_0000001_\d{6}-\d{6}-3.xml"""
        verify(sftpConnector)
            .send(argThat { filePath -> filePath.matches(Regex(fileNamePattern1)) }, eq("one"))
        verify(sftpConnector)
            .send(argThat { filePath -> filePath.matches(Regex(fileNamePattern2)) }, eq("two"))
        verify(sftpConnector)
            .send(argThat { filePath -> filePath.matches(Regex(fileNamePattern3)) }, eq("three"))
        verify(sftpConnector).disconnect()
    }

    @Test
    fun `should time out`() {
        whenever(sftpConnector.connect(any(), any(), any(), any()))
            .thenThrow(JSchException("test message"))
        val path = "/some/path"
        val sftpProperties = SftpProperties("", 22, path, "", "")
        val sftpSender = SftpSender(sftpProperties, sftpConnector)

        val e = assertThrows<JSchException> { sftpSender.send("foo", "bar") }
        assert(e.message == "test message")

        verify(sftpConnector).disconnect()
    }
}
