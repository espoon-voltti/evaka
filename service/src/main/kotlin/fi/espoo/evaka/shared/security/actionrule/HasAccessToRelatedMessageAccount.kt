// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.messaging.filterPermittedMessageDrafts
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControlDecision

private typealias FilterByMessageAccount<T> = (tx: Database.Read, employee: AuthenticatedUser.Employee, targets: Set<T>) -> Iterable<T>

object HasAccessToRelatedMessageAccount : ActionRuleParams<HasAccessToRelatedMessageAccount> {
    override fun merge(other: HasAccessToRelatedMessageAccount): HasAccessToRelatedMessageAccount =
        HasAccessToRelatedMessageAccount

    private data class Query<T>(private val filter: FilterByMessageAccount<T>) :
        DatabaseActionRule.Query<T, HasAccessToRelatedMessageAccount> {
        override fun execute(
            tx: Database.Read,
            user: AuthenticatedUser,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<HasAccessToRelatedMessageAccount>> = when (user) {
            is AuthenticatedUser.Employee -> filter(tx, user, targets).associateWith { Deferred }
            else -> emptyMap()
        }
    }

    private object Deferred : DatabaseActionRule.Deferred<HasAccessToRelatedMessageAccount> {
        override fun evaluate(params: HasAccessToRelatedMessageAccount): AccessControlDecision =
            AccessControlDecision.Permitted(params)
    }

    val messageDraft = DatabaseActionRule(
        this,
        Query<MessageDraftId> { tx, employee, ids -> tx.filterPermittedMessageDrafts(employee, ids) }
    )
}
