// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.actionrule.ActionRuleMapping
import io.opentracing.Tracer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SecurityConfig {
    @Bean
    fun accessControl(
        actionRuleMapping: ActionRuleMapping,
        tracer: Tracer
    ): AccessControl = AccessControl(actionRuleMapping, tracer)
}
