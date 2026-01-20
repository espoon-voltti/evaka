// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.holidayperiod.QuestionnaireType
import java.time.MonthDay

data class FeatureConfig(
    /**
     * Does capacity factor affect voucher value decisions?
     *
     * If true, voucher value decision's value is multiplied by child's capacity factor. If false,
     * value is not multiplied.
     */
    val valueDecisionCapacityFactorEnabled: Boolean,

    /**
     * How many hours before the start of monday the week's reservations are locked
     *
     * For example
     * - 168 means one week before (168 = 7 * 24)
     * - 150 means previous week's monday at 18:00 (150 = 7 * 24 - 18)
     */
    val citizenReservationThresholdHours: Long,

    /** Controls whether FREE_ABSENCE absences give a daily refund on invoices or not */
    val freeAbsenceGivesADailyRefund: Boolean,

    /**
     * Whether to always use the marked daycare finance decision handler
     *
     * If true, regardless of situation use the handler marked in the daycare settings as the
     * handler of a decision. If false, use the current user as the handler as speficied in the code
     */
    val alwaysUseDaycareFinanceDecisionHandler: Boolean,

    /**
     * The first payment number to use
     *
     * The number of subsequent payments is the previous number plus one. If null, payments cannot
     * be generated.
     */
    val paymentNumberSeriesStart: Long?,

    /**
     * Controls whether unplanned absences are counted as surplus contract days on invoices or not
     */
    val unplannedAbsencesAreContractSurplusDays: Boolean,

    /**
     * Optionally set a threshold of surplus contract days after which the monthly maximum is
     * invoiced.
     */
    val maxContractDaySurplusThreshold: Int?,

    /**
     * Controls whether to use the number of contract days or the number of operational days as
     * daily price divisor
     */
    val useContractDaysAsDailyFeeDivisor: Boolean,

    /** The number of days citizens can move daycare start forward */
    val requestedStartUpperLimit: Int,

    /** Post office for the city. Used for external children filter in finance decision searches */
    val postOffice: String,

    /** Name of the message sender when sending messages on the municipal message account */
    val municipalMessageAccountName: String,

    /** Name of the message sender when sending messages on the service worker message account */
    val serviceWorkerMessageAccountName: String,

    /** Name of the message sender when sending messages on the finance message account */
    val financeMessageAccountName: String,

    /**
     * true = placement unit is resolved from decision when it's accepted, false = placement unit is
     * resolved from placement plan
     */
    val applyPlacementUnitFromDecision: Boolean,

    /**
     * Whether automatic application due date is calculated in relation to the preferred start date
     * true = non-urgent early education application due date is max(sentDate + 4 months, preferred
     * start date) false = non-urgent early education application due date is sentDate + 4 months
     */
    val preferredStartRelativeApplicationDueDate: Boolean,

    /** Enable use of special five-year-old daycare placement types */
    val fiveYearsOldDaycareEnabled: Boolean,

    /** Controls whether absences give a daily refund for TEMPORARY_DAYCARE_PART_DAY */
    val temporaryDaycarePartDayAbsenceGivesADailyRefund: Boolean = true,

    /** The name of the organization used in archived metadata */
    val archiveMetadataOrganization: String,

    /** Configs for enabled archive metadata processes */
    val archiveMetadataConfigs: (type: ArchiveProcessType, year: Int) -> ArchiveProcessConfig?,

    /**
     * Whether July is free of charge if daycare started latest on last year's September (normally
     * August)
     */
    val freeJulyStartOnSeptember: Boolean = false,

    /** Default daycare end month-day for new placement plans */
    val daycarePlacementPlanEndMonthDay: MonthDay = MonthDay.of(7, 31),

    /** Status of the applications created by placement tool */
    val placementToolApplicationStatus: ApplicationStatus = ApplicationStatus.SENT,

    /** Type of holiday questionnaire */
    val holidayQuestionnaireType: QuestionnaireType = QuestionnaireType.FIXED_PERIOD,

    /**
     * Invoices whose total sum is less than `minimumInvoiceAmount` are not generated at all. The
     * value is in cents, i.e. 1000 means 10 euros.
     */
    val minimumInvoiceAmount: Int = 0,

    /** Accept preschool decision without asking guardian confirmation */
    val skipGuardianPreschoolDecisionApproval: Boolean = false,
)

enum class ArchiveProcessType {
    APPLICATION_DAYCARE,
    APPLICATION_PRESCHOOL,
    APPLICATION_CLUB,
    FEE_DECISION,
    VOUCHER_VALUE_DECISION;

    companion object {
        fun fromApplicationType(type: ApplicationType): ArchiveProcessType =
            when (type) {
                ApplicationType.DAYCARE -> APPLICATION_DAYCARE
                ApplicationType.PRESCHOOL -> APPLICATION_PRESCHOOL
                ApplicationType.CLUB -> APPLICATION_CLUB
            }
    }
}

data class ArchiveProcessConfig(val processDefinitionNumber: String, val archiveDurationMonths: Int)
