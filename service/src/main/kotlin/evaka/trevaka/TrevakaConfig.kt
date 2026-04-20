// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka

import evaka.trevaka.frends.dvvModificationRequestCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TrevakaConfig {
    @Bean
    fun basicAuthCustomizer(trevakaProperties: TrevakaProperties) =
        dvvModificationRequestCustomizer(trevakaProperties.vtjMutpaApiKey)
}
