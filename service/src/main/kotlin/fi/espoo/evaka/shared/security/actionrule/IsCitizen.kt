// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
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

    private data class Query<T>(private val filter: FilterByCitizen<T>) : DatabaseActionRule.Query<T, IsCitizen> {
        override fun execute(tx: Database.Read, user: AuthenticatedUser, targets: Set<T>): Map<T, DatabaseActionRule.Deferred<IsCitizen>> = when (user) {
            is AuthenticatedUser.Citizen -> user.authLevel
            is AuthenticatedUser.WeakCitizen -> user.authLevel
            else -> null
        }?.let { authLevel -> filter(tx, PersonId(user.id), targets).associateWith { Deferred(authLevel) } } ?: emptyMap()
    }
    private class Deferred(private val authLevel: CitizenAuthLevel) : DatabaseActionRule.Deferred<IsCitizen> {
        override fun evaluate(params: IsCitizen): AccessControlDecision =
            if (authLevel == CitizenAuthLevel.STRONG || params.allowWeakLogin) {
                AccessControlDecision.Permitted(params)
            } else {
                AccessControlDecision.None
            }
    }

    fun any() = object : StaticActionRule {
        override fun isPermitted(user: AuthenticatedUser): Boolean = when (user) {
            is AuthenticatedUser.Citizen -> true
            is AuthenticatedUser.WeakCitizen -> allowWeakLogin
            else -> false
        }
    }

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
}
