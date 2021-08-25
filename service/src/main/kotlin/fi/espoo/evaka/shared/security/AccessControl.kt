// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.BackupPickupId
import fi.espoo.evaka.shared.DaycareDailyNoteId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.VasuTemplateId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.utils.enumSetOf
import fi.espoo.evaka.shared.utils.toEnumSet
import org.intellij.lang.annotations.Language
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo
import java.util.EnumSet
import java.util.UUID

class AccessControl(
    private val permittedRoleActions: PermittedRoleActions,
    private val acl: AccessControlList,
    private val jdbi: Jdbi
) {
    private val backupCare = ActionConfig(
        """
SELECT bc.id, role
FROM child_acl_view acl
JOIN backup_care bc ON acl.child_id = bc.child_id
WHERE acl.employee_id = :userId
        """.trimIndent(),
        "bc.id",
        permittedRoleActions::backupCareActions
    )
    private val group = ActionConfig(
        """
SELECT daycare_group_id AS id, role
FROM daycare_group_acl_view
WHERE employee_id = :userId
        """.trimIndent(),
        "daycare_group_id",
        permittedRoleActions::groupActions
    )
    private val groupPlacement = ActionConfig(
        """
SELECT daycare_group_placement.id, role
FROM placement
JOIN daycare_acl_view ON placement.unit_id = daycare_acl_view.daycare_id
JOIN daycare_group_placement on placement.id = daycare_group_placement.daycare_placement_id
WHERE employee_id = :userId
        """.trimIndent(),
        "daycare_group_placement.id",
        permittedRoleActions::groupPlacementActions
    )
    private val placement = ActionConfig(
        """
SELECT placement.id, role
FROM placement
JOIN daycare_acl_view ON placement.unit_id = daycare_acl_view.daycare_id
WHERE employee_id = :userId
        """.trimIndent(),
        "placement.id",
        permittedRoleActions::placementActions
    )
    private val unit = ActionConfig(
        """
SELECT daycare_id AS id, role
FROM daycare_acl_view
WHERE employee_id = :userId
        """.trimIndent(),
        "daycare_id",
        permittedRoleActions::unitActions
    )

    fun getPermittedFeatures(user: AuthenticatedUser.Employee): EmployeeFeatures =
        EmployeeFeatures(
            applications = user.hasOneOfRoles(
                UserRole.ADMIN,
                UserRole.SERVICE_WORKER,
                UserRole.SPECIAL_EDUCATION_TEACHER
            ),
            employees = user.hasOneOfRoles(UserRole.ADMIN),
            financeBasics = user.hasOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN),
            finance = user.hasOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN),
            messages = isMessagingEnabled(user),
            personSearch = user.hasOneOfRoles(
                UserRole.ADMIN,
                UserRole.SERVICE_WORKER,
                UserRole.FINANCE_ADMIN,
                UserRole.UNIT_SUPERVISOR,
                UserRole.SPECIAL_EDUCATION_TEACHER
            ),
            reports = user.hasOneOfRoles(
                UserRole.ADMIN,
                UserRole.DIRECTOR,
                UserRole.SERVICE_WORKER,
                UserRole.FINANCE_ADMIN,
                UserRole.UNIT_SUPERVISOR,
                UserRole.SPECIAL_EDUCATION_TEACHER
            ),
            units = user.hasOneOfRoles(
                UserRole.ADMIN,
                UserRole.SERVICE_WORKER,
                UserRole.FINANCE_ADMIN,
                UserRole.UNIT_SUPERVISOR,
                UserRole.STAFF,
                UserRole.SPECIAL_EDUCATION_TEACHER
            ),
            vasuTemplates = user.hasOneOfRoles(UserRole.ADMIN),
        )

    private fun isMessagingEnabled(user: AuthenticatedUser): Boolean {
        return acl.getRolesForPilotFeature(user, PilotFeature.MESSAGING)
            .hasOneOfRoles(UserRole.STAFF, UserRole.UNIT_SUPERVISOR)
    }

    fun requireGuardian(user: AuthenticatedUser, childIds: Set<UUID>) {
        val dependants = Database(jdbi).read {
            it.createQuery(
                "SELECT child_id FROM guardian g WHERE g.guardian_id = :userId"
            ).bind("userId", user.id).mapTo<UUID>().list()
        }

        if (childIds.any { !dependants.contains(it) }) {
            throw Forbidden("Not a guardian of a child")
        }
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.Global) {
        assertGlobalPermission(user, action, permittedRoleActions::globalActions)
    }

    fun getPermittedRoles(action: Action.Global): Set<UserRole> {
        return UserRole.values()
            .filter { permittedRoleActions.globalActions(it).contains(action) }
            .toSet()
            .plus(UserRole.ADMIN)
    }

    fun <A : Action.ScopedAction<I>, I> requirePermissionFor(user: AuthenticatedUser, action: A, id: I) {
        if (!hasPermissionFor(user, action, id)) {
            throw Forbidden("Permission denied")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <A : Action.ScopedAction<I>, I> hasPermissionFor(user: AuthenticatedUser, action: A, id: I): Boolean =
        when (action) {
            is Action.BackupCare -> this.backupCare.hasPermission(user, action, id as BackupCareId)
            is Action.Group -> this.group.hasPermission(user, action, id as GroupId)
            is Action.GroupPlacement -> this.groupPlacement.hasPermission(user, action, id as GroupPlacementId)
            is Action.Placement -> this.placement.hasPermission(user, action, id as PlacementId)
            is Action.Unit -> this.unit.hasPermission(user, action, id as DaycareId)
            else -> error("Unsupported action type")
        }.exhaust()

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.Application, id: ApplicationId) {
        assertPermission(
            user = user,
            getAclRoles = { acl.getRolesForApplication(user, id).roles },
            action = action,
            mapping = permittedRoleActions::applicationActions
        )
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.AssistanceAction, id: AssistanceActionId) {
        assertPermission(
            user = user,
            getAclRoles = { acl.getRolesForAssistanceAction(user, id).roles },
            action = action,
            mapping = permittedRoleActions::assistanceActionActions
        )
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.AssistanceNeed, id: AssistanceNeedId) {
        assertPermission(
            user = user,
            getAclRoles = { acl.getRolesForAssistanceNeed(user, id).roles },
            action = action,
            mapping = permittedRoleActions::assistanceNeedActions
        )
    }

    fun getPermittedBackupCareActions(
        user: AuthenticatedUser,
        ids: Collection<BackupCareId>
    ): Map<BackupCareId, Set<Action.BackupCare>> = this.backupCare.getPermittedActions(user, ids)

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.BackupPickup, id: BackupPickupId) {
        assertPermission(
            user = user,
            getAclRoles = { acl.getRolesForBackupPickup(user, id).roles },
            action = action,
            mapping = permittedRoleActions::backupPickupActions
        )
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.Child, id: UUID) {
        assertPermission(
            user = user,
            getAclRoles = { acl.getRolesForChild(user, id).roles },
            action = action,
            mapping = permittedRoleActions::childActions
        )
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.DailyNote, id: DaycareDailyNoteId) {
        assertPermission(
            user = user,
            getAclRoles = { acl.getRolesForDailyNote(user, id).roles },
            action = action,
            mapping = permittedRoleActions::dailyNoteActions
        )
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.Decision, id: DecisionId) {
        assertPermission(
            user = user,
            getAclRoles = { acl.getRolesForDecision(user, id).roles },
            action = action,
            mapping = permittedRoleActions::decisionActions
        )
    }

    fun getPermittedGroupActions(
        user: AuthenticatedUser,
        ids: Collection<GroupId>
    ): Map<GroupId, Set<Action.Group>> = this.group.getPermittedActions(user, ids)

    fun getPermittedGroupPlacementActions(
        user: AuthenticatedUser,
        ids: Collection<GroupPlacementId>
    ): Map<GroupPlacementId, Set<Action.GroupPlacement>> = this.groupPlacement.getPermittedActions(user, ids)

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.MobileDevice, id: MobileDeviceId) {
        assertPermission(
            user = user,
            getAclRoles = { acl.getRolesForMobileDevice(user, id).roles },
            action = action,
            mapping = permittedRoleActions::mobileDeviceActions
        )
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.Pairing, id: PairingId) {
        assertPermission(
            user = user,
            getAclRoles = { acl.getRolesForPairing(user, id).roles },
            action = action,
            mapping = permittedRoleActions::pairingActions
        )
    }

    fun getPermittedPlacementActions(
        user: AuthenticatedUser,
        ids: Collection<PlacementId>
    ): Map<PlacementId, Set<Action.Placement>> = this.placement.getPermittedActions(user, ids)

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.ServiceNeed, id: ServiceNeedId) {
        assertPermission(
            user = user,
            getAclRoles = { acl.getRolesForServiceNeed(user, id).roles },
            action = action,
            mapping = permittedRoleActions::serviceNeedActions
        )
    }

    fun getPermittedUnitActions(
        user: AuthenticatedUser,
        ids: Collection<DaycareId>
    ): Map<DaycareId, Set<Action.Unit>> = this.unit.getPermittedActions(user, ids)

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.VasuDocument, id: VasuDocumentId) {
        assertPermission(
            user = user,
            getAclRoles = { acl.getRolesForVasuDocument(user, id).roles },
            action = action,
            mapping = permittedRoleActions::vasuDocumentActions
        )
    }

    fun requirePermissionFor(
        user: AuthenticatedUser,
        action: Action.VasuTemplate,
        @Suppress("UNUSED_PARAMETER") id: VasuTemplateId
    ) {
        // VasuTemplate actions in Espoo are global so the id parameter is ignored
        assertGlobalPermission(user, action, permittedRoleActions::vasuTemplateActions)
    }

    private inline fun <reified A, reified I> ActionConfig<A>.getPermittedActions(
        user: AuthenticatedUser,
        ids: Collection<I>
    ): Map<I, Set<A>> where A : Action.ScopedAction<I>, A : Enum<A> {
        val globalActions = enumValues<A>().asSequence()
            .filter { action -> hasGlobalPermission(user, action) }.toEnumSet()

        val result = ids.associateTo(linkedMapOf()) { (it to enumSetOf(*globalActions.toTypedArray())) }
        if (user is AuthenticatedUser.Employee) {
            val scopedActions = EnumSet.allOf(A::class.java).also { it -= globalActions }
            if (scopedActions.isNotEmpty()) {
                Database(jdbi).read { tx ->
                    for ((id, roles) in this.getRolesForAll(tx, user, *ids.toTypedArray())) {
                        val permittedActions = result[id]!!
                        for (action in scopedActions) {
                            if (roles.any { mapping(it).contains(action) }) {
                                permittedActions += action
                            }
                        }
                    }
                }
            }
        }
        return result
    }

    private inline fun <reified A, reified I> ActionConfig<A>.hasGlobalPermission(
        user: AuthenticatedUser,
        action: A
    ): Boolean where A : Action.ScopedAction<I>, A : Enum<A> {
        val globalRoles = when (user) {
            is AuthenticatedUser.Employee -> user.globalRoles
            else -> user.roles
        }
        return globalRoles.any { it == UserRole.ADMIN || mapping(it).contains(action) }
    }

    private inline fun <reified A, reified I> ActionConfig<A>.hasPermission(
        user: AuthenticatedUser,
        action: A,
        id: I
    ): Boolean where A : Action.ScopedAction<I>, A : Enum<A> {
        if (hasGlobalPermission(user, action)) return true
        return Database(jdbi).read { getRolesFor(it, user, id) }.any { mapping(it).contains(action) }
    }

    private inline fun <reified A : Action.ScopedAction<I>, reified I> ActionConfig<A>.getRolesFor(
        tx: Database.Read,
        user: AuthenticatedUser,
        id: I
    ): Set<UserRole> = getRolesForAll(tx, user, id)[id]!!

    private inline fun <reified A : Action.ScopedAction<I>, reified I> ActionConfig<A>.getRolesForAll(
        tx: Database.Read,
        user: AuthenticatedUser,
        vararg ids: I
    ): Map<I, Set<UserRole>> = when (user) {
        is AuthenticatedUser.Employee -> tx.createQuery("${this.query} AND ${this.idExpression} = ANY(:ids)")
            .bind("userId", user.id).bind("ids", ids)
            .reduceRows(ids.associateTo(linkedMapOf()) { (it to enumSetOf(*user.globalRoles.toTypedArray())) }) { acc, row ->
                acc[row.mapColumn("id")]!! += row.mapColumn<UserRole>("role")
                acc
            }
        else -> ids.associate { (it to enumSetOf(*user.roles.toTypedArray())) }
    }

    private inline fun <reified A> hasGlobalPermission(
        user: AuthenticatedUser,
        action: A,
        crossinline mapping: (role: UserRole) -> Set<A>
    ): Boolean where A : Action, A : Enum<A> {
        val globalRoles = user.roles - UserRole.SCOPED_ROLES
        return (globalRoles.any { it == UserRole.ADMIN || mapping(it).contains(action) })
    }

    private inline fun <reified A> assertGlobalPermission(
        user: AuthenticatedUser,
        action: A,
        crossinline mapping: (role: UserRole) -> Set<A>
    ) where A : Action, A : Enum<A> {
        if (!hasGlobalPermission(user, action, mapping))
            throw Forbidden("Permission denied")
    }

    private inline fun <reified A> hasPermission(
        user: AuthenticatedUser,
        getAclRoles: () -> Set<UserRole>,
        action: A,
        crossinline mapping: (role: UserRole) -> Set<A>
    ): Boolean where A : Action, A : Enum<A> {
        if (hasGlobalPermission(user, action, mapping)) return true

        return (getAclRoles().any { it == UserRole.ADMIN || mapping(it).contains(action) })
    }

    private inline fun <reified A> assertPermission(
        user: AuthenticatedUser,
        getAclRoles: () -> Set<UserRole>,
        action: A,
        crossinline mapping: (role: UserRole) -> Set<A>
    ) where A : Action, A : Enum<A> {
        if (!hasPermission(user, getAclRoles, action, mapping))
            throw Forbidden("Permission denied")
    }
}

private data class ActionConfig<A>(
    @Language("sql") val query: String,
    val idExpression: String,
    val mapping: (role: UserRole) -> Set<A>
)
