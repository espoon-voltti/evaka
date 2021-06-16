// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import com.fasterxml.jackson.annotation.JsonTypeInfo
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.koski.KoskiSearchParams
import fi.espoo.evaka.koski.KoskiStudyRightKey
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.daily.DailyJob
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

enum class AsyncJobType {
    SERVICE_NEED_UPDATED,
    FAMILY_UPDATED,
    FEE_ALTERATION_UPDATED,
    INCOME_UPDATED,
    DECISION_CREATED,
    SEND_DECISION,
    PLACEMENT_PLAN_APPLIED,
    FEE_DECISION_APPROVED,
    FEE_DECISION_PDF_GENERATED,
    VOUCHER_VALUE_DECISION_APPROVED,
    VOUCHER_VALUE_DECISION_PDF_GENERATED,
    INITIALIZE_FAMILY_FROM_APPLICATION,
    VTJ_REFRESH,
    DVV_MODIFICATIONS_REFRESH,
    UPLOAD_TO_KOSKI,
    SCHEDULE_KOSKI_UPLOADS,
    SEND_APPLICATION_EMAIL,
    GARBAGE_COLLECT_PAIRING,
    VARDA_UPDATE,
    VARDA_UPDATE_V2,
    SEND_PENDING_DECISION_EMAIL,
    SEND_UNREAD_MESSAGE_NOTIFICATION,
    RUN_DAILY_JOB,
    FEE_THRESHOLDS_UPDATED,
    GENERATE_FINANCE_DECISIONS
}

interface AsyncJobPayload {
    val asyncJobType: AsyncJobType
    val user: AuthenticatedUser?
}

data class GarbageCollectPairing(val pairingId: UUID) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.GARBAGE_COLLECT_PAIRING
    override val user: AuthenticatedUser? = null
}

data class SendApplicationEmail(val guardianId: UUID, val language: Language, val type: ApplicationType = ApplicationType.DAYCARE, val sentWithinPreschoolApplicationPeriod: Boolean? = null) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.SEND_APPLICATION_EMAIL
    override val user: AuthenticatedUser? = null
}

data class SendPendingDecisionEmail(val guardianId: UUID, val email: String, val language: String?, val decisionIds: List<UUID>) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.SEND_PENDING_DECISION_EMAIL
    override val user: AuthenticatedUser? = null
}

data class SendMessageNotificationEmail(val messageRecipientId: UUID, val personEmail: String, val language: Language) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.SEND_UNREAD_MESSAGE_NOTIFICATION
    override val user: AuthenticatedUser? = null
}

data class ScheduleKoskiUploads(val params: KoskiSearchParams) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.SCHEDULE_KOSKI_UPLOADS
    override val user: AuthenticatedUser? = null
}

data class UploadToKoski(val key: KoskiStudyRightKey) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.UPLOAD_TO_KOSKI
    override val user: AuthenticatedUser? = null
}

@Deprecated("Use GenerateFinanceDecisions instead")
data class NotifyPlacementPlanApplied(val childId: UUID, val startDate: LocalDate, val endDate: LocalDate) :
    AsyncJobPayload {
    override val asyncJobType = AsyncJobType.PLACEMENT_PLAN_APPLIED
    override val user: AuthenticatedUser? = null
}

@Deprecated("Use GenerateFinanceDecisions instead")
data class NotifyServiceNeedUpdated(val childId: UUID, val startDate: LocalDate, val endDate: LocalDate?) :
    AsyncJobPayload {
    override val asyncJobType = AsyncJobType.SERVICE_NEED_UPDATED
    override val user: AuthenticatedUser? = null
}

@Deprecated("Use GenerateFinanceDecisions instead")
data class NotifyFamilyUpdated(
    val adultId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate?
) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.FAMILY_UPDATED
    override val user: AuthenticatedUser? = null
}

@Deprecated("Use GenerateFinanceDecisions instead")
data class NotifyFeeAlterationUpdated(
    val personId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate?
) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.FEE_ALTERATION_UPDATED
    override val user: AuthenticatedUser? = null
}

@Deprecated("Use GenerateFinanceDecisions instead")
data class NotifyIncomeUpdated(
    val personId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate?
) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.INCOME_UPDATED
    override val user: AuthenticatedUser? = null
}

data class NotifyDecisionCreated(val decisionId: UUID, override val user: AuthenticatedUser, val sendAsMessage: Boolean) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.DECISION_CREATED
}

data class SendDecision(
    val decisionId: UUID,
    @Deprecated(message = "only for backwards compatibility")
    override val user: AuthenticatedUser? = null
) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.SEND_DECISION
}

data class NotifyFeeDecisionApproved(val decisionId: UUID) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.FEE_DECISION_APPROVED
    override val user: AuthenticatedUser? = null
}

data class NotifyFeeDecisionPdfGenerated(val decisionId: UUID) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.FEE_DECISION_PDF_GENERATED
    override val user: AuthenticatedUser? = null
}

data class NotifyVoucherValueDecisionApproved(val decisionId: UUID) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.VOUCHER_VALUE_DECISION_APPROVED
    override val user: AuthenticatedUser? = null
}

data class NotifyVoucherValueDecisionPdfGenerated(val decisionId: UUID) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.VOUCHER_VALUE_DECISION_PDF_GENERATED
    override val user: AuthenticatedUser? = null
}

data class InitializeFamilyFromApplication(val applicationId: UUID, override val user: AuthenticatedUser) :
    AsyncJobPayload {
    override val asyncJobType = AsyncJobType.INITIALIZE_FAMILY_FROM_APPLICATION
}

data class VTJRefresh(val personId: UUID, val requestingUserId: UUID) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.VTJ_REFRESH
    override val user: AuthenticatedUser? = null
}

class VardaUpdate : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.VARDA_UPDATE
    override val user: AuthenticatedUser? = null
}

class VardaUpdateV2 : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.VARDA_UPDATE_V2
    override val user: AuthenticatedUser? = null
}

data class RunDailyJob(val dailyJob: DailyJob) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.RUN_DAILY_JOB
    override val user: AuthenticatedUser? = null
}

data class NotifyFeeThresholdsUpdated(val dateRange: DateRange) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.FEE_THRESHOLDS_UPDATED
    override val user: AuthenticatedUser? = null
}

data class GenerateFinanceDecisions private constructor(val person: Person, val dateRange: DateRange) :
    AsyncJobPayload {
    override val asyncJobType = AsyncJobType.GENERATE_FINANCE_DECISIONS
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

data class ClaimedJobRef(val jobId: UUID, val jobType: AsyncJobType, val txId: Long, val remainingAttempts: Int)
