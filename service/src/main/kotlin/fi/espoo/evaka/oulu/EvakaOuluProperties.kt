// SPDX-FileCopyrightText: 2021 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu

import org.springframework.boot.context.properties.ConfigurationProperties

/** All evakaoulu-specific configuration properties. */
@ConfigurationProperties(prefix = "evakaoulu", ignoreUnknownFields = false)
data class EvakaOuluProperties(
    val intimeInvoices: SftpProperties,
    val intimePayments: SftpProperties,
    val bucket: BucketProperties,
    val dwExport: DwExportProperties,
)

data class SftpProperties(
    val address: String,
    val path: String,
    val username: String,
    val password: String,
)

data class BucketProperties(val export: String) {
    fun allBuckets() = listOf(export)
}

data class DwExportProperties(val prefix: String, val sftp: SftpProperties)
