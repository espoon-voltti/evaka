// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.sftp

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.UserInfo
import evaka.core.SftpEnv
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*

private val logger = KotlinLogging.logger {}

class SftpClient(private val sftpEnv: SftpEnv, basePath: String = "") {
    private val basePath = basePath.trim('/')

    fun put(inputStream: InputStream, filename: String) = session { it.put(inputStream, filename) }

    fun put(filename: String, write: (OutputStream) -> Unit) = session { it.put(filename, write) }

    fun getAsString(filename: String, encoding: Charset): String = session {
        it.getAsString(filename, encoding)
    }

    fun <T> session(block: (SftpSession) -> T): T = execute { channel ->
        block(ChannelSftpSession(sftpEnv, channel, basePath))
    }

    private fun <T> execute(callback: (channel: ChannelSftp) -> T): T {
        val hostKeyAlias = "${sftpEnv.host}:${sftpEnv.port}"
        val jsch =
            JSch().apply {
                if (!sftpEnv.skipHostKeyVerification) {
                    hostKeyRepository =
                        ReadOnlyHostKeyRepository(
                            sftpEnv.hostKeys.map {
                                HostKey(hostKeyAlias, HostKey.GUESS, Base64.getDecoder().decode(it))
                            }
                        )
                }
            }
        if (sftpEnv.privateKey != null) {
            jsch.addIdentity(
                sftpEnv.username,
                sftpEnv.privateKey.value.toByteArray(Charsets.UTF_8),
                null,
                null,
            )
        }
        val session = jsch.getSession(sftpEnv.username, sftpEnv.host, sftpEnv.port)
        if (sftpEnv.password != null) {
            session.setPassword(sftpEnv.password.value.toByteArray(Charsets.UTF_8))
        }
        session.hostKeyAlias = hostKeyAlias
        if (sftpEnv.skipHostKeyVerification) {
            session.setConfig("StrictHostKeyChecking", "no")
            logger.warn {
                "Connecting to ${sftpEnv.host}:${sftpEnv.port} with host key verification disabled"
            }
        }
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

interface SftpSession {
    fun put(inputStream: InputStream, filename: String)

    fun put(filename: String, write: (OutputStream) -> Unit)

    fun getAsString(filename: String, encoding: Charset): String
}

private class ChannelSftpSession(
    private val sftpEnv: SftpEnv,
    private val channel: ChannelSftp,
    private val basePath: String,
) : SftpSession {
    override fun put(inputStream: InputStream, filename: String) {
        val target = resolve(filename)
        logger.info { "Uploading $target to ${sftpEnv.host}:${sftpEnv.port}" }
        channel.put(inputStream, target)
    }

    override fun put(filename: String, write: (OutputStream) -> Unit) {
        val target = resolve(filename)
        logger.info { "Uploading $target to ${sftpEnv.host}:${sftpEnv.port}" }
        channel.put(target).use(write)
    }

    override fun getAsString(filename: String, encoding: Charset): String {
        val target = resolve(filename)
        logger.info { "Downloading $target from ${sftpEnv.host}:${sftpEnv.port}" }
        return channel.get(target).bufferedReader(encoding).use(BufferedReader::readText)
    }

    private fun resolve(filename: String): String =
        if (basePath.isEmpty()) filename else "$basePath/${filename.trimStart('/')}"
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
