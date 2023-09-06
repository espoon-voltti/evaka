// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.daycare.ClubTerm
import fi.espoo.evaka.daycare.PreschoolTerm
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.shared.db.DatabaseEnum
import java.time.LocalDate

enum class PlacementType : DatabaseEnum {
    CLUB,
    DAYCARE,
    DAYCARE_PART_TIME,
    DAYCARE_FIVE_YEAR_OLDS,
    DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
    PRESCHOOL,
    PRESCHOOL_DAYCARE,
    PRESCHOOL_CLUB,
    PREPARATORY,
    PREPARATORY_DAYCARE,
    TEMPORARY_DAYCARE,
    TEMPORARY_DAYCARE_PART_DAY,
    SCHOOL_SHIFT_CARE;

    fun isInvoiced(): Boolean =
        when (this) {
            CLUB -> false
            DAYCARE -> true
            DAYCARE_PART_TIME -> true
            DAYCARE_FIVE_YEAR_OLDS -> true
            DAYCARE_PART_TIME_FIVE_YEAR_OLDS -> true
            PRESCHOOL -> false
            PRESCHOOL_DAYCARE -> true
            PRESCHOOL_CLUB -> true
            PREPARATORY -> false
            PREPARATORY_DAYCARE -> true
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
            CLUB -> clubTerms.firstNotNullOfOrNull { it.scheduleType(date) }
                    ?: ScheduleType.TERM_BREAK
            DAYCARE -> ScheduleType.RESERVATION_REQUIRED
            DAYCARE_PART_TIME -> ScheduleType.RESERVATION_REQUIRED
            DAYCARE_FIVE_YEAR_OLDS -> ScheduleType.RESERVATION_REQUIRED
            DAYCARE_PART_TIME_FIVE_YEAR_OLDS -> ScheduleType.RESERVATION_REQUIRED
            PRESCHOOL -> preschoolTerms.firstNotNullOfOrNull { it.scheduleType(date) }
                    ?: ScheduleType.TERM_BREAK
            PRESCHOOL_DAYCARE -> ScheduleType.RESERVATION_REQUIRED
            PRESCHOOL_CLUB -> ScheduleType.RESERVATION_REQUIRED
            PREPARATORY -> preschoolTerms.firstNotNullOfOrNull { it.scheduleType(date) }
                    ?: ScheduleType.TERM_BREAK
            PREPARATORY_DAYCARE -> ScheduleType.RESERVATION_REQUIRED
            TEMPORARY_DAYCARE -> ScheduleType.RESERVATION_REQUIRED
            TEMPORARY_DAYCARE_PART_DAY -> ScheduleType.RESERVATION_REQUIRED
            SCHOOL_SHIFT_CARE -> ScheduleType.RESERVATION_REQUIRED
        }

    override val sqlType: String = "placement_type"

    companion object {
        val temporary = listOf(TEMPORARY_DAYCARE, TEMPORARY_DAYCARE_PART_DAY)
        val invoiced = values().filter { it.isInvoiced() }.filterNot { temporary.contains(it) }
        val requiringAttendanceReservations =
            values().filter { it != CLUB && it != PRESCHOOL && it != PREPARATORY }
    }
}

enum class ScheduleType {
    RESERVATION_REQUIRED, // Daycare -> reservations required
    FIXED_SCHEDULE, // Preschool/club -> reservations not required
    TERM_BREAK // Preschool/club term break -> no activity
}
