// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.shared.utils.toEnumSet
import org.jdbi.v3.core.kotlin.mapTo
import java.util.EnumSet

data class SsnAddingEnabledAndHasGlobalRole(val oneOf: EnumSet<UserRole>) : ActionRuleParams<SsnAddingEnabledAndHasGlobalRole> {
    init {
        oneOf.forEach { check(it.isGlobalRole()) { "Expected a global role, got $it" } }
    }
    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet())

    override fun merge(other: SsnAddingEnabledAndHasGlobalRole): SsnAddingEnabledAndHasGlobalRole = SsnAddingEnabledAndHasGlobalRole(
        (this.oneOf.asSequence() + other.oneOf.asSequence()).toEnumSet()
    )

    private object Query : DatabaseActionRule.Query<PersonId, SsnAddingEnabledAndHasGlobalRole> {
        override fun execute(
            tx: Database.Read,
            user: AuthenticatedUser,
            targets: Set<PersonId>
        ): Map<PersonId, DatabaseActionRule.Deferred<SsnAddingEnabledAndHasGlobalRole>> = when (user) {
            is AuthenticatedUser.Employee -> user.globalRoles
            else -> null
        }?.let { globalRoles ->
            tx.createQuery("SELECT id FROM person WHERE id = ANY(:ids) AND ssn_adding_disabled IS FALSE")
                .bind("ids", targets.toTypedArray())
                .mapTo<PersonId>()
                .associateWith { Deferred(globalRoles) }
        } ?: emptyMap()
    }
    private class Deferred(private val globalRoles: Set<UserRole>) : DatabaseActionRule.Deferred<SsnAddingEnabledAndHasGlobalRole> {
        override fun evaluate(params: SsnAddingEnabledAndHasGlobalRole): AccessControlDecision = if (globalRoles.any { params.oneOf.contains(it) }) {
            AccessControlDecision.Permitted(params)
        } else {
            AccessControlDecision.None
        }
    }

    fun person() = DatabaseActionRule(this, Query)
}
