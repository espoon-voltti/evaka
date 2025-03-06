// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.sftp

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.UserInfo
import fi.espoo.evaka.SftpEnv
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedReader
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*

private val logger = KotlinLogging.logger {}

class SftpClient(private val sftpEnv: SftpEnv) {
    fun put(inputStream: InputStream, filename: String) = execute { channel ->
        logger.info { "Uploading $filename to ${sftpEnv.host}:${sftpEnv.port}" }
        channel.put(inputStream, filename)
    }

    fun getAsString(filename: String, encoding: Charset): String = execute { channel ->
        logger.info { "Downloading $filename from ${sftpEnv.host}:${sftpEnv.port}" }
        channel.get(filename).bufferedReader(encoding).use(BufferedReader::readText)
    }

    private fun <T> execute(callback: (channel: ChannelSftp) -> T): T {
        val jsch =
            JSch().apply {
                hostKeyRepository =
                    ReadOnlyHostKeyRepository(
                        sftpEnv.hostKeys.map {
                            HostKey(
                                "[${sftpEnv.host}]:${sftpEnv.port}",
                                HostKey.GUESS,
                                Base64.getDecoder().decode(it),
                            )
                        }
                    )
            }
        val session = jsch.getSession(sftpEnv.username, sftpEnv.host, sftpEnv.port)
        session.setPassword(sftpEnv.password.value)
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

private class ReadOnlyHostKeyRepository(private val hostKeys: List<HostKey>) : HostKeyRepository {
    override fun check(host: String?, key: ByteArray?): Int {
        if (host == null) {
            return HostKeyRepository.NOT_INCLUDED
        }
        val hostKey = HostKey(host, HostKey.GUESS, key)
        return if (
            hostKeys.any {
                it.host == hostKey.host && it.type == hostKey.type && it.key == hostKey.key
            }
        )
            HostKeyRepository.OK
        else HostKeyRepository.NOT_INCLUDED
    }

    override fun add(hostkey: HostKey?, ui: UserInfo?) =
        throw UnsupportedOperationException("Use constructor")

    override fun remove(host: String?, type: String?) =
        throw UnsupportedOperationException("Use constructor")

    override fun remove(host: String?, type: String?, key: ByteArray?) =
        throw UnsupportedOperationException("Use constructor")

    override fun getKnownHostsRepositoryID(): String? = null

    override fun getHostKey(): Array<HostKey> = hostKeys.toTypedArray()

    override fun getHostKey(host: String?, type: String?): Array<HostKey> =
        hostKeys.filter { it.host == host && (type == null || it.type == type) }.toTypedArray()
}
