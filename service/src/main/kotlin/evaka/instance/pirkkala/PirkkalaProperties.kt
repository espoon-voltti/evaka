// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.pirkkala

import evaka.instance.nokiankaupunki.SftpArchivalProperties
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "pirkkala")
data class PirkkalaProperties(
    val invoice: InvoiceProperties,
    val bucket: BucketProperties,
    val archival: SftpArchivalProperties? = null,
)

data class InvoiceProperties(val municipalityCode: String, val invoiceType: String)

data class BucketProperties(val export: String) {
    fun allBuckets() = listOf(export)
}
