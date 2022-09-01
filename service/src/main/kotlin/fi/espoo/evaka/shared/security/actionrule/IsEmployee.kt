// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.messaging.filterPermittedAttachmentsThroughMessageContent
import fi.espoo.evaka.messaging.filterPermittedAttachmentsThroughMessageDrafts
import fi.espoo.evaka.messaging.filterPermittedMessageDrafts
import fi.espoo.evaka.shared.ApplicationNoteId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.AccessControlDecision

private typealias FilterByEmployee<T> = (tx: Database.Read, user: AuthenticatedUser.Employee, now: HelsinkiDateTime, targets: Set<T>) -> Iterable<T>

object IsEmployee : ActionRuleParams<IsEmployee> {
    private fun <T> rule(filter: FilterByEmployee<T>): DatabaseActionRule<T, IsEmployee> =
        DatabaseActionRule.Simple(this, Query(filter))
    private data class Query<T>(private val filter: FilterByEmployee<T>) : DatabaseActionRule.Query<T, IsEmployee> {
        override fun execute(
            tx: Database.Read,
            user: AuthenticatedUser,
            now: HelsinkiDateTime,
            targets: Set<T>
        ): Map<T, DatabaseActionRule.Deferred<IsEmployee>> = when (user) {
            is AuthenticatedUser.Employee -> filter(tx, user, now, targets).associateWith { Deferred }
            else -> emptyMap()
        }
    }
    private object Deferred : DatabaseActionRule.Deferred<IsEmployee> {
        override fun evaluate(params: IsEmployee): AccessControlDecision = AccessControlDecision.Permitted(params)
    }

    fun self() = object : TargetActionRule<EmployeeId> {
        override fun evaluate(user: AuthenticatedUser, target: EmployeeId): AccessControlDecision = when (user) {
            is AuthenticatedUser.Employee -> if (user.id == target) {
                AccessControlDecision.Permitted(this@IsEmployee)
            } else AccessControlDecision.None
            else -> AccessControlDecision.None
        }
    }

    fun ownerOfMobileDevice() = rule<MobileDeviceId> { tx, user, _, ids ->
        tx.createQuery(
            """
SELECT id
FROM mobile_device
WHERE employee_id = :userId
AND id = ANY(:ids)
            """.trimIndent()
        )
            .bind("userId", user.id)
            .bind("ids", ids)
            .mapTo()
    }

    fun ownerOfPairing() = rule<PairingId> { tx, user, _, ids ->
        tx.createQuery(
            """
SELECT id
FROM pairing
WHERE employee_id = :userId
AND id = ANY(:ids)
            """.trimIndent()
        )
            .bind("userId", user.id)
            .bind("ids", ids)
            .mapTo()
    }

    fun authorOfApplicationNote() = rule<ApplicationNoteId> { tx, user, _, ids ->
        tx.createQuery("SELECT id FROM application_note WHERE created_by = :userId AND id = ANY(:ids)")
            .bind("userId", user.id)
            .bind("ids", ids)
            .mapTo()
    }

    fun hasPermissionForMessageDraft() = rule { tx, employee, _, ids ->
        tx.filterPermittedMessageDrafts(employee, ids)
    }

    fun hasPermissionForAttachmentThroughMessageContent() = rule { tx, employee, _, ids ->
        tx.filterPermittedAttachmentsThroughMessageContent(employee, ids)
    }

    fun hasPermissionForAttachmentThroughMessageDraft() = rule { tx, employee, _, ids ->
        tx.filterPermittedAttachmentsThroughMessageDrafts(employee, ids)
    }
}
