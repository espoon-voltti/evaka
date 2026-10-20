// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.config

import org.springframework.boot.tomcat.TomcatConnectorCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TomcatConfig {
    // Tomcat's default limit of 10 rejects requests from browsers configured with several
    // languages, because region variants (fi-FI, fi, sv-FI, sv, ...) each count separately
    @Bean fun maxLocaleCountCustomizer() = TomcatConnectorCustomizer { it.maxLocaleCount = 100 }
}
