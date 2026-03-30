// SPDX-FileCopyrightText: 2021 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.invoice.service

import com.jcraft.jsch.SftpException
import fi.espoo.evaka.oulu.SftpProperties
import java.nio.charset.Charset

class SftpSender(val sftpProperties: SftpProperties, val sftpConnector: SftpConnector) {
    @Throws(SftpException::class)
    fun send(content: String, fileName: String, encoding: Charset = Charsets.ISO_8859_1) {
        val path = sftpProperties.path
        val filepath = "$path/$fileName"

        try {
            sftpConnector.connect(
                sftpProperties.address,
                sftpProperties.username.value,
                sftpProperties.password.value,
            )

            sftpConnector.send(filepath, content, encoding)
        } catch (e: Exception) {
            throw e
        } finally {
            sftpConnector.disconnect()
        }
    }
}
