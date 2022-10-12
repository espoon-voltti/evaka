// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.DailyServiceTimeNotificationId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision

private typealias FilterByCitizen<T> = QuerySql.Builder<T>.(personId: PersonId, now: HelsinkiDateTime) -> QuerySql<T>

data class IsCitizen(val allowWeakLogin: Boolean) {
    fun isPermittedAuthLevel(authLevel: CitizenAuthLevel) = authLevel == CitizenAuthLevel.STRONG || allowWeakLogin

    private fun <T : Id<*>> rule(filter: FilterByCitizen<T>): DatabaseActionRule.Scoped<T, IsCitizen> =
        DatabaseActionRule.Scoped.Simple(this, Query(filter))
    private data class Query<T : Id<*>>(private val filter: FilterByCitizen<T>) : DatabaseActionRule.Scoped.Query<T, IsCitizen> {
        override fun executeWithTargets(
            ctx: DatabaseActionRule.QueryContext,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<IsCitizen>> = when (ctx.user) {
            is AuthenticatedUser.Citizen -> ctx.tx.createQuery<T> {
                sql(
                    """
                        SELECT id
                        FROM (${subquery { filter(ctx.user.id, ctx.now) } }) fragment
                        WHERE id = ANY(${bind(targets.map { it.raw })})
                    """.trimIndent()
                )
            }
                .mapTo<Id<DatabaseTable>>()
                .toSet()
                .let { matched ->
                    targets.filter { matched.contains(it) }.associateWith { Deferred(ctx.user.authLevel) }
                }
            else -> emptyMap()
        }

        override fun queryWithParams(
            ctx: DatabaseActionRule.QueryContext,
            params: IsCitizen
        ): QuerySql<T>? = when (ctx.user) {
            is AuthenticatedUser.Citizen -> if (params.isPermittedAuthLevel(ctx.user.authLevel)) {
                QuerySql.of { filter(ctx.user.id, ctx.now) }
            } else {
                null
            }
            else -> null
        }
    }
    private class Deferred(private val authLevel: CitizenAuthLevel) : DatabaseActionRule.Deferred<IsCitizen> {
        override fun evaluate(params: IsCitizen): AccessControlDecision =
            if (params.isPermittedAuthLevel(authLevel)) {
                AccessControlDecision.Permitted(params)
            } else {
                AccessControlDecision.None
            }
    }

    fun any() = object : StaticActionRule {
        override fun evaluate(user: AuthenticatedUser): AccessControlDecision =
            if (user is AuthenticatedUser.Citizen && isPermittedAuthLevel(user.authLevel)) {
                AccessControlDecision.Permitted(this)
            } else {
                AccessControlDecision.None
            }
    }

    fun self() = object : DatabaseActionRule.Scoped<PersonId, IsCitizen> {
        override val params = this@IsCitizen
        override val query = object : DatabaseActionRule.Scoped.Query<PersonId, IsCitizen> {
            override fun executeWithTargets(
                ctx: DatabaseActionRule.QueryContext,
                targets: Set<PersonId>
            ): Map<PersonId, DatabaseActionRule.Deferred<IsCitizen>> = when (ctx.user) {
                is AuthenticatedUser.Citizen -> targets.filter { it == ctx.user.id }.associateWith { Deferred(ctx.user.authLevel) }
                else -> emptyMap()
            }

            override fun queryWithParams(
                ctx: DatabaseActionRule.QueryContext,
                params: IsCitizen
            ): QuerySql<PersonId>? = when (ctx.user) {
                is AuthenticatedUser.Citizen -> QuerySql.of { sql("SELECT ${bind(ctx.user.id)} AS id") }
                else -> null
            }

            override fun equals(other: Any?): Boolean = other?.javaClass == this.javaClass
            override fun hashCode(): Int = this.javaClass.hashCode()
        }
    }

    fun uploaderOfAttachment() = rule<AttachmentId> { personId, _ ->
        sql(
            """
SELECT id
FROM attachment
WHERE uploaded_by = ${bind(personId)}
            """.trimIndent()
        )
    }

    fun guardianOfChild() = rule<ChildId> { guardianId, _ ->
        sql(
            """
SELECT child_id AS id
FROM guardian
WHERE guardian_id = ${bind(guardianId)}
            """.trimIndent()
        )
    }

    fun fosterParentOfChild() = rule<ChildId> { userId, now ->
        sql(
            """
SELECT child_id AS id
FROM foster_parent
WHERE parent_id = ${bind(userId)} AND valid_during @> ${bind(now.toLocalDate())}
            """.trimIndent()
        )
    }

    fun guardianOfChildOfChildImage() = rule<ChildImageId> { guardianId, _ ->
        sql(
            """
SELECT img.id
FROM child_images img
JOIN person child ON img.child_id = child.id
JOIN guardian ON child.id = guardian.child_id
WHERE guardian_id = ${bind(guardianId)}
            """.trimIndent()
        )
    }

    fun guardianOfChildOfIncomeStatement() = rule<IncomeStatementId> { citizenId, _ ->
        sql(
            """
SELECT id
FROM income_statement i
JOIN guardian g ON i.person_id = g.child_id
WHERE g.guardian_id = ${bind(citizenId)}
            """.trimIndent()
        )
    }

    fun guardianOfChildOfPedagogicalDocument() = rule<PedagogicalDocumentId> { guardianId, _ ->
        sql(
            """
SELECT pd.id
FROM pedagogical_document pd
JOIN guardian g ON pd.child_id = g.child_id
WHERE g.guardian_id = ${bind(guardianId)}
            """.trimIndent()
        )
    }

    fun fosterParentOfChildOfPedagogicalDocument() = rule<PedagogicalDocumentId> { userId, now ->
        sql(
            """
SELECT pd.id
FROM pedagogical_document pd
JOIN foster_parent fp ON pd.child_id = fp.child_id
WHERE fp.parent_id = ${bind(userId)} AND fp.valid_during @> ${bind(now.toLocalDate())}
            """.trimIndent()
        )
    }

    fun guardianOfChildOfPedagogicalDocumentOfAttachment() = rule<AttachmentId> { guardianId, _ ->
        sql(
            """
SELECT a.id
FROM attachment a
JOIN pedagogical_document pd ON a.pedagogical_document_id = pd.id
JOIN guardian g ON pd.child_id = g.child_id
WHERE g.guardian_id = ${bind(guardianId)}
            """.trimIndent()
        )
    }

    fun fosterParentOfChildOfPedagogicalDocumentOfAttachment() = rule<AttachmentId> { userId, now ->
        sql(
            """
SELECT a.id
FROM attachment a
JOIN pedagogical_document pd ON a.pedagogical_document_id = pd.id
JOIN foster_parent fp ON pd.child_id = fp.child_id
WHERE fp.parent_id = ${bind(userId)} AND fp.valid_during @> ${bind(now.toLocalDate())}
            """.trimIndent()
        )
    }

    fun guardianOfChildOfVasu() = rule<VasuDocumentId> { citizenId, _ ->
        sql(
            """
SELECT id
FROM curriculum_document cd
JOIN guardian g ON cd.child_id = g.child_id
WHERE g.guardian_id = ${bind(citizenId)}
            """.trimIndent()
        )
    }

    fun fosterParentOfChildOfVasu() = rule<VasuDocumentId> { citizenId, now ->
        sql(
            """
SELECT cd.id
FROM curriculum_document cd
JOIN foster_parent fp ON cd.child_id = fp.child_id
WHERE fp.parent_id = ${bind(citizenId)} AND fp.valid_during @> ${bind(now.toLocalDate())}
"""
        )
    }

    fun guardianOfChildOfPlacement() = rule<PlacementId> { guardianId, _ ->
        sql(
            """
SELECT placement.id
FROM placement
JOIN guardian ON placement.child_id = guardian.child_id
WHERE guardian_id = ${bind(guardianId)}
            """.trimIndent()
        )
    }

    fun fosterParentOfChildOfPlacement() = rule<PlacementId> { userId, now ->
        sql(
            """
SELECT placement.id
FROM placement
JOIN foster_parent fp ON placement.child_id = fp.child_id AND fp.valid_during @> ${bind(now.toLocalDate())}
WHERE parent_id = ${bind(userId)}
            """.trimIndent()
        )
    }

    fun guardianOfChildOfAssistanceNeedDecision() = rule<AssistanceNeedDecisionId> { citizenId, _ ->
        sql(
            """
SELECT id
FROM assistance_need_decision ad
WHERE EXISTS(SELECT 1 FROM guardian g WHERE g.guardian_id = ${bind(citizenId)} AND g.child_id = ad.child_id)
            """.trimIndent()
        )
    }

    fun fosterParentOfChildOfAssistanceNeedDecision() = rule<AssistanceNeedDecisionId> { citizenId, now ->
        sql(
            """
SELECT id
FROM assistance_need_decision ad
WHERE EXISTS(SELECT 1 FROM foster_parent fp WHERE fp.parent_id = ${bind(citizenId)} AND fp.child_id = ad.child_id AND fp.valid_during @> ${bind(now.toLocalDate())})
            """.trimIndent()
        )
    }

    fun hasPermissionForAttachmentThroughMessageContent() = rule<AttachmentId> { personId, _ ->
        sql(
            """
SELECT att.id
FROM attachment att
JOIN message_content content ON att.message_content_id = content.id
JOIN message msg ON content.id = msg.content_id
JOIN message_recipients rec ON msg.id = rec.message_id
JOIN message_account ma ON ma.id = msg.sender_id OR ma.id = rec.recipient_id
WHERE ma.person_id = ${bind(personId)}
            """.trimIndent()
        )
    }

    fun ownerOfApplication() = rule<ApplicationId> { citizenId, _ ->
        sql(
            """
SELECT id
FROM application
WHERE guardian_id = ${bind(citizenId)}
            """.trimIndent()
        )
    }

    fun ownerOfApplicationOfSentDecision() = rule<DecisionId> { citizenId, _ ->
        sql(
            """
SELECT decision.id
FROM decision
JOIN application ON decision.application_id = application.id
WHERE guardian_id = ${bind(citizenId)}
AND decision.sent_date IS NOT NULL
            """.trimIndent()
        )
    }

    fun ownerOfIncomeStatement() = rule<IncomeStatementId> { citizenId, _ ->
        sql(
            """
SELECT id
FROM income_statement
WHERE person_id = ${bind(citizenId)}
            """.trimIndent()
        )
    }

    fun recipientOfDailyServiceTimeNotification() = rule<DailyServiceTimeNotificationId> { citizenId, _ ->
        sql(
            """
SELECT id
FROM daily_service_time_notification
WHERE guardian_id = ${bind(citizenId)}
            """.trimIndent()
        )
    }
}
