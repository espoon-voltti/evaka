// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControlDecision
import org.jdbi.v3.core.kotlin.mapTo

private typealias FilterByOwnership<T> = (tx: Database.Read, citizenId: PersonId, targets: Set<T>) -> Iterable<T>

data class IsCitizensOwn(val allowWeakLogin: Boolean) : ActionRuleParams<IsCitizensOwn> {
    override fun merge(other: IsCitizensOwn): IsCitizensOwn = IsCitizensOwn(this.allowWeakLogin || other.allowWeakLogin)

    private data class Query<I>(private val filterByOwnership: FilterByOwnership<I>) : DatabaseActionRule.Query<I, IsCitizensOwn> {
        override fun execute(
            tx: Database.Read,
            user: AuthenticatedUser,
            targets: Set<I>
        ): Map<I, DatabaseActionRule.Deferred<IsCitizensOwn>> = when (user) {
            is AuthenticatedUser.Citizen -> user.authLevel
            is AuthenticatedUser.WeakCitizen -> user.authLevel
            else -> null
        }?.let { authLevel ->
            filterByOwnership(tx, PersonId(user.id), targets).associateWith { Deferred(authLevel) }
        } ?: emptyMap()
    }
    private class Deferred(private val authLevel: CitizenAuthLevel) : DatabaseActionRule.Deferred<IsCitizensOwn> {
        override fun evaluate(params: IsCitizensOwn): AccessControlDecision =
            if (authLevel == CitizenAuthLevel.STRONG || params.allowWeakLogin) {
                AccessControlDecision.Permitted(params)
            } else {
                AccessControlDecision.None
            }
    }
    val application = DatabaseActionRule(
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
