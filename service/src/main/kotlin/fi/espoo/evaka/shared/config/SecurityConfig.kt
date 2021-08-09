// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.PermittedRoleActions
import fi.espoo.evaka.shared.security.StaticPermittedRoleActions
import org.jdbi.v3.core.Jdbi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SecurityConfig {
    @Bean
    fun accessControlList(jdbi: Jdbi): AccessControlList = AccessControlList(jdbi)

    @Bean
    fun permittedRoleActions(): PermittedRoleActions = StaticPermittedRoleActions()

    @Bean
    fun accessControl(permittedRoleActions: PermittedRoleActions, acl: AccessControlList, jdbi: Jdbi): AccessControl =
        AccessControl(permittedRoleActions, acl, jdbi)
}
