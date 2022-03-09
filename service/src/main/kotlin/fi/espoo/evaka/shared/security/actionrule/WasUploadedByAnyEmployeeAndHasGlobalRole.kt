// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.shared.utils.toEnumSet
import org.jdbi.v3.core.kotlin.mapTo
import java.util.EnumSet

data class WasUploadedByAnyEmployeeAndHasGlobalRole(val oneOf: EnumSet<UserRole>) : ActionRuleParams<WasUploadedByAnyEmployeeAndHasGlobalRole> {
    init {
        oneOf.forEach { check(it.isGlobalRole()) { "Expected a global role, got $it" } }
    }
    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet())

    override fun merge(other: WasUploadedByAnyEmployeeAndHasGlobalRole): WasUploadedByAnyEmployeeAndHasGlobalRole = WasUploadedByAnyEmployeeAndHasGlobalRole(
        (this.oneOf.asSequence() + other.oneOf.asSequence()).toEnumSet()
    )

    private object Query : DatabaseActionRule.Query<AttachmentId, WasUploadedByAnyEmployeeAndHasGlobalRole> {
        override fun execute(
            tx: Database.Read,
            user: AuthenticatedUser,
            targets: Set<AttachmentId>
        ): Map<AttachmentId, DatabaseActionRule.Deferred<WasUploadedByAnyEmployeeAndHasGlobalRole>> = when (user) {
            is AuthenticatedUser.Employee -> user.globalRoles
            else -> null
        }?.let { globalRoles ->
            tx.createQuery(
                """
                SELECT id
                FROM attachment
                JOIN evaka_user ON uploaded_by = evaka_user.id
                WHERE id = ANY(:ids)
                AND evaka_user.type = 'EMPLOYEE'
                """.trimIndent()
            )
                .bind("ids", targets.toTypedArray())
                .mapTo<AttachmentId>()
                .associateWith { Deferred(globalRoles) }
        } ?: emptyMap()
    }
    private class Deferred(private val globalRoles: Set<UserRole>) : DatabaseActionRule.Deferred<WasUploadedByAnyEmployeeAndHasGlobalRole> {
        override fun evaluate(params: WasUploadedByAnyEmployeeAndHasGlobalRole): AccessControlDecision = if (globalRoles.any { params.oneOf.contains(it) }) {
            AccessControlDecision.Permitted(params)
        } else {
            AccessControlDecision.None
        }
    }

    fun attachment() = DatabaseActionRule(this, Query)
}
