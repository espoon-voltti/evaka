// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.CalendarEventId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.DailyServiceTimeNotificationId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceApplicationId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision

private typealias FilterByCitizen =
    QuerySql.Builder.(personId: PersonId, now: HelsinkiDateTime) -> QuerySql

data class IsCitizen(val allowWeakLogin: Boolean) : DatabaseActionRule.Params {
    fun isPermittedAuthLevel(authLevel: CitizenAuthLevel) =
        authLevel == CitizenAuthLevel.STRONG || allowWeakLogin

    private fun <T : Id<*>> rule(filter: FilterByCitizen): DatabaseActionRule.Scoped<T, IsCitizen> =
        DatabaseActionRule.Scoped.Simple(this, Query(filter))

    private data class Query<T : Id<*>>(private val filter: FilterByCitizen) :
        DatabaseActionRule.Scoped.Query<T, IsCitizen> {
        override fun cacheKey(user: AuthenticatedUser, now: HelsinkiDateTime): Any =
            when (user) {
                is AuthenticatedUser.Citizen -> QuerySql { filter(user.id, now) }
                else -> Pair(user, now)
            }

        override fun executeWithTargets(
            ctx: DatabaseActionRule.QueryContext,
            targets: Set<T>,
        ): Map<T, DatabaseActionRule.Deferred<IsCitizen>> =
            when (ctx.user) {
                is AuthenticatedUser.Citizen -> {
                    val targetCheck = targets.idTargetPredicate()
                    ctx.tx
                        .createQuery {
                            sql(
                                """
                    SELECT id
                    FROM (${subquery { filter(ctx.user.id, ctx.now) } }) fragment
                    WHERE ${predicate(targetCheck.forTable("fragment"))}
                    """
                                    .trimIndent()
                            )
                        }
                        .toSet<Id<DatabaseTable>>()
                        .let { matched ->
                            targets
                                .filter { matched.contains(it) }
                                .associateWith { Deferred(ctx.user.authLevel) }
                        }
                }
                else -> emptyMap()
            }

        override fun queryWithParams(
            ctx: DatabaseActionRule.QueryContext,
            params: IsCitizen,
        ): QuerySql? =
            when (ctx.user) {
                is AuthenticatedUser.Citizen ->
                    if (params.isPermittedAuthLevel(ctx.user.authLevel)) {
                        QuerySql { filter(ctx.user.id, ctx.now) }
                    } else {
                        null
                    }
                else -> null
            }
    }

    private class Deferred(private val authLevel: CitizenAuthLevel) :
        DatabaseActionRule.Deferred<IsCitizen> {
        override fun evaluate(params: IsCitizen): AccessControlDecision =
            if (params.isPermittedAuthLevel(authLevel)) {
                AccessControlDecision.Permitted(params)
            } else {
                AccessControlDecision.None
            }
    }

    fun any() =
        object : StaticActionRule {
            override fun evaluate(user: AuthenticatedUser): AccessControlDecision =
                if (user is AuthenticatedUser.Citizen && isPermittedAuthLevel(user.authLevel)) {
                    AccessControlDecision.Permitted(this)
                } else {
                    AccessControlDecision.None
                }
        }

    fun self() =
        object : DatabaseActionRule.Scoped<PersonId, IsCitizen> {
            override val params = this@IsCitizen
            override val query =
                object : DatabaseActionRule.Scoped.Query<PersonId, IsCitizen> {
                    override fun cacheKey(user: AuthenticatedUser, now: HelsinkiDateTime): Any =
                        Pair(user, now)

                    override fun executeWithTargets(
                        ctx: DatabaseActionRule.QueryContext,
                        targets: Set<PersonId>,
                    ): Map<PersonId, DatabaseActionRule.Deferred<IsCitizen>> =
                        when (ctx.user) {
                            is AuthenticatedUser.Citizen ->
                                targets
                                    .filter { it == ctx.user.id }
                                    .associateWith { Deferred(ctx.user.authLevel) }
                            else -> emptyMap()
                        }

                    override fun queryWithParams(
                        ctx: DatabaseActionRule.QueryContext,
                        params: IsCitizen,
                    ): QuerySql? =
                        when (ctx.user) {
                            is AuthenticatedUser.Citizen ->
                                QuerySql { sql("SELECT ${bind(ctx.user.id)} AS id") }
                            else -> null
                        }
                }
        }

    fun uploaderOfAttachment() =
        rule<AttachmentId> { personId, _ ->
            sql(
                """
SELECT id
FROM attachment
WHERE uploaded_by = ${bind(personId)}
            """
                    .trimIndent()
            )
        }

    fun guardianOfChild() =
        rule<ChildId> { guardianId, _ ->
            sql(
                """
SELECT child_id AS id
FROM guardian
WHERE guardian_id = ${bind(guardianId)}
            """
                    .trimIndent()
            )
        }

    fun fosterParentOfChild() =
        rule<ChildId> { userId, now ->
            sql(
                """
SELECT child_id AS id
FROM foster_parent
WHERE parent_id = ${bind(userId)} AND valid_during @> ${bind(now.toLocalDate())}
            """
                    .trimIndent()
            )
        }

    fun fosterParentOfChildOfChildImage() =
        rule<ChildImageId> { userId, now ->
            sql(
                """
SELECT img.id
FROM child_images img
JOIN person child ON img.child_id = child.id
JOIN foster_parent ON child.id = foster_parent.child_id
WHERE parent_id = ${bind(userId)} AND valid_during @> ${bind(now.toLocalDate())}
            """
                    .trimIndent()
            )
        }

    fun guardianOfChildOfChildImage() =
        rule<ChildImageId> { guardianId, _ ->
            sql(
                """
SELECT img.id
FROM child_images img
JOIN person child ON img.child_id = child.id
JOIN guardian ON child.id = guardian.child_id
WHERE guardian_id = ${bind(guardianId)}
            """
                    .trimIndent()
            )
        }

    fun guardianOfChildOfIncomeStatement() =
        rule<IncomeStatementId> { citizenId, _ ->
            sql(
                """
SELECT id
FROM income_statement i
JOIN guardian g ON i.person_id = g.child_id
WHERE g.guardian_id = ${bind(citizenId)}
            """
                    .trimIndent()
            )
        }

    fun guardianOfChildOfPedagogicalDocument() =
        rule<PedagogicalDocumentId> { guardianId, _ ->
            sql(
                """
SELECT pd.id
FROM pedagogical_document pd
JOIN guardian g ON pd.child_id = g.child_id
WHERE g.guardian_id = ${bind(guardianId)}
            """
                    .trimIndent()
            )
        }

    fun fosterParentOfChildOfPedagogicalDocument() =
        rule<PedagogicalDocumentId> { userId, now ->
            sql(
                """
SELECT pd.id
FROM pedagogical_document pd
JOIN foster_parent fp ON pd.child_id = fp.child_id
WHERE fp.parent_id = ${bind(userId)} AND fp.valid_during @> ${bind(now.toLocalDate())}
            """
                    .trimIndent()
            )
        }

    fun guardianOfChildOfPedagogicalDocumentOfAttachment() =
        rule<AttachmentId> { guardianId, _ ->
            sql(
                """
SELECT a.id
FROM attachment a
JOIN pedagogical_document pd ON a.pedagogical_document_id = pd.id
JOIN guardian g ON pd.child_id = g.child_id
WHERE g.guardian_id = ${bind(guardianId)}
            """
                    .trimIndent()
            )
        }

    fun fosterParentOfChildOfPedagogicalDocumentOfAttachment() =
        rule<AttachmentId> { userId, now ->
            sql(
                """
SELECT a.id
FROM attachment a
JOIN pedagogical_document pd ON a.pedagogical_document_id = pd.id
JOIN foster_parent fp ON pd.child_id = fp.child_id
WHERE fp.parent_id = ${bind(userId)} AND fp.valid_during @> ${bind(now.toLocalDate())}
            """
                    .trimIndent()
            )
        }

    fun guardianOfChildOfPlacement() =
        rule<PlacementId> { guardianId, _ ->
            sql(
                """
SELECT placement.id
FROM placement
JOIN guardian ON placement.child_id = guardian.child_id
WHERE guardian_id = ${bind(guardianId)}
            """
                    .trimIndent()
            )
        }

    fun fosterParentOfChildOfPlacement() =
        rule<PlacementId> { userId, now ->
            sql(
                """
SELECT placement.id
FROM placement
JOIN foster_parent fp ON placement.child_id = fp.child_id AND fp.valid_during @> ${bind(now.toLocalDate())}
WHERE parent_id = ${bind(userId)}
            """
                    .trimIndent()
            )
        }

    fun guardianOfChildOfAssistanceNeedDecision() =
        rule<AssistanceNeedDecisionId> { citizenId, _ ->
            sql(
                """
SELECT id
FROM assistance_need_decision ad
WHERE EXISTS(SELECT 1 FROM guardian g WHERE g.guardian_id = ${bind(citizenId)} AND g.child_id = ad.child_id)
            """
                    .trimIndent()
            )
        }

    fun fosterParentOfChildOfAssistanceNeedDecision() =
        rule<AssistanceNeedDecisionId> { citizenId, now ->
            sql(
                """
SELECT id
FROM assistance_need_decision ad
WHERE EXISTS(SELECT 1 FROM foster_parent fp WHERE fp.parent_id = ${bind(citizenId)} AND fp.child_id = ad.child_id AND fp.valid_during @> ${bind(now.toLocalDate())})
            """
                    .trimIndent()
            )
        }

    fun guardianOfChildOfAssistanceNeedPreschoolDecision() =
        rule<AssistanceNeedPreschoolDecisionId> { citizenId, _ ->
            sql(
                """
SELECT id
FROM assistance_need_preschool_decision ad
WHERE EXISTS(SELECT 1 FROM guardian g WHERE g.guardian_id = ${bind(citizenId)} AND g.child_id = ad.child_id)
            """
                    .trimIndent()
            )
        }

    fun fosterParentOfChildOfAssistanceNeedPreschoolDecision() =
        rule<AssistanceNeedPreschoolDecisionId> { citizenId, now ->
            sql(
                """
SELECT id
FROM assistance_need_preschool_decision ad
WHERE EXISTS(SELECT 1 FROM foster_parent fp WHERE fp.parent_id = ${bind(citizenId)} AND fp.child_id = ad.child_id AND fp.valid_during @> ${bind(now.toLocalDate())})
            """
                    .trimIndent()
            )
        }

    fun guardianOfChildOfPublishedChildDocument() =
        rule<ChildDocumentId> { citizenId, _ ->
            sql(
                """
SELECT id
FROM child_document cd
WHERE EXISTS(SELECT 1 FROM guardian g WHERE g.guardian_id = ${bind(citizenId)} AND g.child_id = cd.child_id)
    AND cd.published_at IS NOT NULL 
            """
                    .trimIndent()
            )
        }

    fun fosterParentOfChildOfPublishedChildDocument() =
        rule<ChildDocumentId> { citizenId, now ->
            sql(
                """
SELECT id
FROM child_document cd
WHERE EXISTS(SELECT 1 FROM foster_parent fp WHERE fp.parent_id = ${bind(citizenId)} AND fp.child_id = cd.child_id AND fp.valid_during @> ${bind(now.toLocalDate())})
    AND cd.published_at IS NOT NULL 
            """
                    .trimIndent()
            )
        }

    fun hasMessageAccount() =
        rule<MessageAccountId> { citizenId, _ ->
            sql("SELECT id FROM message_account WHERE person_id = ${bind(citizenId)}")
        }

    fun ownerOfApplication() =
        rule<ApplicationId> { citizenId, _ ->
            sql(
                """
SELECT id
FROM application
WHERE guardian_id = ${bind(citizenId)}
            """
            )
        }

    fun otherGuardianOfApplication() =
        rule<ApplicationId> { citizenId, now ->
            sql(
                """
SELECT a.id
FROM application a
JOIN application_other_guardian aog ON a.id = aog.application_id
WHERE aog.guardian_id = ${bind(citizenId)}
AND allow_other_guardian_access IS TRUE
AND (
    EXISTS (SELECT FROM guardian g WHERE g.guardian_id = aog.guardian_id AND g.child_id = a.child_id)
    OR EXISTS (SELECT FROM foster_parent fp WHERE fp.parent_id = aog.guardian_id AND fp.child_id = a.child_id AND valid_during @> ${bind(now.toLocalDate())})
)
"""
            )
        }

    fun ownerOfApplicationOfSentDecision() =
        rule<DecisionId> { citizenId, _ ->
            sql(
                """
SELECT decision.id
FROM decision
JOIN application ON decision.application_id = application.id
WHERE guardian_id = ${bind(citizenId)}
AND decision.sent_date IS NOT NULL
"""
            )
        }

    fun otherGuardianOfApplicationOfSentDecision() =
        rule<DecisionId> { citizenId, now ->
            sql(
                """
SELECT decision.id
FROM decision
JOIN application a ON decision.application_id = a.id
JOIN application_other_guardian aog ON a.id = aog.application_id
WHERE aog.guardian_id = ${bind(citizenId)}
AND decision.sent_date IS NOT NULL
AND allow_other_guardian_access IS TRUE
AND (
    EXISTS (SELECT FROM guardian g WHERE g.guardian_id = aog.guardian_id AND g.child_id = a.child_id)
    OR EXISTS (SELECT FROM foster_parent fp WHERE fp.parent_id = aog.guardian_id AND fp.child_id = a.child_id AND valid_during @> ${bind(now.toLocalDate())})
)
"""
            )
        }

    fun otherGuardianOfApplicationOfSentDecisionWithNoContactInfo() =
        rule<DecisionId> { citizenId, now ->
            sql(
                """
SELECT decision.id
FROM decision
JOIN application a ON decision.application_id = a.id
JOIN application_other_guardian aog ON a.id = aog.application_id
WHERE aog.guardian_id = ${bind(citizenId)}
AND decision.sent_date IS NOT NULL
AND allow_other_guardian_access IS TRUE
AND (
    EXISTS (SELECT FROM guardian g WHERE g.guardian_id = aog.guardian_id AND g.child_id = a.child_id)
    OR EXISTS (SELECT FROM foster_parent fp WHERE fp.parent_id = aog.guardian_id AND fp.child_id = a.child_id AND valid_during @> ${bind(now.toLocalDate())})
)
AND NOT decision.document_contains_contact_info
"""
            )
        }

    fun ownerOfServiceApplication() =
        rule<ServiceApplicationId> { userId, _ ->
            sql(
                """
SELECT id
FROM service_application
WHERE person_id = ${bind(userId)}
            """
            )
        }

    fun ownerOfIncomeStatement() =
        rule<IncomeStatementId> { citizenId, _ ->
            sql(
                """
SELECT id
FROM income_statement
WHERE person_id = ${bind(citizenId)}
            """
                    .trimIndent()
            )
        }

    fun recipientOfDailyServiceTimeNotification() =
        rule<DailyServiceTimeNotificationId> { citizenId, _ ->
            sql(
                """
SELECT id
FROM daily_service_time_notification
WHERE guardian_id = ${bind(citizenId)}
            """
                    .trimIndent()
            )
        }

    fun liableForFeeDecisionPayment() =
        rule<FeeDecisionId> { citizenId, _ ->
            sql(
                """
SELECT fd.id
FROM fee_decision fd
WHERE (fd.head_of_family_id = ${bind(citizenId)} OR fd.partner_id = ${bind(citizenId)}) 
            """
                    .trimIndent()
            )
        }

    fun liableForVoucherValueDecisionPayment() =
        rule<VoucherValueDecisionId> { citizenId, _ ->
            sql(
                """
SELECT vvd.id
FROM voucher_value_decision vvd
WHERE (vvd.head_of_family_id = ${bind(citizenId)} OR vvd.partner_id = ${bind(citizenId)}) 
            """
                    .trimIndent()
            )
        }

    fun guardianOfChildOfCalendarEventAttendee() =
        rule<CalendarEventId> { citizenId, _ ->
            sql(
                """
SELECT cea.calendar_event_id AS id
FROM calendar_event_attendee_child_view cea
JOIN person child ON cea.child_id = child.id
JOIN guardian ON child.id = guardian.child_id
WHERE guardian_id = ${bind(citizenId)}
"""
            )
        }

    fun fosterParentOfChildOfCalendarEventAttendee() =
        rule<CalendarEventId> { citizenId, now ->
            sql(
                """
SELECT cea.calendar_event_id AS id
FROM calendar_event_attendee_child_view cea
JOIN person child ON cea.child_id = child.id
JOIN foster_parent ON child.id = foster_parent.child_id
WHERE parent_id = ${bind(citizenId)} AND valid_during @> ${bind(now.toLocalDate())}
"""
            )
        }
}
