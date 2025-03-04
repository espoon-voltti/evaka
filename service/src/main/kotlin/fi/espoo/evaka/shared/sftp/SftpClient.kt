// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.sftp

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import fi.espoo.evaka.SftpEnv
import java.io.BufferedReader
import java.io.InputStream
import java.nio.charset.Charset

class SftpClient(private val sftpEnv: SftpEnv) {
    fun put(inputStream: InputStream, filename: String) = execute { channel ->
        channel.put(inputStream, filename)
    }

    fun getAsString(filename: String, encoding: Charset): String = execute { channel ->
        channel.get(filename).bufferedReader(encoding).use(BufferedReader::readText)
    }

    private fun <T> execute(callback: (channel: ChannelSftp) -> T): T {
        val jsch = JSch()
        val session = jsch.getSession(sftpEnv.username, sftpEnv.host, sftpEnv.port)
        session.setPassword(sftpEnv.password.value)
        session.setConfig("StrictHostKeyChecking", "no")
        try {
            session.connect()
            val channel = session.openChannel("sftp") as ChannelSftp
            try {
                channel.connect()
                return callback(channel)
            } finally {
                channel.exit()
            }
        } finally {
            session.disconnect()
        }
    }
}
