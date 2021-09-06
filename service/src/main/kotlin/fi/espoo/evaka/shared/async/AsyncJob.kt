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
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.job.ScheduledJob
import java.time.Duration
import java.time.LocalDate
import java.util.UUID
import kotlin.reflect.KClass

data class AsyncJobType<T : AsyncJobPayload>(val payloadClass: KClass<T>) {
    val name: String = payloadClass.simpleName!!
    override fun toString(): String = name

    @Suppress("DEPRECATION")
    fun getAllNames(): List<String> = listOf(name) + when (payloadClass) {
        NotifyServiceNeedUpdated::class -> listOf("SERVICE_NEED_UPDATED")
        NotifyFamilyUpdated::class -> listOf("FAMILY_UPDATED")
        NotifyFeeAlterationUpdated::class -> listOf("FEE_ALTERATION_UPDATED")
        NotifyIncomeUpdated::class -> listOf("INCOME_UPDATED")
        NotifyDecisionCreated::class -> listOf("DECISION_CREATED")
        SendDecision::class -> listOf("SEND_DECISION")
        NotifyPlacementPlanApplied::class -> listOf("PLACEMENT_PLAN_APPLIED")
        NotifyFeeDecisionApproved::class -> listOf("FEE_DECISION_APPROVED")
        NotifyFeeDecisionPdfGenerated::class -> listOf("FEE_DECISION_PDF_GENERATED")
        NotifyVoucherValueDecisionApproved::class -> listOf("VOUCHER_VALUE_DECISION_APPROVED")
        NotifyVoucherValueDecisionPdfGenerated::class -> listOf("VOUCHER_VALUE_DECISION_PDF_GENERATED")
        InitializeFamilyFromApplication::class -> listOf("INITIALIZE_FAMILY_FROM_APPLICATION")
        VTJRefresh::class -> listOf("VTJ_REFRESH")
        DvvModificationsRefresh::class -> listOf("DVV_MODIFICATIONS_REFRESH")
        UploadToKoski::class -> listOf("UPLOAD_TO_KOSKI")
        ScheduleKoskiUploads::class -> listOf("SCHEDULE_KOSKI_UPLOADS")
        SendApplicationEmail::class -> listOf("SEND_APPLICATION_EMAIL")
        GarbageCollectPairing::class -> listOf("GARBAGE_COLLECT_PAIRING")
        VardaUpdate::class -> listOf("VARDA_UPDATE")
        VardaUpdateV2::class -> listOf("VARDA_UPDATE_V2")
        SendPendingDecisionEmail::class -> listOf("SEND_PENDING_DECISION_EMAIL")
        SendMessageNotificationEmail::class -> listOf("SEND_UNREAD_MESSAGE_NOTIFICATION")
        RunScheduledJob::class -> listOf("RUN_SCHEDULED_JOB")
        NotifyFeeThresholdsUpdated::class -> listOf("FEE_THRESHOLDS_UPDATED")
        GenerateFinanceDecisions::class -> listOf("GENERATE_FINANCE_DECISIONS")
        else -> emptyList()
    }

    companion object {
        fun <T : AsyncJobPayload> ofPayload(payload: T): AsyncJobType<T> = AsyncJobType(payload.javaClass.kotlin)
    }
}

sealed interface AsyncJobPayload {
    val user: AuthenticatedUser?
}

data class DvvModificationsRefresh(val ssns: List<String>, val requestingUserId: UUID) : AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

data class GarbageCollectPairing(val pairingId: PairingId) : AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

data class SendApplicationEmail(val guardianId: UUID, val language: Language, val type: ApplicationType = ApplicationType.DAYCARE, val sentWithinPreschoolApplicationPeriod: Boolean? = null) : AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

data class SendPendingDecisionEmail(val guardianId: UUID, val email: String, val language: String?, val decisionIds: List<UUID>) : AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

data class SendMessageNotificationEmail(val messageRecipientId: UUID, val personEmail: String, val language: Language) : AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

data class ScheduleKoskiUploads(val params: KoskiSearchParams) : AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

data class UploadToKoski(val key: KoskiStudyRightKey) : AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

@Deprecated("Use GenerateFinanceDecisions instead")
data class NotifyPlacementPlanApplied(val childId: UUID, val startDate: LocalDate, val endDate: LocalDate) :
    AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

@Deprecated("Use GenerateFinanceDecisions instead")
data class NotifyServiceNeedUpdated(val childId: UUID, val startDate: LocalDate, val endDate: LocalDate?) :
    AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

@Deprecated("Use GenerateFinanceDecisions instead")
data class NotifyFamilyUpdated(
    val adultId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate?
) : AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

@Deprecated("Use GenerateFinanceDecisions instead")
data class NotifyFeeAlterationUpdated(
    val personId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate?
) : AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

@Deprecated("Use GenerateFinanceDecisions instead")
data class NotifyIncomeUpdated(
    val personId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate?
) : AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

data class NotifyDecisionCreated(val decisionId: DecisionId, override val user: AuthenticatedUser, val sendAsMessage: Boolean) : AsyncJobPayload

data class SendDecision(
    val decisionId: DecisionId,
    @Deprecated(message = "only for backwards compatibility")
    override val user: AuthenticatedUser? = null
) : AsyncJobPayload

data class NotifyFeeDecisionApproved(val decisionId: FeeDecisionId) : AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

data class NotifyFeeDecisionPdfGenerated(val decisionId: FeeDecisionId) : AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

data class NotifyVoucherValueDecisionApproved(val decisionId: VoucherValueDecisionId) : AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

data class NotifyVoucherValueDecisionPdfGenerated(val decisionId: VoucherValueDecisionId) : AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

data class InitializeFamilyFromApplication(val applicationId: ApplicationId, override val user: AuthenticatedUser) :
    AsyncJobPayload

data class VTJRefresh(val personId: UUID, val requestingUserId: UUID) : AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

class VardaUpdate : AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

class VardaUpdateV2 : AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

data class RunScheduledJob(val job: ScheduledJob) : AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

data class NotifyFeeThresholdsUpdated(val dateRange: DateRange) : AsyncJobPayload {
    override val user: AuthenticatedUser? = null
}

data class GenerateFinanceDecisions private constructor(val person: Person, val dateRange: DateRange) :
    AsyncJobPayload {
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

data class JobParams<T : AsyncJobPayload>(
    val payload: T,
    val retryCount: Int,
    val retryInterval: Duration,
    val runAt: HelsinkiDateTime
)

data class ClaimedJobRef<T : AsyncJobPayload>(val jobId: UUID, val jobType: AsyncJobType<T>, val txId: Long, val remainingAttempts: Int)
