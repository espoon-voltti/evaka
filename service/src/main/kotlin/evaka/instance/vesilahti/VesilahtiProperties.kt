// SPDX-FileCopyrightText: 2023 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.vesilahti

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "vesilahti")
data class VesilahtiProperties(val bucket: BucketProperties)

data class BucketProperties(val export: String) {
    fun allBuckets() = listOf(export)
}
