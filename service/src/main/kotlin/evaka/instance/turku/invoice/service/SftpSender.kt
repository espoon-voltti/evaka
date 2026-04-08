// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku.invoice.service

import evaka.instance.turku.SftpProperties

class SftpSender(val sftpProperties: SftpProperties, val sftpConnector: SftpConnector) {
    @Throws(Exception::class)
    fun send(content: String, fileName: String) {
        val path = sftpProperties.path

        val filepath = "$path/$fileName"

        try {
            sftpConnector.connect(
                sftpProperties.address,
                sftpProperties.port,
                sftpProperties.username.value,
                sftpProperties.password.value,
            )

            sftpConnector.send(filepath, content)
        } catch (e: Exception) {
            throw e
        } finally {
            sftpConnector.disconnect()
        }
    }

    @Throws(Exception::class)
    fun sendAll(contents: Map<String, String>) {
        val path = sftpProperties.path

        try {
            sftpConnector.connect(
                sftpProperties.address,
                sftpProperties.port,
                sftpProperties.username.value,
                sftpProperties.password.value,
            )

            contents.forEach {
                val (fileName, content) = it

                val filepath = "$path/$fileName"
                sftpConnector.send(filepath, content)
            }
        } catch (e: Exception) {
            throw e
        } finally {
            sftpConnector.disconnect()
        }
    }
}
