// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

data class FeatureConfig(
    /** Does capacity factor affect voucher value decisions?
     *
     * If true, voucher value decision's value is multiplied by child's capacity factor.
     * If false, value is not multiplied.
     */
    val valueDecisionCapacityFactorEnabled: Boolean,

    /** Whether to show service needs on application form */
    val daycareApplicationServiceNeedOptionsEnabled: Boolean,

    /** How many hours before the start of monday the week's reservations are locked
     *
     * For example
     * - 168 means one week before (168 = 7 * 24)
     * - 150 means previous week's monday at 18:00 (150 = 7 * 24 - 18)
     * */
    val citizenReservationThresholdHours: Long,

    /** Use a fixed divisor for calculating a daily daycare price.
     *
     * Daily daycare price is used for partial month invoices, daily refunds, etc. By default, it's computed by
     * dividing the daycare fee by the number of operating days in a month. For example, Decemper 2021 had 21 operating
     * days (31 days minus 9 days of weekends, independence day and Christmas eve).
     *
     * If this this option is not null, the daily daycare price is calculated by dividing by its value regardless of
     * the month's actual operating days.
     */
    val dailyFeeDivisorOperationalDaysOverride: Int?,

    /** Whether a full month of sick leaves and planned absences give a free month
     *
     * By default, sick leaves make a month free of charge only if all the operating days in the month are marked as
     * sick leaves. If this option is true, a month is free of charge also if there's at least one sick leave day
     * and all the operating days are marked either sick leaves or planned absences.
     */
    val freeSickLeaveOnContractDays: Boolean,
)
