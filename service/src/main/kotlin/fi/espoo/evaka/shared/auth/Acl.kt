// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.VasuDocumentId
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
    fun getRolesForUnit(user: AuthenticatedUser, daycareId: DaycareId): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).connect { db ->
            db.read {
                it.createQuery(
                    // language=SQL
                    """
SELECT role
FROM daycare_acl_view
WHERE employee_id = :userId AND daycare_id = :daycareId
                    """.trimIndent()
                ).bind("userId", user.id).bind("daycareId", daycareId).mapTo<UserRole>().toSet()
            }
        }
    )

    @Deprecated("use Action model instead", replaceWith = ReplaceWith(""))
    fun getRolesForApplication(user: AuthenticatedUser, applicationId: ApplicationId): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).connect { db ->
            db.read {
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
                    .toSet()
            }
        }
    )

    @Deprecated("use Action model instead", replaceWith = ReplaceWith(""))
    fun getRolesForUnitGroup(user: AuthenticatedUser, groupId: GroupId): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).connect { db ->
            db.read {
                it.createQuery(
                    // language=SQL
                    """
SELECT role
FROM daycare_group_acl_view
WHERE employee_id = :userId AND daycare_group_id = :groupId
                    """.trimIndent()
                ).bind("userId", user.id).bind("groupId", groupId).mapTo<UserRole>().toSet()
            }
        }
    )

    @Deprecated("use Action model instead", replaceWith = ReplaceWith(""))
    fun getRolesForChild(user: AuthenticatedUser, childId: ChildId): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).connect { db ->
            db.read {
                it.createQuery(
                    // language=SQL
                    """
SELECT role
FROM child_acl_view
WHERE employee_id = :userId AND child_id = :childId
                    """.trimIndent()
                ).bind("userId", user.id).bind("childId", childId).mapTo<UserRole>().toSet()
            }
        }
    )

    @Deprecated("use Action model instead", replaceWith = ReplaceWith(""))
    fun getRolesForAssistanceNeed(user: AuthenticatedUser, assistanceNeedId: AssistanceNeedId): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).connect { db ->
            db.read {
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
        }
    )

    @Deprecated("use Action model instead", replaceWith = ReplaceWith(""))
    fun getRolesForPedagogicalDocument(user: AuthenticatedUser, documentId: PedagogicalDocumentId): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).connect { db ->
            db.read {
                it.createQuery(
                    // language=SQL
                    """
SELECT role
FROM pedagogical_document pd
JOIN child_acl_view ON pd.child_id = child_acl_view.child_id
WHERE child_acl_view.employee_id = :userId AND pd.id = :documentId
                    """.trimIndent()
                )
                    .bind("userId", user.id)
                    .bind("documentId", documentId)
                    .mapTo<UserRole>().toSet()
            }
        }
    )

    @Deprecated("use Action model instead", replaceWith = ReplaceWith(""))
    fun getRolesForVasuDocument(user: AuthenticatedUser, documentId: VasuDocumentId): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).connect { db ->
            db.read {
                it.createQuery(
                    // language=SQL
                    """
SELECT role
FROM curriculum_document
JOIN child_acl_view ON curriculum_document.child_id = child_acl_view.child_id
WHERE employee_id = :userId AND curriculum_document.id = :documentId
                    """.trimIndent()
                ).bind("userId", user.id).bind("documentId", documentId).mapTo<UserRole>().toSet()
            }
        }
    )

    @Deprecated("use Action model instead", replaceWith = ReplaceWith(""))
    fun getRolesForPilotFeature(user: AuthenticatedUser, feature: PilotFeature): AclAppliedRoles = AclAppliedRoles(
        (user.roles - UserRole.SCOPED_ROLES) + Database(jdbi).connect { db ->
            db.read {
                it.createQuery(
                    // language=SQL
                    """
SELECT role
FROM daycare d
JOIN daycare_acl_view acl
ON d.id = acl.daycare_id
WHERE :pilotFeature = ANY(d.enabled_pilot_features)
AND employee_id = :userId
                    """.trimIndent()
                ).bind("userId", user.id).bind("pilotFeature", feature).mapTo<UserRole>().toSet()
            }
        }
    )
}

private fun Database.Read.selectAuthorizedDaycares(user: AuthenticatedUser, roles: Set<UserRole>? = null): Set<DaycareId> {
    if (roles?.isEmpty() == true) return emptySet()

    return createQuery(
        "SELECT daycare_id FROM daycare_acl_view WHERE employee_id = :userId AND (:roles::user_role[] IS NULL OR role = ANY(:roles::user_role[]))"
    )
        .bind("userId", user.id)
        .bindNullable("roles", roles?.toTypedArray())
        .mapTo<DaycareId>()
        .toSet()
}
