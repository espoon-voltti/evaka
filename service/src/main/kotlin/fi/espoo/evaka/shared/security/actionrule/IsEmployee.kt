// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security.actionrule

import fi.espoo.evaka.messaging.filterPermittedAttachmentsThroughMessageContent
import fi.espoo.evaka.messaging.filterPermittedAttachmentsThroughMessageDrafts
import fi.espoo.evaka.messaging.filterPermittedMessageDrafts
import fi.espoo.evaka.shared.ApplicationNoteId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.VasuDocumentFollowupEntryId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControlDecision
import fi.espoo.evaka.vasu.getVasuFollowupEntry
import org.jdbi.v3.core.kotlin.mapTo

private typealias FilterByEmployee<T> = (tx: Database.Read, user: AuthenticatedUser.Employee, targets: Set<T>) -> Iterable<T>

object IsEmployee : ActionRuleParams<IsEmployee> {
    override fun merge(other: IsEmployee): IsEmployee = IsEmployee

    private data class Query<I>(private val filter: FilterByEmployee<I>) : DatabaseActionRule.Query<I, IsEmployee> {
        override fun execute(
            tx: Database.Read,
            user: AuthenticatedUser,
            targets: Set<I>
        ): Map<I, DatabaseActionRule.Deferred<IsEmployee>> = when (user) {
            is AuthenticatedUser.Employee -> filter(tx, user, targets).associateWith { Deferred }
            else -> emptyMap()
        }

        override fun classifier(): Any = filter.javaClass
    }
    private object Deferred : DatabaseActionRule.Deferred<IsEmployee> {
        override fun evaluate(params: IsEmployee): AccessControlDecision = AccessControlDecision.Permitted(params)
    }

    fun ownerOfMobileDevice() = DatabaseActionRule(
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

    fun ownerOfPairing() = DatabaseActionRule(
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

    fun authorOfVasuDocumentFollowupEntry() = DatabaseActionRule(
        this,
        Query<VasuDocumentFollowupEntryId> { tx, user, ids ->
            // TODO: replace naive loop with a batch operation
            ids.filter { id -> tx.getVasuFollowupEntry(id).authorId == user.id }
        }
    )

    fun authorOfApplicationNote() = DatabaseActionRule(
        this,
        Query<ApplicationNoteId> { tx, user, ids ->
            tx.createQuery("SELECT id FROM application_note WHERE created_by = :userId AND id = ANY(:ids)")
                .bind("userId", user.id)
                .bind("ids", ids.toTypedArray())
                .mapTo()
        }
    )

    fun hasPermissionForMessageDraft() = DatabaseActionRule(
        this,
        Query<MessageDraftId> { tx, employee, ids -> tx.filterPermittedMessageDrafts(employee, ids) }
    )

    fun hasPermissionForAttachmentThroughMessageContent() = DatabaseActionRule(
        this,
        Query<AttachmentId> { tx, employee, ids -> tx.filterPermittedAttachmentsThroughMessageContent(employee, ids) }
    )

    fun hasPermissionForAttachmentThroughMessageDraft() = DatabaseActionRule(
        this,
        Query<AttachmentId> { tx, employee, ids -> tx.filterPermittedAttachmentsThroughMessageDrafts(employee, ids) }
    )
}
