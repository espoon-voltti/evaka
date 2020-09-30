// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import com.fasterxml.jackson.annotation.JsonAlias
import fi.espoo.evaka.shared.domain.Forbidden

enum class UserRole {
    @JsonAlias("ROLE_ENDUSER")
    END_USER,

    @JsonAlias("ROLE_EVAKA_ESPOO_ADMIN")
    ADMIN,

    @JsonAlias("ROLE_EVAKA_ESPOO_DIRECTOR")
    DIRECTOR,

    @JsonAlias("ROLE_EVAKA_ESPOO_FINANCEADMIN")
    FINANCE_ADMIN,

    @JsonAlias("ROLE_EVAKA_ESPOO_OFFICEHOLDER")
    SERVICE_WORKER,

    @JsonAlias("ROLE_EVAKA_ESPOO_UNITSUPERVISOR")
    UNIT_SUPERVISOR,

    STAFF;

    companion object {
        fun parse(value: String): UserRole = when (value) {
            "ROLE_ENDUSER" -> END_USER
            "ROLE_EVAKA_ESPOO_ADMIN" -> ADMIN
            "ROLE_EVAKA_ESPOO_DIRECTOR" -> DIRECTOR
            "ROLE_EVAKA_ESPOO_FINANCEADMIN" -> FINANCE_ADMIN
            "ROLE_EVAKA_ESPOO_OFFICEHOLDER" -> SERVICE_WORKER
            "ROLE_EVAKA_ESPOO_STAFF" -> STAFF
            "ROLE_EVAKA_ESPOO_UNITSUPERVISOR" -> UNIT_SUPERVISOR
            else -> enumValueOf(value.removePrefix("ROLE_"))
        }

        val ACL_ROLES = setOf(UNIT_SUPERVISOR, STAFF)
    }
}

interface RoleContainer {
    val roles: Set<UserRole>
    fun hasOneOfRoles(vararg requiredRoles: UserRole) = requiredRoles.any { roles.contains(it) }
    fun requireOneOfRoles(vararg roles: UserRole) {
        if (!hasOneOfRoles(*roles)) throw Forbidden("Permission denied")
    }
}
