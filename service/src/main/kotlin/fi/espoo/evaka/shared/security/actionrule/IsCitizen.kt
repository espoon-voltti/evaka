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
import fi.espoo.evaka.shared.db.QueryFragment
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision

private typealias FilterByCitizen<T> = (personId: PersonId, now: HelsinkiDateTime) -> QueryFragment<T>

data class IsCitizen(val allowWeakLogin: Boolean) {
    fun isPermittedAuthLevel(authLevel: CitizenAuthLevel) = authLevel == CitizenAuthLevel.STRONG || allowWeakLogin

    private fun <T : Id<*>> rule(filter: FilterByCitizen<T>): DatabaseActionRule.Scoped<T, IsCitizen> =
        DatabaseActionRule.Scoped.Simple(this, Query(filter))
    private data class Query<T : Id<*>>(private val filter: FilterByCitizen<T>) : DatabaseActionRule.Scoped.Query<T, IsCitizen> {
        override fun executeWithTargets(
            ctx: DatabaseActionRule.QueryContext,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<IsCitizen>> = when (ctx.user) {
            is AuthenticatedUser.Citizen -> filter(ctx.user.id, ctx.now).let { subquery ->
                ctx.tx.createQuery(
                    QueryFragment<Any>(
                        """
                    SELECT id
                    FROM (${subquery.sql}) fragment
                    WHERE id = ANY(:ids)
                        """.trimIndent(),
                        subquery.bindings
                    )
                )
                    .bind("ids", targets.map { it.raw })
                    .mapTo<Id<DatabaseTable>>()
                    .toSet()
            }.let { matched ->
                targets.filter { matched.contains(it) }.associateWith { Deferred(ctx.user.authLevel) }
            }
            else -> emptyMap()
        }

        override fun executeWithParams(
            ctx: DatabaseActionRule.QueryContext,
            params: IsCitizen
        ): AccessControlFilter<T>? = when (ctx.user) {
            is AuthenticatedUser.Citizen -> if (params.isPermittedAuthLevel(ctx.user.authLevel)) {
                filter(ctx.user.id, ctx.now).let { subquery ->
                    ctx.tx.createQuery(subquery)
                        .mapTo<Id<DatabaseTable>>()
                        .toSet()
                        .let { ids -> AccessControlFilter.Some(ids) }
                }
            } else null
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
            } else AccessControlDecision.None
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

            override fun executeWithParams(
                ctx: DatabaseActionRule.QueryContext,
                params: IsCitizen
            ): AccessControlFilter<PersonId>? = when (ctx.user) {
                is AuthenticatedUser.Citizen -> AccessControlFilter.Some(setOf(ctx.user.id))
                else -> null
            }

            override fun equals(other: Any?): Boolean = other?.javaClass == this.javaClass
            override fun hashCode(): Int = this.javaClass.hashCode()
        }
    }

    fun uploaderOfAttachment() = rule { personId, _ ->
        QueryFragment<AttachmentId>(
            """
SELECT id
FROM attachment
WHERE uploaded_by = :personId
            """.trimIndent()
        )
            .bind("personId", personId)
    }

    fun guardianOfChild() = rule { guardianId, _ ->
        QueryFragment<ChildId>(
            """
SELECT child_id AS id
FROM guardian
WHERE guardian_id = :guardianId
            """.trimIndent()
        )
            .bind("guardianId", guardianId)
    }

    fun fosterParentOfChild() = rule { userId, now ->
        QueryFragment<ChildId>(
            """
SELECT child_id AS id
FROM foster_parent
WHERE parent_id = :userId AND valid_during @> :today
            """.trimIndent()
        )
            .bind("today", now.toLocalDate())
            .bind("userId", userId)
    }

    fun guardianOfChildOfChildImage() = rule { guardianId, _ ->
        QueryFragment<ChildImageId>(
            """
SELECT img.id
FROM child_images img
JOIN person child ON img.child_id = child.id
JOIN guardian ON child.id = guardian.child_id
WHERE guardian_id = :guardianId
            """.trimIndent()
        )
            .bind("guardianId", guardianId)
    }

    fun guardianOfChildOfIncomeStatement() = rule { citizenId, _ ->
        QueryFragment<IncomeStatementId>(
            """
SELECT id
FROM income_statement i
JOIN guardian g ON i.person_id = g.child_id
WHERE g.guardian_id = :userId
            """.trimIndent()
        )
            .bind("userId", citizenId)
    }

    fun guardianOfChildOfPedagogicalDocument() = rule { guardianId, _ ->
        QueryFragment<PedagogicalDocumentId>(
            """
SELECT pd.id
FROM pedagogical_document pd
JOIN guardian g ON pd.child_id = g.child_id
WHERE g.guardian_id = :guardianId
            """.trimIndent()
        )
            .bind("guardianId", guardianId)
    }

    fun fosterParentOfChildOfPedagogicalDocument() = rule { userId, now ->
        QueryFragment<PedagogicalDocumentId>(
            """
SELECT pd.id
FROM pedagogical_document pd
JOIN foster_parent fp ON pd.child_id = fp.child_id
WHERE fp.parent_id = :userId AND fp.valid_during @> :today
            """.trimIndent()
        )
            .bind("today", now.toLocalDate())
            .bind("userId", userId)
    }

    fun guardianOfChildOfPedagogicalDocumentOfAttachment() = rule { guardianId, _ ->
        QueryFragment<AttachmentId>(
            """
SELECT a.id
FROM attachment a
JOIN pedagogical_document pd ON a.pedagogical_document_id = pd.id
JOIN guardian g ON pd.child_id = g.child_id
WHERE g.guardian_id = :guardianId
            """.trimIndent()
        )
            .bind("guardianId", guardianId)
    }

    fun fosterParentOfChildOfPedagogicalDocumentOfAttachment() = rule { userId, now ->
        QueryFragment<AttachmentId>(
            """
SELECT a.id
FROM attachment a
JOIN pedagogical_document pd ON a.pedagogical_document_id = pd.id
JOIN foster_parent fp ON pd.child_id = fp.child_id
WHERE fp.parent_id = :userId AND fp.valid_during @> :today
            """.trimIndent()
        )
            .bind("today", now.toLocalDate())
            .bind("userId", userId)
    }

    fun guardianOfChildOfVasu() = rule { citizenId, _ ->
        QueryFragment<VasuDocumentId>(
            """
SELECT id
FROM curriculum_document cd
JOIN guardian g ON cd.child_id = g.child_id
WHERE g.guardian_id = :userId
            """.trimIndent()
        )
            .bind("userId", citizenId)
    }

    fun fosterParentOfChildOfVasu() = rule { citizenId, now ->
        QueryFragment<VasuDocumentId>(
            """
SELECT cd.id
FROM curriculum_document cd
JOIN foster_parent fp ON cd.child_id = fp.child_id
WHERE fp.parent_id = :userId AND fp.valid_during @> :today
"""
        )
            .bind("userId", citizenId)
            .bind("today", now.toLocalDate())
    }

    fun guardianOfChildOfPlacement() = rule { guardianId, _ ->
        QueryFragment<PlacementId>(
            """
SELECT placement.id
FROM placement
JOIN guardian ON placement.child_id = guardian.child_id
WHERE guardian_id = :guardianId
            """.trimIndent()
        )
            .bind("guardianId", guardianId)
    }

    fun fosterParentOfChildOfPlacement() = rule { userId, now ->
        QueryFragment<PlacementId>(
            """
SELECT placement.id
FROM placement
JOIN foster_parent ON placement.child_id = foster_parent.child_id
WHERE parent_id = :userId
            """.trimIndent()
        )
            .bind("userId", userId)
            .bind("today", now.toLocalDate())
    }

    fun guardianOfChildOfAssistanceNeedDecision() = rule { citizenId, _ ->
        QueryFragment<AssistanceNeedDecisionId>(
            """
SELECT id
FROM assistance_need_decision ad
WHERE EXISTS(SELECT 1 FROM guardian g WHERE g.guardian_id = :userId AND g.child_id = ad.child_id)
            """.trimIndent()
        )
            .bind("userId", citizenId)
    }

    fun fosterParentOfChildOfAssistanceNeedDecision() = rule { citizenId, now ->
        QueryFragment<AssistanceNeedDecisionId>(
            """
SELECT id
FROM assistance_need_decision ad
WHERE EXISTS(SELECT 1 FROM foster_parent fp WHERE fp.parent_id = :userId AND fp.child_id = ad.child_id AND fp.valid_during @> :today)
            """.trimIndent()
        )
            .bind("userId", citizenId)
            .bind("today", now.toLocalDate())
    }

    fun hasPermissionForAttachmentThroughMessageContent() = rule { personId, _ ->
        QueryFragment<AttachmentId>(
            """
SELECT att.id
FROM attachment att
JOIN message_content content ON att.message_content_id = content.id
JOIN message msg ON content.id = msg.content_id
JOIN message_recipients rec ON msg.id = rec.message_id
JOIN message_account ma ON ma.id = msg.sender_id OR ma.id = rec.recipient_id
WHERE ma.person_id = :personId
            """.trimIndent()
        )
            .bind("personId", personId)
    }

    fun ownerOfApplication() = rule { citizenId, _ ->
        QueryFragment<ApplicationId>(
            """
SELECT id
FROM application
WHERE guardian_id = :userId
            """.trimIndent()
        )
            .bind("userId", citizenId)
    }

    fun ownerOfApplicationOfSentDecision() = rule { citizenId, _ ->
        QueryFragment<DecisionId>(
            """
SELECT decision.id
FROM decision
JOIN application ON decision.application_id = application.id
WHERE guardian_id = :userId
AND decision.sent_date IS NOT NULL
            """.trimIndent()
        )
            .bind("userId", citizenId)
    }

    fun ownerOfIncomeStatement() = rule { citizenId, _ ->
        QueryFragment<IncomeStatementId>(
            """
SELECT id
FROM income_statement
WHERE person_id = :userId
            """.trimIndent()
        )
            .bind("userId", citizenId)
    }

    fun recipientOfDailyServiceTimeNotification() = rule { citizenId, _ ->
        QueryFragment<DailyServiceTimeNotificationId>(
            """
SELECT id
FROM daily_service_time_notification
WHERE guardian_id = :citizenId
            """.trimIndent()
        )
            .bind("citizenId", citizenId)
    }
}
