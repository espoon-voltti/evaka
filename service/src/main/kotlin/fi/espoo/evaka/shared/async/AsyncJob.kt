// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.invoicing.service.IncomeNotificationType
import fi.espoo.evaka.koski.KoskiStudyRightKey
import fi.espoo.evaka.sficlient.SfiMessage
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageContentId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageRecipientId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.varda.VardaChildCalculatedServiceNeedChanges
import java.time.Duration
import java.util.UUID
import kotlin.reflect.KClass

data class AsyncJobType<T : AsyncJobPayload>(val payloadClass: KClass<T>) {
    val name: String = payloadClass.simpleName!!

    override fun toString(): String = name

    companion object {
        fun <T : AsyncJobPayload> ofPayload(payload: T): AsyncJobType<T> =
            AsyncJobType(payload.javaClass.kotlin)
    }
}

interface AsyncJobPayload {
    val user: AuthenticatedUser?
}

sealed interface AsyncJob : AsyncJobPayload {
    data class DvvModificationsRefresh(val ssns: List<String>, val requestingUserId: UUID) :
        AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class GarbageCollectPairing(val pairingId: PairingId) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendApplicationEmail(
        val guardianId: PersonId,
        val language: Language,
        val type: ApplicationType = ApplicationType.DAYCARE,
        val sentWithinPreschoolApplicationPeriod: Boolean? = null
    ) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendPendingDecisionEmail(
        val guardianId: PersonId,
        val language: String?,
        val decisionIds: List<DecisionId>
    ) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendMessageNotificationEmail(
        val threadId: MessageThreadId,
        val messageId: MessageId,
        val recipientId: MessageAccountId,
        val messageRecipientId: MessageRecipientId,
        val personId: PersonId,
        val language: Language,
        val urgent: Boolean = false
    ) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendMessagePushNotification(
        val recipient: MessageRecipientId,
        val device: MobileDeviceId
    ) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class UploadToKoski(val key: KoskiStudyRightKey) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class NotifyDecisionCreated(
        val decisionId: DecisionId,
        override val user: AuthenticatedUser,
        val sendAsMessage: Boolean
    ) : AsyncJob

    data class SendDecision(val decisionId: DecisionId) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class NotifyFeeDecisionApproved(val decisionId: FeeDecisionId) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class NotifyFeeDecisionPdfGenerated(val decisionId: FeeDecisionId) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class NotifyVoucherValueDecisionApproved(val decisionId: VoucherValueDecisionId) :
        AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class NotifyVoucherValueDecisionPdfGenerated(val decisionId: VoucherValueDecisionId) :
        AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class InitializeFamilyFromApplication(
        val applicationId: ApplicationId,
        override val user: AuthenticatedUser
    ) : AsyncJob

    @JsonIgnoreProperties("requestingUser") // only present in old jobs
    data class VTJRefresh(val personId: PersonId) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class RunScheduledJob(val job: String) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class NotifyFeeThresholdsUpdated(val dateRange: DateRange) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class GenerateFinanceDecisions
    private constructor(val person: Person, val dateRange: DateRange) : AsyncJob {
        override val user: AuthenticatedUser? = null

        companion object {
            fun forAdult(
                personId: PersonId,
                dateRange: DateRange,
                skipPropagation: Boolean? = false
            ) = GenerateFinanceDecisions(Person.Adult(personId, skipPropagation), dateRange)

            fun forChild(personId: ChildId, dateRange: DateRange) =
                GenerateFinanceDecisions(Person.Child(personId), dateRange)
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
        sealed class Person {
            data class Adult(val adultId: PersonId, val skipPropagation: Boolean? = false) :
                Person()

            data class Child(val childId: ChildId) : Person()
        }
    }

    data class SendPedagogicalDocumentNotificationEmail(
        val pedagogicalDocumentId: PedagogicalDocumentId,
        val recipientId: PersonId,
        val language: Language
    ) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendVasuNotificationEmail(
        val vasuDocumentId: VasuDocumentId,
        val childId: ChildId,
        val recipientId: PersonId,
        val language: Language
    ) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendChildDocumentNotificationEmail(
        val documentId: ChildDocumentId,
        val childId: ChildId,
        val recipientId: PersonId,
        val language: Language
    ) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendPatuReport(val dateRange: DateRange) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendAssistanceNeedDecisionEmail(val decisionId: AssistanceNeedDecisionId) :
        AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class CreateAssistanceNeedDecisionPdf(val decisionId: AssistanceNeedDecisionId) :
        AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendAssistanceNeedDecisionSfiMessage(val decisionId: AssistanceNeedDecisionId) :
        AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class CreateChildDocumentPdf(val documentId: ChildDocumentId) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendAssistanceNeedPreschoolDecisionEmail(
        val decisionId: AssistanceNeedPreschoolDecisionId
    ) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class CreateAssistanceNeedPreschoolDecisionPdf(
        val decisionId: AssistanceNeedPreschoolDecisionId
    ) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendAssistanceNeedPreschoolDecisionSfiMessage(
        val decisionId: AssistanceNeedPreschoolDecisionId
    ) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class UpdateFromVtj(val ssn: String) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendMissingReservationsReminder(val guardian: PersonId, val range: FiniteDateRange) :
        AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendMissingHolidayReservationsReminder(
        val guardian: PersonId,
        val holidayRange: FiniteDateRange
    ) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class UpdateMessageThreadRecipients(
        val threadId: MessageThreadId,
        val recipientIds: Set<MessageAccountId>,
        val sentAt: HelsinkiDateTime
    ) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class MarkMessagesAsSent(
        val messageContentId: MessageContentId,
        val sentAt: HelsinkiDateTime
    ) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendMessage(val message: SfiMessage) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class UpdateVardaChild(
        val serviceNeedDiffByChild: VardaChildCalculatedServiceNeedChanges
    ) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class ResetVardaChild(val childId: ChildId) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class DeleteVardaChildOld(val vardaChildId: Long) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class ResetVardaChildOld(val childId: ChildId) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class DeleteVardaChild(val vardaChildId: Long) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendOutdatedIncomeNotificationEmail(
        val guardianId: PersonId,
        val type: IncomeNotificationType
    ) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendNewCustomerIncomeNotificationEmail(val guardianId: PersonId) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class DeleteAttachment(val attachmentId: AttachmentId) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class MigrateVasuDocument(val documentId: VasuDocumentId) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    companion object {
        val main =
            AsyncJobRunner.Pool(
                AsyncJobPool.Id(AsyncJob::class, "main"),
                AsyncJobPool.Config(concurrency = 4),
                setOf(
                    CreateAssistanceNeedDecisionPdf::class,
                    CreateAssistanceNeedPreschoolDecisionPdf::class,
                    CreateChildDocumentPdf::class,
                    DeleteAttachment::class,
                    DvvModificationsRefresh::class,
                    GarbageCollectPairing::class,
                    GenerateFinanceDecisions::class,
                    InitializeFamilyFromApplication::class,
                    NotifyDecisionCreated::class,
                    NotifyFeeDecisionApproved::class,
                    NotifyFeeDecisionPdfGenerated::class,
                    NotifyFeeThresholdsUpdated::class,
                    NotifyVoucherValueDecisionApproved::class,
                    NotifyVoucherValueDecisionPdfGenerated::class,
                    RunScheduledJob::class,
                    SendAssistanceNeedDecisionSfiMessage::class,
                    SendAssistanceNeedPreschoolDecisionSfiMessage::class,
                    SendDecision::class,
                    SendPatuReport::class,
                    UpdateFromVtj::class,
                    UploadToKoski::class,
                    VTJRefresh::class,
                )
            )
        val email =
            AsyncJobRunner.Pool(
                AsyncJobPool.Id(AsyncJob::class, "email"),
                AsyncJobPool.Config(concurrency = 1),
                setOf(
                    SendApplicationEmail::class,
                    SendAssistanceNeedDecisionEmail::class,
                    SendAssistanceNeedPreschoolDecisionEmail::class,
                    SendChildDocumentNotificationEmail::class,
                    SendMessageNotificationEmail::class,
                    SendMissingReservationsReminder::class,
                    SendMissingHolidayReservationsReminder::class,
                    SendOutdatedIncomeNotificationEmail::class,
                    SendPedagogicalDocumentNotificationEmail::class,
                    SendPendingDecisionEmail::class,
                    SendVasuNotificationEmail::class,
                    SendNewCustomerIncomeNotificationEmail::class,
                )
            )
        val urgent =
            AsyncJobRunner.Pool(
                AsyncJobPool.Id(AsyncJob::class, "urgent"),
                AsyncJobPool.Config(concurrency = 4),
                setOf(
                    MarkMessagesAsSent::class,
                    SendMessagePushNotification::class,
                    UpdateMessageThreadRecipients::class,
                )
            )
        val suomiFi =
            AsyncJobRunner.Pool(
                AsyncJobPool.Id(AsyncJob::class, "suomiFi"),
                AsyncJobPool.Config(concurrency = 1),
                setOf(SendMessage::class)
            )
        val varda =
            AsyncJobRunner.Pool(
                AsyncJobPool.Id(AsyncJob::class, "varda"),
                AsyncJobPool.Config(concurrency = 1),
                setOf(
                    UpdateVardaChild::class,
                    ResetVardaChild::class,
                    DeleteVardaChild::class,
                    ResetVardaChildOld::class,
                    DeleteVardaChildOld::class,
                )
            )
        val vasuMigration =
            AsyncJobRunner.Pool(
                AsyncJobPool.Id(AsyncJob::class, "vasuMigration"),
                AsyncJobPool.Config(concurrency = 1),
                setOf(MigrateVasuDocument::class)
            )
    }
}

data class JobParams<T : AsyncJobPayload>(
    val payload: T,
    val retryCount: Int,
    val retryInterval: Duration,
    val runAt: HelsinkiDateTime
)

data class ClaimedJobRef<T : AsyncJobPayload>(
    val jobId: UUID,
    val jobType: AsyncJobType<T>,
    val txId: Long,
    val remainingAttempts: Int
)

data class WorkPermit(val availableAt: HelsinkiDateTime)
