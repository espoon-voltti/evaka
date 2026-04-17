// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.lempaala

import evaka.trevaka.primus.PrimusProperties
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "lempaala")
data class LempaalaProperties(
    val invoice: InvoiceProperties,
    val bucket: BucketProperties,
    val primus: PrimusProperties? = null,
)

data class InvoiceProperties(val organizationCode: String, val invoiceType: String)

data class BucketProperties(val export: String) {
    fun allBuckets() = listOf(export)
}
