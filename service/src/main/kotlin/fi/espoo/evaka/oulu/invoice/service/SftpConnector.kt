// SPDX-FileCopyrightText: 2023-2025 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.invoice.service

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import org.springframework.stereotype.Component

@Component
class SftpConnector(val jsch: JSch) {
    var jschSession: Session? = null
    var channelSftp: ChannelSftp? = null

    @Throws(Exception::class)
    fun connect(address: String, username: String, password: String) {
        jschSession = jsch.getSession(username, address)
        jschSession?.setConfig("StrictHostKeyChecking", "no")
        jschSession?.setPassword(password)
        jschSession?.connect()

        channelSftp = jschSession?.openChannel("sftp") as ChannelSftp
        channelSftp?.connect()
    }

    @Throws(Exception::class)
    fun send(filePath: String, proEInvoice: String, encoding: Charset) {
        channelSftp?.put(ByteArrayInputStream(proEInvoice.toByteArray(encoding)), filePath)
    }

    fun disconnect() {
        channelSftp?.disconnect()
        jschSession?.disconnect()
    }
}
