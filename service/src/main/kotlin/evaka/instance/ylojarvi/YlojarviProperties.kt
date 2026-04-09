// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.ylojarvi

import evaka.trevaka.sftp.SftpProperties
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ylojarvi")
data class YlojarviProperties(val invoice: InvoiceProperties, val bucket: BucketProperties)

data class InvoiceProperties(
    val municipalityCode: String,
    val invoiceType: String,
    val sftp: SftpProperties,
)

data class BucketProperties(val export: String) {
    fun allBuckets() = listOf(export)
}
