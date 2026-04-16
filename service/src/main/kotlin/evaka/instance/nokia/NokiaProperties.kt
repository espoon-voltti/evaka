// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.nokia

import evaka.instance.tampere.ArchivalSchedule
import evaka.trevaka.primus.PrimusProperties
import evaka.trevaka.sftp.SftpProperties
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "nokia")
data class NokiaProperties(
    val invoice: InvoiceProperties,
    val bucket: BucketProperties,
    val archival: SftpArchivalProperties?,
    val primus: PrimusProperties? = null,
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
