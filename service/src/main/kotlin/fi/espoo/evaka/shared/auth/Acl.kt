// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.security.PilotFeature
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo

data class AclAppliedRoles(override val roles: Set<UserRole>) : RoleContainer
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
}

class AccessControlList(private val jdbi: Jdbi) {
    fun getAuthorizedDaycares(user: AuthenticatedUser): AclAuthorization =
        getAuthorizedUnits(user, UserRole.SCOPED_ROLES)

    fun getAuthorizedUnits(user: AuthenticatedUser): AclAuthorization = getAuthorizedUnits(user, UserRole.SCOPED_ROLES)

    fun getAuthorizedUnits(user: AuthenticatedUser, roles: Set<UserRole>): AclAuthorization =
        @Suppress("DEPRECATION")
        if (user.hasOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN, UserRole.SERVICE_WORKER, UserRole.DIRECTOR, UserRole.REPORT_VIEWER)) {
            AclAuthorization.All
        } else {
            AclAuthorization.Subset(Database(jdbi).connect { db -> db.read { it.selectAuthorizedDaycares(user, roles) } })
        }

    @Deprecated("use Action model instead", replaceWith = ReplaceWith(""))
    fun getRolesForPilotFeature(user: AuthenticatedUser, feature: PilotFeature): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).connect { db ->
            db.read {
                it.createQuery(
                    // language=SQL
                    """
SELECT role
FROM daycare d
JOIN daycare_acl acl
ON d.id = acl.daycare_id
WHERE :pilotFeature = ANY(d.enabled_pilot_features)
AND employee_id = :userId
                    """.trimIndent()
                ).bind("userId", user.rawId()).bind("pilotFeature", feature).mapTo<UserRole>().toSet()
            }
        }
    )
}

private fun Database.Read.selectAuthorizedDaycares(user: AuthenticatedUser, roles: Set<UserRole>? = null): Set<DaycareId> {
    if (roles?.isEmpty() == true) return emptySet()

    return createQuery(
        "SELECT daycare_id FROM daycare_acl_view WHERE employee_id = :userId AND (:roles::user_role[] IS NULL OR role = ANY(:roles::user_role[]))"
    )
        .bind("userId", user.rawId())
        .bindNullable("roles", roles?.toTypedArray())
        .mapTo<DaycareId>()
        .toSet()
}
