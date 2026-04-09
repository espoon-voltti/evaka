// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.nokiankaupunki

import evaka.instance.tampere.ArchivalSchedule
import evaka.trevaka.sftp.SftpProperties
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "nokia")
data class NokiaProperties(
    val invoice: InvoiceProperties,
    val bucket: BucketProperties,
    val archival: SftpArchivalProperties?,
)

data class InvoiceProperties(
    val municipalityCode: String,
    val invoiceType: String,
    val sftp: SftpProperties,
)

data class BucketProperties(val export: String) {
    fun allBuckets() = listOf(export)
}

data class SftpArchivalProperties(
    val schedule: ArchivalSchedule = ArchivalSchedule(),
    val sftp: SftpProperties,
)
