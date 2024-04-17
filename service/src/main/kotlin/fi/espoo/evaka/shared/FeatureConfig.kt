// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import fi.espoo.evaka.shared.auth.UserRole

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

    /**
     * Use a fixed divisor for calculating a daily daycare price.
     *
     * Daily daycare price is used for partial month invoices, daily refunds, etc. By default, it's
     * computed by dividing the daycare fee by the number of operating days in a month. For example,
     * December 2021 had 21 operating days (31 days minus 9 days of weekends, independence day and
     * Christmas Eve).
     *
     * If this option is not null, the daily daycare price is calculated by dividing by its value
     * regardless of the month's actual operating days.
     */
    val dailyFeeDivisorOperationalDaysOverride: Int?,

    /**
     * Whether a full month of sick leaves and planned absences give a free month
     *
     * By default, sick leaves make a month free of charge only if all the operating days in the
     * month are marked as sick leaves. If this option is true, a month is free of charge also if
     * there's at least one sick leave day and all the operating days are marked either sick leaves
     * or planned absences.
     */
    val freeSickLeaveOnContractDays: Boolean,

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
     * The first invoice number to use
     *
     * The number of subsequent invoices is the previous number plus one.
     */
    val invoiceNumberSeriesStart: Long,

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

    /** Controls whether permission to share is required for curriculum documents */
    val curriculumDocumentPermissionToShareRequired: Boolean,

    /**
     * Employees with given user roles to show as options for assistance decision makers.
     *
     * May contain global and unit scoped roles (null = all roles are visible).
     */
    val assistanceDecisionMakerRoles: Set<UserRole>?,

    /**
     * Employees with given user roles to show as options for preschool assistance decision makers.
     *
     * May contain global and unit scoped roles (null = all roles are visible).
     */
    val preschoolAssistanceDecisionMakerRoles: Set<UserRole>?,

    /** The number of days citizens can move daycare start forward */
    val requestedStartUpperLimit: Int,

    /** Post office for the city. Used for external children filter in finance decision searches */
    val postOffice: String,

    /** Name of the message sender when sending messages on the municipal message account */
    val municipalMessageAccountName: String,

    /** Name of the message sender when sending messages on the service worker message account */
    val serviceWorkerMessageAccountName: String,

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
)
