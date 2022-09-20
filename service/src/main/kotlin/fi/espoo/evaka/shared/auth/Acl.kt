// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import org.jdbi.v3.core.Jdbi

sealed class AclAuthorization {
    abstract fun isAuthorized(id: DaycareId): Boolean
    abstract val ids: Set<DaycareId>?

    object All : AclAuthorization() {
        override fun isAuthorized(id: DaycareId): Boolean = true
        override val ids: Set<DaycareId>? = null
    }

    data class Subset(override val ids: Set<DaycareId>) : AclAuthorization() {
        override fun isAuthorized(id: DaycareId): Boolean = ids.contains(id)
    }

    fun isEmpty(): Boolean {
        return this is Subset && this.ids.isEmpty()
    }

    companion object {
        fun from(filter: AccessControlFilter<DaycareId>) =
            when (filter) {
                AccessControlFilter.PermitAll -> All
                is AccessControlFilter.Some -> Subset(filter.filter)
            }
    }
}

class AccessControlList(private val jdbi: Jdbi) {
    fun getAuthorizedUnits(user: AuthenticatedUser): AclAuthorization =
        getAuthorizedUnits(user, UserRole.SCOPED_ROLES)

    fun getAuthorizedUnits(user: AuthenticatedUser, roles: Set<UserRole>): AclAuthorization =
        if (
            user is AuthenticatedUser.Employee &&
                setOf(
                        UserRole.ADMIN,
                        UserRole.FINANCE_ADMIN,
                        UserRole.SERVICE_WORKER,
                        UserRole.DIRECTOR,
                        UserRole.REPORT_VIEWER
                    )
                    .any { user.globalRoles.contains(it) }
        ) {
            AclAuthorization.All
        } else {
            AclAuthorization.Subset(
                Database(jdbi).connect { db ->
                    db.read { it.selectAuthorizedDaycares(user, roles) }
                }
            )
        }
}

private fun Database.Read.selectAuthorizedDaycares(
    user: AuthenticatedUser,
    roles: Set<UserRole>? = null
): Set<DaycareId> {
    if (roles?.isEmpty() == true) return emptySet()

    return createQuery(
            "SELECT daycare_id FROM daycare_acl_view WHERE employee_id = :userId AND (:roles::user_role[] IS NULL OR role = ANY(:roles::user_role[]))"
        )
        .bind("userId", user.rawId())
        .bind("roles", roles)
        .mapTo<DaycareId>()
        .toSet()
}
