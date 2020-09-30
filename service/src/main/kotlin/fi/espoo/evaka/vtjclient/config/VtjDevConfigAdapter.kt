// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@Configuration
@Profile("vtj-dev")
class VtjDevConfigAdapter : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http.requestMatchers()
            .antMatchers("/vtj/identity/*")
            .antMatchers("/actuator/health")
            .and()
            .authorizeRequests()
            .anyRequest().permitAll()
    }
}
