// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.shared.db.DatabaseEnum

enum class UserRole : DatabaseEnum {
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
    EARLY_CHILDHOOD_EDUCATION_SECRETARY;

    fun isGlobalRole(): Boolean = GLOBAL_ROLES.contains(this)

    fun isUnitScopedRole(): Boolean = SCOPED_ROLES.contains(this)

    override val sqlType: String = "user_role"

    companion object {
        val SCOPED_ROLES =
            setOf(
                UNIT_SUPERVISOR,
                STAFF,
                SPECIAL_EDUCATION_TEACHER,
                EARLY_CHILDHOOD_EDUCATION_SECRETARY,
            )
        val GLOBAL_ROLES = entries.filter { !SCOPED_ROLES.contains(it) }.toSet()
    }
}
