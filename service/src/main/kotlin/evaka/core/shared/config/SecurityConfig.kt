// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.config

import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.actionrule.ActionRuleMapping
import io.opentelemetry.api.trace.Tracer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SecurityConfig {
    @Bean
    fun accessControl(actionRuleMapping: ActionRuleMapping, tracer: Tracer): AccessControl =
        AccessControl(actionRuleMapping, tracer)
}
