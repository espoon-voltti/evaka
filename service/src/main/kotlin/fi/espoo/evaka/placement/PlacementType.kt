// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.shared.db.DatabaseEnum

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

    fun requiresAttendanceReservations(): Boolean =
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
            SCHOOL_SHIFT_CARE -> true
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

    override val sqlType: String = "placement_type"

    companion object {
        val temporary = listOf(TEMPORARY_DAYCARE, TEMPORARY_DAYCARE_PART_DAY)
        val invoiced = values().filter { it.isInvoiced() }.filterNot { temporary.contains(it) }
        val requiringAttendanceReservations =
            values().filter { it.requiresAttendanceReservations() }
    }
}
