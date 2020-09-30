// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.auth

import com.auth0.spring.security.api.BearerSecurityContextRepository
import com.auth0.spring.security.api.JwtAuthenticationEntryPoint
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.security.SecurityProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy

@Configuration
@EnableWebSecurity
@ConditionalOnWebApplication
@Order(SecurityProperties.BASIC_AUTH_ORDER)
class VolttiJwtSecurityConfigurerAdapter(
    private val volttiJwtAuthenticationProvider: VolttiJwtAuthenticationProvider
) : WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        http.csrf().disable()
            .httpBasic().disable()
            .exceptionHandling()
            .authenticationEntryPoint(JwtAuthenticationEntryPoint())
            .and()
            .securityContext()
            .securityContextRepository(BearerSecurityContextRepository())
            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authenticationProvider(volttiJwtAuthenticationProvider)
            .authorizeRequests()
            .antMatchers(HttpMethod.GET, "/health", "/actuator/health").permitAll()
            .anyRequest().authenticated()
            .and()
    }
}
