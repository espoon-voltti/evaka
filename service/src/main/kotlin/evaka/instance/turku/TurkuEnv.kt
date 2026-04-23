// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku

import evaka.core.Sensitive
import evaka.core.lookup
import org.springframework.core.env.Environment

data class TurkuEnv(
    val sapInvoicing: SftpProperties,
    val sapPayments: SftpProperties,
    val dwExport: DwExportProperties,
) {
    companion object {
        fun fromEnvironment(env: Environment) =
            TurkuEnv(
                sapInvoicing = SftpProperties.fromEnvironment(env, "evakaturku.sap_invoicing"),
                sapPayments = SftpProperties.fromEnvironment(env, "evakaturku.sap_payments"),
                dwExport =
                    DwExportProperties(
                        sftp = SftpProperties.fromEnvironment(env, "evakaturku.dw_export.sftp")
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

data class DwExportProperties(val sftp: SftpProperties)
