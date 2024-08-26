// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.daycare.ClubTerm
import fi.espoo.evaka.daycare.PreschoolTerm
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.TimeRange
import java.time.LocalDate

enum class PlacementType : DatabaseEnum {
    CLUB,
    DAYCARE,
    DAYCARE_PART_TIME,
    DAYCARE_FIVE_YEAR_OLDS,
    DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
    PRESCHOOL,
    PRESCHOOL_DAYCARE,
    PRESCHOOL_DAYCARE_ONLY,
    PRESCHOOL_CLUB,
    PREPARATORY,
    PREPARATORY_DAYCARE,
    PREPARATORY_DAYCARE_ONLY,
    TEMPORARY_DAYCARE,
    TEMPORARY_DAYCARE_PART_DAY,
    SCHOOL_SHIFT_CARE;

    fun isRelevantToKoski(): Boolean =
        when (this) {
            CLUB,
            DAYCARE,
            DAYCARE_PART_TIME,
            DAYCARE_FIVE_YEAR_OLDS,
            DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
            PRESCHOOL_DAYCARE_ONLY,
            PREPARATORY_DAYCARE_ONLY,
            TEMPORARY_DAYCARE,
            TEMPORARY_DAYCARE_PART_DAY,
            SCHOOL_SHIFT_CARE -> false
            PRESCHOOL,
            PRESCHOOL_DAYCARE,
            PRESCHOOL_CLUB,
            PREPARATORY,
            PREPARATORY_DAYCARE -> true
        }

    fun isInvoiced(): Boolean =
        when (this) {
            CLUB -> false
            DAYCARE -> true
            DAYCARE_PART_TIME -> true
            DAYCARE_FIVE_YEAR_OLDS -> true
            DAYCARE_PART_TIME_FIVE_YEAR_OLDS -> true
            PRESCHOOL -> false
            PRESCHOOL_DAYCARE -> true
            PRESCHOOL_DAYCARE_ONLY -> true
            PRESCHOOL_CLUB -> true
            PREPARATORY -> false
            PREPARATORY_DAYCARE -> true
            PREPARATORY_DAYCARE_ONLY -> true
            TEMPORARY_DAYCARE -> true
            TEMPORARY_DAYCARE_PART_DAY -> true
            SCHOOL_SHIFT_CARE -> false
        }

    fun absenceCategories(): Set<AbsenceCategory> =
        when (this) {
            PRESCHOOL,
            PREPARATORY,
            CLUB,
            SCHOOL_SHIFT_CARE -> setOf(AbsenceCategory.NONBILLABLE)
            DAYCARE,
            DAYCARE_PART_TIME,
            PRESCHOOL_DAYCARE_ONLY,
            PREPARATORY_DAYCARE_ONLY,
            TEMPORARY_DAYCARE,
            TEMPORARY_DAYCARE_PART_DAY -> setOf(AbsenceCategory.BILLABLE)
            PRESCHOOL_DAYCARE,
            PRESCHOOL_CLUB,
            PREPARATORY_DAYCARE,
            DAYCARE_FIVE_YEAR_OLDS,
            DAYCARE_PART_TIME_FIVE_YEAR_OLDS ->
                setOf(AbsenceCategory.BILLABLE, AbsenceCategory.NONBILLABLE)
        }

    fun scheduleType(
        date: LocalDate,
        clubTerms: List<ClubTerm>,
        preschoolTerms: List<PreschoolTerm>,
    ): ScheduleType =
        when (this) {
            CLUB ->
                clubTerms.firstNotNullOfOrNull { it.scheduleType(date) } ?: ScheduleType.TERM_BREAK
            DAYCARE -> ScheduleType.RESERVATION_REQUIRED
            DAYCARE_PART_TIME -> ScheduleType.RESERVATION_REQUIRED
            DAYCARE_FIVE_YEAR_OLDS -> ScheduleType.RESERVATION_REQUIRED
            DAYCARE_PART_TIME_FIVE_YEAR_OLDS -> ScheduleType.RESERVATION_REQUIRED
            // Fall back to FIXED_SCHEDULE outside preschool terms for situations where placement's
            // start/end is outside the term. This might happen in Espoo if swedish terms differ
            // from finnish terms, for example.
            PRESCHOOL ->
                preschoolTerms.firstNotNullOfOrNull { it.scheduleType(date) }
                    ?: ScheduleType.FIXED_SCHEDULE
            PREPARATORY ->
                preschoolTerms.firstNotNullOfOrNull { it.scheduleType(date) }
                    ?: ScheduleType.FIXED_SCHEDULE
            PRESCHOOL_DAYCARE -> ScheduleType.RESERVATION_REQUIRED
            PRESCHOOL_DAYCARE_ONLY -> ScheduleType.RESERVATION_REQUIRED
            PRESCHOOL_CLUB -> ScheduleType.RESERVATION_REQUIRED
            PREPARATORY_DAYCARE -> ScheduleType.RESERVATION_REQUIRED
            PREPARATORY_DAYCARE_ONLY -> ScheduleType.RESERVATION_REQUIRED
            TEMPORARY_DAYCARE -> ScheduleType.RESERVATION_REQUIRED
            TEMPORARY_DAYCARE_PART_DAY -> ScheduleType.RESERVATION_REQUIRED
            SCHOOL_SHIFT_CARE -> ScheduleType.RESERVATION_REQUIRED
        }

    fun fixedScheduleRange(
        dailyPreschoolTime: TimeRange?,
        dailyPreparatoryTime: TimeRange?,
    ): TimeRange? =
        when (this) {
            CLUB,
            DAYCARE,
            DAYCARE_PART_TIME,
            DAYCARE_FIVE_YEAR_OLDS,
            DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
            TEMPORARY_DAYCARE,
            TEMPORARY_DAYCARE_PART_DAY,
            SCHOOL_SHIFT_CARE -> null
            PRESCHOOL,
            PRESCHOOL_DAYCARE,
            PRESCHOOL_CLUB,
            PRESCHOOL_DAYCARE_ONLY -> dailyPreschoolTime
            PREPARATORY,
            PREPARATORY_DAYCARE,
            PREPARATORY_DAYCARE_ONLY -> dailyPreparatoryTime
        }

    fun fixedScheduleOnlyRange(
        date: LocalDate,
        dailyPreschoolTime: TimeRange?,
        dailyPreparatoryTime: TimeRange?,
        preschoolTerms: List<PreschoolTerm>,
    ): TimeRange? =
        when (this) {
            CLUB,
            DAYCARE,
            DAYCARE_PART_TIME,
            DAYCARE_FIVE_YEAR_OLDS,
            DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
            TEMPORARY_DAYCARE,
            TEMPORARY_DAYCARE_PART_DAY,
            SCHOOL_SHIFT_CARE,
            PRESCHOOL_DAYCARE,
            PRESCHOOL_CLUB,
            PRESCHOOL_DAYCARE_ONLY,
            PREPARATORY_DAYCARE,
            PREPARATORY_DAYCARE_ONLY -> null
            PRESCHOOL ->
                if (
                    preschoolTerms.firstNotNullOfOrNull { it.scheduleType(date) } ==
                        ScheduleType.FIXED_SCHEDULE
                )
                    dailyPreschoolTime
                else null
            PREPARATORY ->
                if (
                    preschoolTerms.firstNotNullOfOrNull { it.scheduleType(date) } ==
                        ScheduleType.FIXED_SCHEDULE
                )
                    dailyPreparatoryTime
                else null
        }

    override val sqlType: String = "placement_type"

    companion object {
        val temporary = listOf(TEMPORARY_DAYCARE, TEMPORARY_DAYCARE_PART_DAY)
        val invoiced = entries.filter { it.isInvoiced() }.filterNot { temporary.contains(it) }
        val requiringAttendanceReservations =
            entries.filter { it != CLUB && it != PRESCHOOL && it != PREPARATORY }
        val withBillableAbsences =
            entries.filter { it.absenceCategories().contains(AbsenceCategory.BILLABLE) }
        val withNonbillableAbsences =
            entries.filter { it.absenceCategories().contains(AbsenceCategory.NONBILLABLE) }
    }
}

enum class ScheduleType {
    RESERVATION_REQUIRED, // Daycare -> reservations required
    FIXED_SCHEDULE, // Preschool/club -> reservations not required
    TERM_BREAK, // Preschool/club term break -> no activity
}
