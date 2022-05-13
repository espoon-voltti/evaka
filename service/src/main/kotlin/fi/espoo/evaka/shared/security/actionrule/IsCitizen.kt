// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.messaging.filterCitizenPermittedAttachmentsThroughMessageContent
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
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
import org.jdbi.v3.core.kotlin.mapTo

private typealias FilterByCitizen<T> = (tx: Database.Read, personId: PersonId, now: HelsinkiDateTime, targets: Set<T>) -> Iterable<T>

data class IsCitizen(val allowWeakLogin: Boolean) : ActionRuleParams<IsCitizen> {
    fun isPermittedAuthLevel(authLevel: CitizenAuthLevel) = authLevel == CitizenAuthLevel.STRONG || allowWeakLogin

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

    fun self() = object : TargetActionRule<PersonId> {
        override fun evaluate(user: AuthenticatedUser, target: PersonId): AccessControlDecision = when (user) {
            is AuthenticatedUser.Citizen -> if (user.id == target && isPermittedAuthLevel(user.authLevel)) {
                AccessControlDecision.Permitted(this@IsCitizen)
            } else AccessControlDecision.None
            else -> AccessControlDecision.None
        }
    }

    fun uploaderOfAttachment() = DatabaseActionRule(
        this,
        Query<AttachmentId> { tx, personId, _, ids ->
            tx.createQuery(
                """
SELECT id
FROM attachment
WHERE uploaded_by = :personId
AND id = ANY(:ids)
                """.trimIndent()
            )
                .bind("ids", ids.toTypedArray())
                .bind("personId", personId)
                .mapTo()
        }
    )

    fun guardianOfChild() = DatabaseActionRule(
        this,
        Query<ChildId> { tx, guardianId, _, ids ->
            tx.createQuery(
                """
SELECT child_id
FROM guardian
WHERE guardian_id = :guardianId
AND child_id = ANY(:ids)
                """.trimIndent()
            )
                .bind("ids", ids.toTypedArray())
                .bind("guardianId", guardianId)
                .mapTo()
        }
    )

    fun guardianOfChildOfChildImage() = DatabaseActionRule(
        this,
        Query<ChildImageId> { tx, guardianId, _, ids ->
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
                .bind("ids", ids.toTypedArray())
                .bind("guardianId", guardianId)
                .mapTo()
        }
    )

    fun guardianOfChildOfIncomeStatement() = DatabaseActionRule(
        this,
        Query<IncomeStatementId> { tx, citizenId, _, ids ->
            tx.createQuery(
                """
SELECT id
FROM income_statement i
JOIN guardian g ON i.person_id = g.child_id
WHERE i.id = ANY(:ids)
AND g.guardian_id = :userId
                """.trimIndent()
            )
                .bind("ids", ids.toTypedArray())
                .bind("userId", citizenId)
                .mapTo()
        }
    )

    fun guardianOfChildOfPedagogicalDocument() = DatabaseActionRule(
        this,
        Query<PedagogicalDocumentId> { tx, guardianId, _, ids ->
            tx.createQuery(
                """
SELECT pd.id
FROM pedagogical_document pd
JOIN guardian g ON pd.child_id = g.child_id
WHERE pd.id = ANY(:ids)
AND g.guardian_id = :guardianId
                """.trimIndent()
            )
                .bind("ids", ids.toTypedArray())
                .bind("guardianId", guardianId)
                .mapTo()
        }
    )

    fun guardianOfChildOfPedagogicalDocumentOfAttachment() = DatabaseActionRule(
        this,
        Query<AttachmentId> { tx, guardianId, _, ids ->
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
                .bind("ids", ids.toTypedArray())
                .bind("guardianId", guardianId)
                .mapTo()
        }
    )

    fun guardianOfChildOfVasu() = DatabaseActionRule(
            this,
            Query<VasuDocumentId> { tx, citizenId, ids ->
                tx.createQuery(
                        """
SELECT id
FROM curriculum_document cd
JOIN guardian g ON cd.child_id = g.child_id
WHERE cd.id = ANY(:ids)
AND g.guardian_id = :userId
                """.trimIndent()
                )
                        .bind("ids", ids.toTypedArray())
                        .bind("userId", citizenId)
                        .mapTo()
            }
    )

    fun guardianOfChildOfPlacement() = DatabaseActionRule(
        this,
        Query<PlacementId> { tx, guardianId, _, ids ->
            tx.createQuery(
                """
SELECT placement.id
FROM placement
JOIN guardian ON placement.child_id = guardian.child_id
WHERE placement.id = ANY(:ids)
AND guardian_id = :guardianId
                """.trimIndent()
            )
                .bind("ids", ids.toTypedArray())
                .bind("guardianId", guardianId)
                .mapTo()
        }
    )

    fun hasPermissionForAttachmentThroughMessageContent() = DatabaseActionRule(
        this,
        Query<AttachmentId> { tx, personId, _, ids -> tx.filterCitizenPermittedAttachmentsThroughMessageContent(personId, ids) }
    )

    fun ownerOfApplication() = DatabaseActionRule(
        this,
        Query<ApplicationId> { tx, citizenId, _, ids ->
            tx.createQuery(
                """
SELECT id
FROM application
WHERE guardian_id = :userId
AND id = ANY(:ids)
                """.trimIndent()
            )
                .bind("ids", ids.toTypedArray())
                .bind("userId", citizenId)
                .mapTo()
        }
    )

    fun ownerOfApplicationOfSentDecision() = DatabaseActionRule(
        this,
        Query<DecisionId> { tx, citizenId, _, ids ->
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
                .bind("ids", ids.toTypedArray())
                .bind("userId", citizenId)
                .mapTo()
        }
    )

    fun ownerOfIncomeStatement() = DatabaseActionRule(
        this,
        Query<IncomeStatementId> { tx, citizenId, _, ids ->
            tx.createQuery(
                """
SELECT id
FROM income_statement
WHERE person_id = :userId
AND id = ANY(:ids)
                """.trimIndent()
            )
                .bind("ids", ids.toTypedArray())
                .bind("userId", citizenId)
                .mapTo()
        }
    )
}
