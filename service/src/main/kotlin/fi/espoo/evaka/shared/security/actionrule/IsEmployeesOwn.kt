// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.VasuDocumentFollowupEntryId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.vasu.getVasuFollowupEntry
import org.jdbi.v3.core.kotlin.mapTo

private typealias FilterByEmployeeOwnership<T> = (tx: Database.Read, user: AuthenticatedUser.Employee, targets: Set<T>) -> Iterable<T>

object IsEmployeesOwn : ActionRuleParams<IsEmployeesOwn> {
    override fun merge(other: IsEmployeesOwn): IsEmployeesOwn = IsEmployeesOwn

    private data class Query<I>(private val filter: FilterByEmployeeOwnership<I>) : DatabaseActionRule.Query<I, IsEmployeesOwn> {
        override fun execute(
            tx: Database.Read,
            user: AuthenticatedUser,
            targets: Set<I>
        ): Map<I, DatabaseActionRule.Deferred<IsEmployeesOwn>> = when (user) {
            is AuthenticatedUser.Employee -> filter(tx, user, targets).associateWith { Deferred }
            else -> emptyMap()
        }
    }
    private object Deferred : DatabaseActionRule.Deferred<IsEmployeesOwn> {
        override fun evaluate(params: IsEmployeesOwn): AccessControlDecision = AccessControlDecision.Permitted(params)
    }

    fun mobileDevice() = DatabaseActionRule(
        this,
        Query<MobileDeviceId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT id
FROM mobile_device
WHERE employee_id = :userId
AND id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun pairing() = DatabaseActionRule(
        this,
        Query<PairingId> { tx, user, ids ->
            tx.createQuery(
                """
SELECT id
FROM pairing
WHERE employee_id = :userId
AND id = ANY(:ids)
                """.trimIndent()
            )
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun vasuDocumentFollowupEntry() = DatabaseActionRule(
        this,
        Query<VasuDocumentFollowupEntryId> { tx, user, ids ->
            // TODO: replace naive loop with a batch operation
            ids.filter { id -> tx.getVasuFollowupEntry(id).authorId == user.id }
        }
    )
}
