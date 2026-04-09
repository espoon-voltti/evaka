// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.orivesi

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "orivesi")
data class OrivesiProperties(val invoice: InvoiceProperties, val bucket: BucketProperties)

data class InvoiceProperties(val municipalityCode: String, val invoiceType: String)

data class BucketProperties(val export: String) {
    fun allBuckets() = listOf(export)
}
