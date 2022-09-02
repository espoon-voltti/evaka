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
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision

private typealias FilterByCitizen<T> = (tx: Database.Read, personId: PersonId, now: HelsinkiDateTime, targets: Set<T>) -> Iterable<T>

data class IsCitizen(val allowWeakLogin: Boolean) : ActionRuleParams<IsCitizen> {
    fun isPermittedAuthLevel(authLevel: CitizenAuthLevel) = authLevel == CitizenAuthLevel.STRONG || allowWeakLogin

    private fun <T> rule(filter: FilterByCitizen<T>): DatabaseActionRule<T, IsCitizen> =
        DatabaseActionRule.Simple(this, Query(filter))
    private data class Query<T>(private val filter: FilterByCitizen<T>) : DatabaseActionRule.Query<T, IsCitizen> {
        override fun execute(
            tx: Database.Read,
            user: AuthenticatedUser,
            now: HelsinkiDateTime,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<IsCitizen>> = when (user) {
            is AuthenticatedUser.Citizen -> Pair(user.authLevel, user.id)
            else -> null
        }?.let { (authLevel, id) -> filter(tx, id, now, targets).associateWith { Deferred(authLevel) } } ?: emptyMap()
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
        override fun isPermitted(user: AuthenticatedUser): Boolean = when (user) {
            is AuthenticatedUser.Citizen -> isPermittedAuthLevel(user.authLevel)
            else -> false
        }
    }

    fun self() = rule<PersonId> { _, personId, _, ids ->
        ids.filter { it == personId }
    }

    fun uploaderOfAttachment() = rule<AttachmentId> { tx, personId, _, ids ->
        tx.createQuery(
            """
SELECT id
FROM attachment
WHERE uploaded_by = :personId
AND id = ANY(:ids)
            """.trimIndent()
        )
            .bind("ids", ids)
            .bind("personId", personId)
            .mapTo()
    }

    fun guardianOfChild() = rule<ChildId> { tx, guardianId, _, ids ->
        tx.createQuery(
            """
SELECT child_id
FROM guardian
WHERE guardian_id = :guardianId
AND child_id = ANY(:ids)
            """.trimIndent()
        )
            .bind("ids", ids)
            .bind("guardianId", guardianId)
            .mapTo()
    }

    fun guardianOfChildOfChildImage() = rule<ChildImageId> { tx, guardianId, _, ids ->
        tx.createQuery(
            """
SELECT img.id
FROM child_images img
JOIN person child ON img.child_id = child.id
JOIN guardian ON child.id = guardian.child_id
WHERE img.id = ANY(:ids)
AND guardian_id = :guardianId
            """.trimIndent()
        )
            .bind("ids", ids)
            .bind("guardianId", guardianId)
            .mapTo()
    }

    fun guardianOfChildOfIncomeStatement() = rule<IncomeStatementId> { tx, citizenId, _, ids ->
        tx.createQuery(
            """
SELECT id
FROM income_statement i
JOIN guardian g ON i.person_id = g.child_id
WHERE i.id = ANY(:ids)
AND g.guardian_id = :userId
            """.trimIndent()
        )
            .bind("ids", ids)
            .bind("userId", citizenId)
            .mapTo()
    }

    fun guardianOfChildOfPedagogicalDocument() = rule<PedagogicalDocumentId> { tx, guardianId, _, ids ->
        tx.createQuery(
            """
SELECT pd.id
FROM pedagogical_document pd
JOIN guardian g ON pd.child_id = g.child_id
WHERE pd.id = ANY(:ids)
AND g.guardian_id = :guardianId
            """.trimIndent()
        )
            .bind("ids", ids)
            .bind("guardianId", guardianId)
            .mapTo()
    }

    fun guardianOfChildOfPedagogicalDocumentOfAttachment() = rule<AttachmentId> { tx, guardianId, _, ids ->
        tx.createQuery(
            """
SELECT a.id
FROM attachment a
JOIN pedagogical_document pd ON a.pedagogical_document_id = pd.id
JOIN guardian g ON pd.child_id = g.child_id
WHERE a.id = ANY(:ids)
AND g.guardian_id = :guardianId
            """.trimIndent()
        )
            .bind("ids", ids)
            .bind("guardianId", guardianId)
            .mapTo()
    }

    fun guardianOfChildOfVasu() = rule<VasuDocumentId> { tx, citizenId, _, ids ->
        tx.createQuery(
            """
SELECT id
FROM curriculum_document cd
JOIN guardian g ON cd.child_id = g.child_id
WHERE cd.id = ANY(:ids)
AND g.guardian_id = :userId
            """.trimIndent()
        )
            .bind("ids", ids)
            .bind("userId", citizenId)
            .mapTo()
    }

    fun guardianOfChildOfPlacement() = rule<PlacementId> { tx, guardianId, _, ids ->
        tx.createQuery(
            """
SELECT placement.id
FROM placement
JOIN guardian ON placement.child_id = guardian.child_id
WHERE placement.id = ANY(:ids)
AND guardian_id = :guardianId
            """.trimIndent()
        )
            .bind("ids", ids)
            .bind("guardianId", guardianId)
            .mapTo()
    }

    fun guardianOfChildOfAssistanceNeedDecision() = rule<AssistanceNeedDecisionId> { tx, citizenId, _, ids ->
        tx.createQuery(
            """
SELECT id
FROM assistance_need_decision ad
WHERE EXISTS(SELECT 1 FROM guardian g WHERE g.guardian_id = :userId AND g.child_id = ad.child_id)
            """.trimIndent()
        )
            .bind("ids", ids)
            .bind("userId", citizenId)
            .mapTo()
    }

    fun hasPermissionForAttachmentThroughMessageContent() = rule<AttachmentId> { tx, personId, _, ids ->
        tx.createQuery(
            """
SELECT att.id
FROM attachment att
JOIN message_content content ON att.message_content_id = content.id
JOIN message msg ON content.id = msg.content_id
JOIN message_recipients rec ON msg.id = rec.message_id
JOIN message_account ma ON ma.id = msg.sender_id OR ma.id = rec.recipient_id
WHERE att.id = ANY(:ids) AND ma.person_id = :personId
            """.trimIndent()
        )
            .bind("personId", personId)
            .bind("ids", ids)
            .mapTo()
    }

    fun ownerOfApplication() = rule<ApplicationId> { tx, citizenId, _, ids ->
        tx.createQuery(
            """
SELECT id
FROM application
WHERE guardian_id = :userId
AND id = ANY(:ids)
            """.trimIndent()
        )
            .bind("ids", ids)
            .bind("userId", citizenId)
            .mapTo()
    }

    fun ownerOfApplicationOfSentDecision() = rule<DecisionId> { tx, citizenId, _, ids ->
        tx.createQuery(
            """
SELECT decision.id
FROM decision
JOIN application ON decision.application_id = application.id
WHERE guardian_id = :userId
AND decision.id = ANY(:ids)
AND decision.sent_date IS NOT NULL
            """.trimIndent()
        )
            .bind("ids", ids)
            .bind("userId", citizenId)
            .mapTo()
    }

    fun ownerOfIncomeStatement() = rule<IncomeStatementId> { tx, citizenId, _, ids ->
        tx.createQuery(
            """
SELECT id
FROM income_statement
WHERE person_id = :userId
AND id = ANY(:ids)
            """.trimIndent()
        )
            .bind("ids", ids)
            .bind("userId", citizenId)
            .mapTo()
    }

    fun guardianOfDailyServiceTimeNotification() = rule<DailyServiceTimeNotificationId> { tx, guardianId, _, ids ->
        tx.createQuery(
            """
SELECT id
FROM daily_service_time_notification
WHERE id = ANY(:ids)
AND guardian_id = :guardianId
            """.trimIndent()
        )
            .bind("ids", ids)
            .bind("guardianId", guardianId)
            .mapTo()
    }
}
