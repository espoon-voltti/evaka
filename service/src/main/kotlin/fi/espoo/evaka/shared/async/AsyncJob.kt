// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import com.fasterxml.jackson.annotation.JsonTypeInfo
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.koski.KoskiSearchParams
import fi.espoo.evaka.koski.KoskiStudyRightKey
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.job.ScheduledJob
import fi.espoo.evaka.varda.VardaChildCalculatedServiceNeedChanges
import java.time.Duration
import java.util.UUID
import kotlin.reflect.KClass

data class AsyncJobType<T : AsyncJobPayload>(val payloadClass: KClass<T>) {
    val name: String = payloadClass.simpleName!!
    override fun toString(): String = name

    companion object {
        fun <T : AsyncJobPayload> ofPayload(payload: T): AsyncJobType<T> = AsyncJobType(payload.javaClass.kotlin)
    }
}

interface AsyncJobPayload {
    val user: AuthenticatedUser?
}

sealed interface VardaAsyncJob : AsyncJobPayload {
    data class UpdateVardaChild(
        val serviceNeedDiffByChild: VardaChildCalculatedServiceNeedChanges
    ) : VardaAsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class ResetVardaChild(
        val childId: UUID
    ) : VardaAsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class DeleteVardaChild(
        val vardaChildId: Long
    ) : VardaAsyncJob {
        override val user: AuthenticatedUser? = null
    }
}

sealed interface AsyncJob : AsyncJobPayload {
    data class DvvModificationsRefresh(val ssns: List<String>, val requestingUserId: UUID) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class GarbageCollectPairing(val pairingId: PairingId) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendApplicationEmail(val guardianId: UUID, val language: Language, val type: ApplicationType = ApplicationType.DAYCARE, val sentWithinPreschoolApplicationPeriod: Boolean? = null) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendPendingDecisionEmail(val guardianId: UUID, val email: String, val language: String?, val decisionIds: List<UUID>) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class SendMessageNotificationEmail(val messageRecipientId: MessageAccountId, val personEmail: String, val language: Language) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class ScheduleKoskiUploads(val params: KoskiSearchParams) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class UploadToKoski(val key: KoskiStudyRightKey) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class NotifyDecisionCreated(val decisionId: DecisionId, override val user: AuthenticatedUser, val sendAsMessage: Boolean) : AsyncJob

    data class SendDecision(val decisionId: DecisionId) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class NotifyFeeDecisionApproved(val decisionId: FeeDecisionId) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class NotifyFeeDecisionPdfGenerated(val decisionId: FeeDecisionId) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class NotifyVoucherValueDecisionApproved(val decisionId: VoucherValueDecisionId) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class NotifyVoucherValueDecisionPdfGenerated(val decisionId: VoucherValueDecisionId) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class InitializeFamilyFromApplication(val applicationId: ApplicationId, override val user: AuthenticatedUser) :
        AsyncJob

    data class VTJRefresh(val personId: UUID, val requestingUserId: UUID) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class RunScheduledJob(val job: ScheduledJob) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class NotifyFeeThresholdsUpdated(val dateRange: DateRange) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class GenerateFinanceDecisions private constructor(val person: Person, val dateRange: DateRange) :
        AsyncJob {
        override val user: AuthenticatedUser? = null

        companion object {
            fun forAdult(personId: UUID, dateRange: DateRange) = GenerateFinanceDecisions(Person.Adult(personId), dateRange)
            fun forChild(personId: UUID, dateRange: DateRange) = GenerateFinanceDecisions(Person.Child(personId), dateRange)
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
        sealed class Person {
            data class Adult(val adultId: UUID) : Person()
            data class Child(val childId: UUID) : Person()
        }
    }

    data class SendPedagogicalDocumentNotificationEmail(val pedagogicalDocumentId: PedagogicalDocumentId, val recipientEmail: String, val language: Language) : AsyncJob {
        override val user: AuthenticatedUser? = null
    }
}

data class JobParams<T : AsyncJobPayload>(
    val payload: T,
    val retryCount: Int,
    val retryInterval: Duration,
    val runAt: HelsinkiDateTime
)

data class ClaimedJobRef<T : AsyncJobPayload>(val jobId: UUID, val jobType: AsyncJobType<T>, val txId: Long, val remainingAttempts: Int)
