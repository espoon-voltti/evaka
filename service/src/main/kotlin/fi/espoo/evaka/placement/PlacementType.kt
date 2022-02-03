// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.daycare.service.AbsenceCategory

enum class PlacementType {
    CLUB,
    DAYCARE,
    DAYCARE_PART_TIME,
    DAYCARE_FIVE_YEAR_OLDS,
    DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
    PRESCHOOL,
    PRESCHOOL_DAYCARE,
    PREPARATORY,
    PREPARATORY_DAYCARE,
    TEMPORARY_DAYCARE,
    TEMPORARY_DAYCARE_PART_DAY,
    SCHOOL_SHIFT_CARE;

    fun isInvoiced(): Boolean = when (this) {
        CLUB -> false
        DAYCARE -> true
        DAYCARE_PART_TIME -> true
        DAYCARE_FIVE_YEAR_OLDS -> true
        DAYCARE_PART_TIME_FIVE_YEAR_OLDS -> true
        PRESCHOOL -> false
        PRESCHOOL_DAYCARE -> true
        PREPARATORY -> false
        PREPARATORY_DAYCARE -> true
        TEMPORARY_DAYCARE -> true
        TEMPORARY_DAYCARE_PART_DAY -> true
        SCHOOL_SHIFT_CARE -> false
    }

    fun absenceCategories(): Set<AbsenceCategory> = when (this) {
        PRESCHOOL, PREPARATORY, CLUB, SCHOOL_SHIFT_CARE -> setOf(AbsenceCategory.NONBILLABLE)
        DAYCARE, DAYCARE_PART_TIME, TEMPORARY_DAYCARE, TEMPORARY_DAYCARE_PART_DAY -> setOf(AbsenceCategory.BILLABLE)
        PRESCHOOL_DAYCARE, PREPARATORY_DAYCARE,
        DAYCARE_FIVE_YEAR_OLDS, DAYCARE_PART_TIME_FIVE_YEAR_OLDS -> setOf(
            AbsenceCategory.BILLABLE,
            AbsenceCategory.NONBILLABLE
        )
    }

    companion object {
        fun invoiced() = values().filter { it.isInvoiced() }
    }
}
