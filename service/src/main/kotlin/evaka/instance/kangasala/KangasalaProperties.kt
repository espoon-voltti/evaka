// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.kangasala

import evaka.instance.nokia.SftpArchivalProperties
import evaka.trevaka.primus.PrimusProperties
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kangasala")
data class KangasalaProperties(
    val bucket: BucketProperties,
    val archival: SftpArchivalProperties? = null,
    val primus: PrimusProperties? = null,
)

data class BucketProperties(val export: String) {
    fun allBuckets() = listOf(export)
}
