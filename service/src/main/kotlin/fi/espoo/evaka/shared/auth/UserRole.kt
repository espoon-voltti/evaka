// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.shared.db.DatabaseEnum

enum class UserRole : DatabaseEnum {
    END_USER,
    CITIZEN_WEAK,
    ADMIN,
    REPORT_VIEWER,
    DIRECTOR,
    FINANCE_ADMIN,
    FINANCE_STAFF,
    SERVICE_WORKER,
    MESSAGING,
    UNIT_SUPERVISOR,
    STAFF,
    SPECIAL_EDUCATION_TEACHER,

    /** Varhaiskasvatussihteeri */
    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
    MOBILE,

    @Deprecated("Exists only for backwards compatibility")
    GROUP_STAFF;

    fun isGlobalRole(): Boolean =
        when (this) {
            ADMIN -> true
            REPORT_VIEWER -> true
            DIRECTOR -> true
            FINANCE_ADMIN -> true
            SERVICE_WORKER -> true
            MESSAGING -> true
            FINANCE_STAFF -> true
            else -> false
        }

    fun isUnitScopedRole(): Boolean =
        when (this) {
            UNIT_SUPERVISOR -> true
            STAFF -> true
            SPECIAL_EDUCATION_TEACHER -> true
            EARLY_CHILDHOOD_EDUCATION_SECRETARY -> true
            else -> false
        }

    override val sqlType: String = "user_role"

    companion object {
        @Suppress("DEPRECATION")
        val SCOPED_ROLES =
            setOf(
                UNIT_SUPERVISOR,
                STAFF,
                SPECIAL_EDUCATION_TEACHER,
                EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                MOBILE,
                GROUP_STAFF
            )
    }
}
