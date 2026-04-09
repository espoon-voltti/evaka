// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka.sftp

import evaka.core.Sensitive
import evaka.core.SftpEnv

data class SftpProperties(
    val host: String,
    val port: Int,
    val hostKeys: List<String>,
    val username: String,
    val password: String?,
    val privateKey: String?,
    val prefix: String,
) {
    fun toSftpEnv(): SftpEnv =
        SftpEnv(
            host = host,
            port = port,
            username = username,
            password = password?.let { Sensitive(it) },
            privateKey = privateKey?.let { Sensitive(it) },
            hostKeys = hostKeys,
        )
}
