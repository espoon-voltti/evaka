// SPDX-FileCopyrightText: 2023-2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.hameenkyro

import evaka.trevaka.primus.PrimusProperties
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "hameenkyro")
data class HameenkyroProperties(val bucket: BucketProperties, val primus: PrimusProperties? = null)

data class BucketProperties(val export: String) {
    fun allBuckets() = listOf(export)
}
