// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.utils.toEnumSet
import java.util.EnumSet

data class HasRoleInAnyUnit(val oneOf: EnumSet<UserRole>) : StaticActionRule, ActionRuleParams<HasRoleInAnyUnit> {
    init {
        oneOf.forEach { check(it.isUnitScopedRole()) { "Expected a unit-scoped role, got $it" } }
    }
    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet())

    override fun isPermitted(user: AuthenticatedUser): Boolean =
        user is AuthenticatedUser.Employee && user.allScopedRoles.any { this.oneOf.contains(it) }

    override fun merge(other: HasRoleInAnyUnit): HasRoleInAnyUnit = HasRoleInAnyUnit(
        (this.oneOf.asSequence() + other.oneOf.asSequence()).toEnumSet()
    )
}
