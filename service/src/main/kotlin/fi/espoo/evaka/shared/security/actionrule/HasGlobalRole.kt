// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.assistanceneed.decision.filterPermittedAssistanceNeedDecisionsForDecisionMaker
import fi.espoo.evaka.assistanceneed.decision.filterSentAssistanceNeedDecisions
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.shared.utils.toEnumSet
import java.util.EnumSet

private typealias Filter<T> = (tx: Database.Read, user: AuthenticatedUser.Employee, now: HelsinkiDateTime, targets: Set<T>) -> Iterable<T>

data class HasGlobalRole(val oneOf: EnumSet<UserRole>) : StaticActionRule {
    init {
        oneOf.forEach { check(it.isGlobalRole()) { "Expected a global role, got $it" } }
    }
    constructor(vararg oneOf: UserRole) : this(oneOf.toEnumSet())

    override fun isPermitted(user: AuthenticatedUser): Boolean =
        user is AuthenticatedUser.Employee && user.globalRoles.any { this.oneOf.contains(it) }

    private fun <T> rule(filter: Filter<T>): DatabaseActionRule.Scoped<T, HasGlobalRole> =
        DatabaseActionRule.Scoped.Simple(this, Query(filter))
    data class Query<T>(private val filter: Filter<T>) : DatabaseActionRule.Scoped.Query<T, HasGlobalRole> {
        override fun executeWithTargets(
            ctx: DatabaseActionRule.QueryContext,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<HasGlobalRole>> = when (ctx.user) {
            is AuthenticatedUser.Employee -> filter(ctx.tx, ctx.user, ctx.now, targets).associateWith { Deferred(ctx.user.globalRoles) }
            else -> emptyMap()
        }

        override fun executeWithParams(
            ctx: DatabaseActionRule.QueryContext,
            params: HasGlobalRole
        ): AccessControlFilter<T>? = TODO("unsupported for this rule type")
    }
    private class Deferred(private val globalRoles: Set<UserRole>) : DatabaseActionRule.Deferred<HasGlobalRole> {
        override fun evaluate(params: HasGlobalRole): AccessControlDecision = if (globalRoles.any { params.oneOf.contains(it) }) {
            AccessControlDecision.Permitted(params)
        } else {
            AccessControlDecision.None
        }
    }

    fun andAttachmentWasUploadedByAnyEmployee() = rule<AttachmentId> { tx, _, _, ids ->
        tx.createQuery(
            """
SELECT attachment.id
FROM attachment
JOIN evaka_user ON uploaded_by = evaka_user.id
WHERE attachment.id = ANY(:ids)
AND evaka_user.type = 'EMPLOYEE'
            """.trimIndent()
        )
            .bind("ids", ids)
            .mapTo()
    }

    fun andIsDecisionMakerForAssistanceNeedDecision() = rule { tx, employee, _, ids ->
        tx.filterPermittedAssistanceNeedDecisionsForDecisionMaker(
            employee,
            ids
        )
    }

    fun andAssistanceNeedDecisionHasBeenSent() = rule { tx, _, _, ids ->
        tx.filterSentAssistanceNeedDecisions(ids)
    }

    fun andChildHasServiceVoucherPlacement() = rule<ChildId> { tx, _, _, ids ->
        tx.createQuery(
            """
SELECT p.child_id
FROM placement p
JOIN daycare pd ON pd.id = p.unit_id
WHERE p.child_id = ANY(:ids)
  AND pd.provider_type = 'PRIVATE_SERVICE_VOUCHER'
            """.trimIndent()
        )
            .bind("ids", ids)
            .mapTo()
    }
}
