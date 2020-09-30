// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.web.context.SecurityContextPersistenceFilter

@TestConfiguration
@Import(AuthenticatedUserSpringSupport::class)
class AuthenticatedUserMockMvc {
    @Bean
    fun securityContextPersistenceFilter() = SecurityContextPersistenceFilter()
}
