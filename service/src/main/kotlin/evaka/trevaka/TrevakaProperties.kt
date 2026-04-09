// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "trevaka")
data class TrevakaProperties(val vtjKyselyApiKey: String, val vtjMutpaApiKey: String)
