// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControlDecision
import org.jdbi.v3.core.kotlin.mapTo

private typealias FilterByGuardian<T> = (tx: Database.Read, personId: PersonId, targets: Set<T>) -> Iterable<T>

data class IsChildGuardian(val allowWeakLogin: Boolean) : ActionRuleParams<IsChildGuardian> {
    override fun merge(other: IsChildGuardian): IsChildGuardian = IsChildGuardian(this.allowWeakLogin || other.allowWeakLogin)

    private data class Query<T>(private val filterByGuardian: FilterByGuardian<T>) : DatabaseActionRule.Query<T, IsChildGuardian> {
        override fun execute(tx: Database.Read, user: AuthenticatedUser, targets: Set<T>): Map<T, DatabaseActionRule.Deferred<IsChildGuardian>> = when (user) {
            is AuthenticatedUser.Citizen -> user.authLevel
            is AuthenticatedUser.WeakCitizen -> user.authLevel
            else -> null
        }?.let { authLevel -> filterByGuardian(tx, PersonId(user.id), targets).associateWith { Deferred(authLevel) } } ?: emptyMap()
    }
    private class Deferred(private val authLevel: CitizenAuthLevel) : DatabaseActionRule.Deferred<IsChildGuardian> {
        override fun evaluate(params: IsChildGuardian): AccessControlDecision =
            if (authLevel == CitizenAuthLevel.STRONG || params.allowWeakLogin) {
                AccessControlDecision.Permitted(params)
            } else {
                AccessControlDecision.None
            }
    }

    val child = DatabaseActionRule(
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
    val childImage = DatabaseActionRule(
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
    val placement = DatabaseActionRule(
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
}
