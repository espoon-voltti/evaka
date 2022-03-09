// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.utils.enumSetOf
import java.util.EnumMap
import java.util.EnumSet

/**
 * Role → action mapping
 */
interface PermittedRoleActions {
    fun attachmentActions(role: UserRole): Set<Action.Attachment>
    fun incomeStatementActions(role: UserRole): Set<Action.IncomeStatement>
}

/**
 * Role → action mapping based on static data.
 *
 * Uses system defaults, unless some mappings are overridden using constructor parameters
 */
class StaticPermittedRoleActions(
    val attachment: ActionsByRole<Action.Attachment> = getDefaults(),
    val incomeStatement: ActionsByRole<Action.IncomeStatement> = getDefaults(),
) : PermittedRoleActions {
    override fun attachmentActions(role: UserRole): Set<Action.Attachment> = attachment[role] ?: emptySet()
    override fun incomeStatementActions(role: UserRole): Set<Action.IncomeStatement> = incomeStatement[role] ?: emptySet()
}

typealias ActionsByRole<A> = Map<UserRole, Set<A>>
typealias RolesByAction<A> = Map<A, Set<UserRole>>

private inline fun <reified A> RolesByAction<A>.invert(): ActionsByRole<A> where A : Action, A : Enum<A> {
    val result = EnumMap<UserRole, EnumSet<A>>(UserRole::class.java)
    this.entries.forEach { (action, roles) ->
        roles.forEach { role ->
            result[role] = EnumSet.copyOf(result[role] ?: enumSetOf()).apply {
                add(action)
            }
        }
    }
    return result
}

private inline fun <reified A> getDefaults() where A : Action.LegacyAction, A : Enum<A> =
    enumValues<A>().associateWith { it.defaultRoles() }.invert()
