// SPDX-FileCopyrightText: 2021 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu

import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.lookup
import org.springframework.core.env.Environment

data class OuluEnv(
    val intimeInvoices: SftpProperties,
    val intimePayments: SftpProperties,
    val bucket: BucketProperties,
    val dwExport: DwExportProperties,
) {
    companion object {
        fun fromEnvironment(env: Environment) =
            OuluEnv(
                intimeInvoices = SftpProperties.fromEnvironment(env, "evakaoulu.intime_invoices"),
                intimePayments = SftpProperties.fromEnvironment(env, "evakaoulu.intime_payments"),
                bucket = BucketProperties(export = env.lookup("evakaoulu.bucket.export")),
                dwExport =
                    DwExportProperties(
                        prefix = env.lookup("evakaoulu.dw_export.prefix"),
                        sftp = SftpProperties.fromEnvironment(env, "evakaoulu.dw_export.sftp"),
                    ),
            )
    }
}

data class SftpProperties(
    val address: String,
    val path: String,
    val port: Int,
    val username: Sensitive<String>,
    val password: Sensitive<String>,
) {
    companion object {
        fun fromEnvironment(env: Environment, prefix: String) =
            SftpProperties(
                address = env.lookup("$prefix.address"),
                port = env.lookup<Int?>("$prefix.port") ?: 22,
                path = env.lookup("$prefix.path"),
                username = Sensitive(env.lookup("$prefix.username")),
                password = Sensitive(env.lookup("$prefix.password")),
            )
    }
}

data class BucketProperties(val export: String) {
    fun allBuckets() = listOf(export)
}

data class DwExportProperties(val prefix: String, val sftp: SftpProperties)
