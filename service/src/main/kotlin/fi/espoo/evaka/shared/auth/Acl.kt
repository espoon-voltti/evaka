// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
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
        getAuthorizedUnits(user, UserRole.SCOPED_ROLES)

    fun getAuthorizedUnits(user: AuthenticatedUser): AclAuthorization = getAuthorizedUnits(user, UserRole.SCOPED_ROLES)

    fun getAuthorizedUnits(user: AuthenticatedUser, roles: Set<UserRole>): AclAuthorization =
        if (user.hasOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN, UserRole.SERVICE_WORKER, UserRole.DIRECTOR)) {
            AclAuthorization.All
        } else {
            AclAuthorization.Subset(Database(jdbi).read { it.selectAuthorizedDaycares(user, roles) })
        }

    fun getRolesForUnit(user: AuthenticatedUser, daycareId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).read {
            it.createQuery(
                // language=SQL
                """
SELECT role
FROM daycare_acl_view
WHERE employee_id = :userId AND daycare_id = :daycareId
                """.trimIndent()
            ).bind("userId", user.id).bind("daycareId", daycareId).mapTo<UserRole>().toSet()
        }
    )

    fun getRolesForApplication(user: AuthenticatedUser, applicationId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).read { it ->
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
LEFT JOIN placement_plan pp ON pp.application_id = av.id
JOIN daycare_acl_view acl ON acl.daycare_id = ANY(av.preferredunits) OR acl.daycare_id = pp.unit_id
WHERE employee_id = :userId AND av.id = :applicationId AND av.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION,ACTIVE}'::application_status_type[])
                """.trimIndent()
            ).bind("userId", user.id).bind("applicationId", applicationId)
                .mapTo<UserRole>()
                .filter { it != UserRole.SPECIAL_EDUCATION_TEACHER || assistanceNeeded }
                .toSet()
        }
    )

    fun getRolesForUnitGroup(user: AuthenticatedUser, groupId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).read {
            it.createQuery(
                // language=SQL
                """
SELECT role
FROM daycare_group
JOIN daycare_acl_view USING (daycare_id)
WHERE employee_id = :userId AND daycare_group.id = :groupId
                """.trimIndent()
            ).bind("userId", user.id).bind("groupId", groupId).mapTo<UserRole>().toSet()
        }
    )

    fun getRolesForPlacement(user: AuthenticatedUser, placementId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).read {
            it.createQuery(
                // language=SQL
                """
SELECT role
FROM placement
JOIN daycare_acl_view ON placement.unit_id = daycare_acl_view.daycare_id
WHERE employee_id = :userId AND placement.id = :placementId
                """.trimIndent()
            ).bind("userId", user.id).bind("placementId", placementId).mapTo<UserRole>().toSet()
        }
    )

    fun getRolesForChild(user: AuthenticatedUser, childId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).read {
            it.createQuery(
                // language=SQL
                """
SELECT role
FROM child_acl_view
WHERE employee_id = :userId AND child_id = :childId
                """.trimIndent()
            ).bind("userId", user.id).bind("childId", childId).mapTo<UserRole>().toSet()
        }
    )

    fun getRolesForDecision(user: AuthenticatedUser, decisionId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).read {
            it.createQuery(
                // language=SQL
                """
SELECT role
FROM decision
JOIN daycare_acl_view ON decision.unit_id = daycare_acl_view.daycare_id
WHERE employee_id = :userId AND decision.id = :decisionId
                """.trimIndent()
            ).bind("userId", user.id).bind("decisionId", decisionId).mapTo<UserRole>().toSet()
        }
    )

    fun getRolesForAssistanceNeed(user: AuthenticatedUser, assistanceNeedId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).read {
            it.createQuery(
                // language=SQL
                """
SELECT role
FROM child_acl_view acl
JOIN assistance_need an ON acl.child_id = an.child_id
WHERE an.id = :assistanceNeedId AND acl.employee_id = :userId
                """.trimIndent()
            ).bind("assistanceNeedId", assistanceNeedId).bind("userId", user.id).mapTo<UserRole>().toSet()
        }
    )

    fun getRolesForAssistanceAction(user: AuthenticatedUser, assistanceActionId: UUID): AclAppliedRoles =
        AclAppliedRoles(
            (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).read {
                it.createQuery(
                    // language=SQL
                    """
SELECT role
FROM child_acl_view acl
JOIN assistance_action ac ON acl.child_id = ac.child_id
WHERE ac.id = :assistanceActionId AND acl.employee_id = :userId
                    """.trimIndent()
                ).bind("assistanceActionId", assistanceActionId).bind("userId", user.id).mapTo<UserRole>().toSet()
            }
        )

    fun getRolesForBackupCare(user: AuthenticatedUser, backupCareId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).read {
            it.createQuery(
                // language=SQL
                """
SELECT role
FROM child_acl_view acl
JOIN backup_care bc ON acl.child_id = bc.child_id
WHERE bc.id = :backupCareId AND acl.employee_id = :userId
                """.trimIndent()
            ).bind("backupCareId", backupCareId).bind("userId", user.id).mapTo<UserRole>().toSet()
        }
    )

    fun getRolesForServiceNeed(user: AuthenticatedUser, serviceNeedId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).read {
            it.createQuery(
                // language=SQL
                """
SELECT role
FROM child_acl_view acl
JOIN service_need sn ON acl.child_id = sn.child_id
WHERE sn.id = :serviceNeedId AND acl.employee_id = :userId
                """.trimIndent()
            ).bind("serviceNeedId", serviceNeedId).bind("userId", user.id).mapTo<UserRole>().toSet()
        }
    )

    fun getRolesForPairing(user: AuthenticatedUser, pairingId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).read {
            it.createQuery(
                // language=SQL
                """
SELECT role
FROM daycare_acl_view
JOIN pairing p ON daycare_id = p.unit_id
WHERE employee_id = :userId AND p.id = :pairingId
                """.trimIndent()
            ).bind("userId", user.id).bind("pairingId", pairingId).mapTo<UserRole>().toSet()
        }
    )

    fun getRolesForMobileDevice(user: AuthenticatedUser, deviceId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).read {
            it.createQuery(
                // language=SQL
                """
SELECT role
FROM daycare_acl_view
JOIN mobile_device d ON daycare_id = d.unit_id
WHERE employee_id = :userId AND d.id = :deviceId
                """.trimIndent()
            ).bind("userId", user.id).bind("deviceId", deviceId).mapTo<UserRole>().toSet()
        }
    )

    fun getRolesForDailyNote(user: AuthenticatedUser, noteId: UUID): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).read {
            it.createQuery(
                """
SELECT role
FROM daycare_daily_note dn
JOIN LATERAL (
    SELECT role
    FROM child_acl_view acl
    WHERE acl.employee_id = :userId
    AND acl.child_id = dn.child_id

    UNION ALL

    SELECT role
    FROM daycare_acl_view acl
    JOIN daycare_group dg USING (daycare_id)
    WHERE acl.employee_id = :userId
    AND dg.id = dn.group_id
) acls ON true
WHERE dn.id = :noteId
"""
            ).bind("userId", user.id).bind("noteId", noteId).mapTo<UserRole>().toSet()
        }
    )
}

private fun Database.Read.selectAuthorizedDaycares(user: AuthenticatedUser, roles: Set<UserRole>? = null): Set<UUID> =
    createQuery(
        "SELECT daycare_id FROM daycare_acl_view WHERE employee_id = :userId AND (:roles::user_role[] IS NULL OR role = ANY(:roles::user_role[]))"
    )
        .bind("userId", user.id)
        .bindNullable("roles", roles?.toTypedArray())
        .mapTo<UUID>()
        .toSet()
