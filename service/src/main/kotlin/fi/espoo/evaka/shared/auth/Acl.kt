// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

data class AclAppliedRoles(override val roles: Set<UserRole>) : RoleContainer
sealed class AclAuthorization {
    abstract fun isAuthorized(id: UUID): Boolean
    abstract val ids: Set<UUID>?

    object All : AclAuthorization() {
        override fun isAuthorized(id: UUID): Boolean = true
        override val ids: Set<UUID>? = null
    }

    data class Subset(override val ids: Set<UUID>) : AclAuthorization() {
        override fun isAuthorized(id: UUID): Boolean = ids.contains(id)
    }
}

class AccessControlList(private val jdbi: Jdbi) {
    fun getAuthorizedDaycares(user: AuthenticatedUser): AclAuthorization =
        if (user.hasOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN, UserRole.SERVICE_WORKER, UserRole.DIRECTOR)) {
            AclAuthorization.All
        } else {
            AclAuthorization.Subset(Database(jdbi).read { it.selectAuthorizedDaycares(user) })
        }

    fun getAuthorizedUnits(user: AuthenticatedUser): AclAuthorization =
        if (user.hasOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN, UserRole.SERVICE_WORKER, UserRole.DIRECTOR)) {
            AclAuthorization.All
        } else {
            AclAuthorization.Subset(Database(jdbi).read { it.selectAuthorizedDaycares(user) })
        }

    fun getRolesForUnit(user: AuthenticatedUser, daycareId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.ACL_ROLES) + Database(jdbi).read {
            it.createQuery(
                // language=SQL
                """
SELECT role
FROM daycare_acl
WHERE employee_id = :userId AND daycare_id = :daycareId
                """.trimIndent()
            ).bind("userId", user.id).bind("daycareId", daycareId).mapTo<UserRole>().toSet()
        }
    )

    fun getRolesForApplication(user: AuthenticatedUser, applicationId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.ACL_ROLES) + Database(jdbi).read { it ->
            val assistanceNeeded = it.createQuery(
                // language=SQL
                """
SELECT (document -> 'careDetails' ->> 'assistanceNeeded')
FROM application_form
WHERE application_id = :applicationId AND latest IS TRUE;
                """.trimIndent()
            ).bind("applicationId", applicationId)
                .mapTo<Boolean>()
                .contains(true)

            it.createQuery(
                // language=SQL
                """
SELECT role
FROM application_view av
LEFT JOIN placement_plan pp ON pp.application_id = av.id AND pp.deleted = false
JOIN daycare_acl acl ON acl.daycare_id = ANY(av.preferredunits) OR acl.daycare_id = pp.unit_id
WHERE employee_id = :userId AND av.id = :applicationId AND av.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION,ACTIVE}'::application_status_type[])
                """.trimIndent()
            ).bind("userId", user.id).bind("applicationId", applicationId)
                .mapTo<UserRole>()
                .filter { it != UserRole.SPECIAL_EDUCATION_TEACHER || assistanceNeeded }
                .toSet()
        }
    )

    fun getRolesForUnitGroup(user: AuthenticatedUser, groupId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.ACL_ROLES) + Database(jdbi).read {
            it.createQuery(
                // language=SQL
                """
SELECT role
FROM daycare_group
JOIN daycare_acl USING (daycare_id)
WHERE employee_id = :userId AND daycare_group.id = :groupId
                """.trimIndent()
            ).bind("userId", user.id).bind("groupId", groupId).mapTo<UserRole>().toSet()
        }
    )

    fun getRolesForPlacement(user: AuthenticatedUser, placementId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.ACL_ROLES) + Database(jdbi).read {
            it.createQuery(
                // language=SQL
                """
SELECT role
FROM placement
JOIN daycare_acl ON placement.unit_id = daycare_acl.daycare_id
WHERE employee_id = :userId AND placement.id = :placementId
                """.trimIndent()
            ).bind("userId", user.id).bind("placementId", placementId).mapTo<UserRole>().toSet()
        }
    )

    fun getRolesForChild(user: AuthenticatedUser, childId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.ACL_ROLES) + Database(jdbi).read {
            it.createQuery(
                // language=SQL
                """
SELECT acl.role
FROM person ch
LEFT JOIN placement pl ON pl.child_id = ch.id AND pl.end_date > current_date - INTERVAL '1 month'
LEFT JOIN application a ON a.child_id = ch.id AND a.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION, ACTIVE}'::application_status_type[])
LEFT JOIN placement_plan pp ON pp.application_id = a.id
JOIN daycare_acl acl ON acl.daycare_id = pl.unit_id OR acl.daycare_id = pp.unit_id
WHERE employee_id = :userId AND ch.id = :childId
                """.trimIndent()
            ).bind("userId", user.id).bind("childId", childId).mapTo<UserRole>().toSet()
        }
    )

    fun getRolesForDecision(user: AuthenticatedUser, decisionId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.ACL_ROLES) + Database(jdbi).read {
            it.createQuery(
                // language=SQL
                """
SELECT role
FROM decision
JOIN daycare_acl ON decision.unit_id = daycare_acl.daycare_id
WHERE employee_id = :userId AND decision.id = :decisionId
                """.trimIndent()
            ).bind("userId", user.id).bind("decisionId", decisionId).mapTo<UserRole>().toSet()
        }
    )

    fun getRolesForAssistanceNeed(user: AuthenticatedUser, assistanceNeedId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.ACL_ROLES) + Database(jdbi).read {
            it.createQuery(
                // language=SQL
                """
SELECT child_id
FROM assistance_need
WHERE id = :assistanceNeedId
                """.trimIndent()
            ).bind("assistanceNeedId", assistanceNeedId)
                .mapTo<UUID>()
                .map { childId -> getRolesForChild(user, childId).roles }
                .fold(setOf<UserRole>()) { acc, rolesSet -> acc.intersect(rolesSet) }
        }
    )

    fun getRolesForBackupCare(user: AuthenticatedUser, backupCareId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.ACL_ROLES) + Database(jdbi).read {
            it.createQuery(
                // language=SQL
                """
SELECT child_id
FROM backup_care
WHERE id = :backupCareId
                """.trimIndent()
            ).bind("backupCareId", backupCareId)
                .mapTo<UUID>()
                .map { childId -> getRolesForChild(user, childId).roles }
                .fold(setOf<UserRole>()) { acc, rolesSet -> acc.intersect(rolesSet) }
        }
    )

    fun getRolesForPairing(user: AuthenticatedUser, pairingId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.ACL_ROLES) + Database(jdbi).read {
            it.createQuery(
                // language=SQL
                """
SELECT role
FROM daycare_acl
JOIN pairing p ON daycare_id = p.unit_id
WHERE employee_id = :userId AND p.id = :pairingId
                """.trimIndent()
            ).bind("userId", user.id).bind("pairingId", pairingId).mapTo<UserRole>().toSet()
        }
    )
}

private fun Database.Read.selectAuthorizedDaycares(user: AuthenticatedUser): Set<UUID> = createQuery(
    // language=SQL
    "SELECT daycare_id FROM daycare_acl WHERE employee_id = :userId"
).bind("userId", user.id).mapTo<UUID>().toSet()
