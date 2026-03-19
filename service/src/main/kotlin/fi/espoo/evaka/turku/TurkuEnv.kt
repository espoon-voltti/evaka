// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku

import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.lookup
import org.springframework.core.env.Environment

data class TurkuEnv(
    val sapInvoicing: SftpProperties,
    val sapPayments: SftpProperties,
    val bucket: BucketProperties,
    val dwExport: DwExportProperties,
) {
    companion object {
        fun fromEnvironment(env: Environment) =
            TurkuEnv(
                sapInvoicing = SftpProperties.fromEnvironment(env, "evakaturku.sap_invoicing"),
                sapPayments = SftpProperties.fromEnvironment(env, "evakaturku.sap_payments"),
                bucket = BucketProperties(export = env.lookup("evakaturku.bucket.export")),
                dwExport =
                    DwExportProperties(
                        prefix = env.lookup("evakaturku.dw_export.prefix"),
                        sftp = SftpProperties.fromEnvironment(env, "evakaturku.dw_export.sftp"),
                    ),
            )
    }
}

data class SftpProperties(
    val address: String,
    val port: Int,
    val path: String,
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
