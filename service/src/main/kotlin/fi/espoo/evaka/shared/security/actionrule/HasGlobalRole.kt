// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.utils.toEnumSet
import java.util.EnumSet

data class HasGlobalRole(val oneOf: EnumSet<UserRole>) : StaticActionRule, ActionRuleParams<HasGlobalRole> {
    init {
        assert(oneOf.all { it.isGlobalRole() })
    }
    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet())

    override fun isPermitted(user: AuthenticatedUser): Boolean =
        user is AuthenticatedUser.Employee && user.globalRoles.any { this.oneOf.contains(it) }

    override fun merge(other: HasGlobalRole): HasGlobalRole = HasGlobalRole(
        (this.oneOf.asSequence() + other.oneOf.asSequence()).toEnumSet()
    )
}
