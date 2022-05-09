// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.messaging.filterCitizenPermittedAttachmentsThroughMessageContent
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControlDecision
import org.jdbi.v3.core.kotlin.mapTo

private typealias FilterByCitizen<T> = (tx: Database.Read, personId: PersonId, targets: Set<T>) -> Iterable<T>

data class IsCitizen(val allowWeakLogin: Boolean) : ActionRuleParams<IsCitizen> {
    override fun merge(other: IsCitizen): IsCitizen = IsCitizen(this.allowWeakLogin || other.allowWeakLogin)

    fun isPermittedAuthLevel(authLevel: CitizenAuthLevel) = authLevel == CitizenAuthLevel.STRONG || allowWeakLogin

    private data class Query<T>(private val filter: FilterByCitizen<T>) : DatabaseActionRule.Query<T, IsCitizen> {
        override fun execute(tx: Database.Read, user: AuthenticatedUser, targets: Set<T>): Map<T, DatabaseActionRule.Deferred<IsCitizen>> = when (user) {
            is AuthenticatedUser.Citizen -> Pair(user.authLevel, user.id)
            else -> null
        }?.let { (authLevel, id) -> filter(tx, id, targets).associateWith { Deferred(authLevel) } } ?: emptyMap()

        override fun classifier(): Any = filter.javaClass
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
        Query<AttachmentId> { tx, personId, ids ->
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
        Query<ChildId> { tx, guardianId, ids ->
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
        Query<ChildImageId> { tx, guardianId, ids ->
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
        Query<IncomeStatementId> { tx, citizenId, ids ->
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
        Query<PedagogicalDocumentId> { tx, guardianId, ids ->
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
        Query<AttachmentId> { tx, guardianId, ids ->
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

    fun guardianOfChildOfPlacement() = DatabaseActionRule(
        this,
        Query<PlacementId> { tx, guardianId, ids ->
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
        Query<AttachmentId> { tx, personId, ids -> tx.filterCitizenPermittedAttachmentsThroughMessageContent(personId, ids) }
    )

    fun ownerOfApplication() = DatabaseActionRule(
        this,
        Query<ApplicationId> { tx, citizenId, ids ->
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

    fun ownerOfIncomeStatement() = DatabaseActionRule(
        this,
        Query<IncomeStatementId> { tx, citizenId, ids ->
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
