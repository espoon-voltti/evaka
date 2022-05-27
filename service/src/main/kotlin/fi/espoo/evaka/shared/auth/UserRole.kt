// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

enum class UserRole {
    END_USER,
    CITIZEN_WEAK,

    ADMIN,
    REPORT_VIEWER,
    DIRECTOR,
    FINANCE_ADMIN,
    SERVICE_WORKER,

    UNIT_SUPERVISOR,
    STAFF,
    SPECIAL_EDUCATION_TEACHER,
    MOBILE,

    @Deprecated("Exists only for backwards compatibility")
    GROUP_STAFF;

    fun isGlobalRole(): Boolean = when (this) {
        ADMIN -> true
        REPORT_VIEWER -> true
        DIRECTOR -> true
        FINANCE_ADMIN -> true
        SERVICE_WORKER -> true
        else -> false
    }
    fun isUnitScopedRole(): Boolean = when (this) {
        UNIT_SUPERVISOR -> true
        STAFF -> true
        SPECIAL_EDUCATION_TEACHER -> true
        else -> false
    }
    companion object {
        @Suppress("DEPRECATION")
        val SCOPED_ROLES = setOf(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE, GROUP_STAFF)
    }
}
