// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

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
import fi.espoo.evaka.shared.domain.Forbidden
import org.jdbi.v3.core.Jdbi
import java.util.UUID

class AccessControl(
    private val permittedRoleActions: PermittedRoleActions,
    private val acl: AccessControlList,
    private val jdbi: Jdbi
) {
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
        return acl.getRolesForPilotFeature(user, PilotFeature.MESSAGING).hasOneOfRoles(UserRole.STAFF, UserRole.UNIT_SUPERVISOR)
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

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.BackupCare, id: BackupCareId) {
        assertPermission(
            user = user,
            getAclRoles = { acl.getRolesForBackupCare(user, id).roles },
            action = action,
            mapping = permittedRoleActions::backupCareActions
        )
    }

    fun getPermittedBackupCareActions(user: AuthenticatedUser, id: BackupCareId): Set<Action.BackupCare> {
        val aclRoles: Set<UserRole> by lazy {
            acl.getRolesForBackupCare(user, id).roles
        }
        return Action.BackupCare.values().asSequence().filter {
            hasPermission(
                user = user,
                getAclRoles = { aclRoles },
                action = it,
                mapping = permittedRoleActions::backupCareActions
            )
        }.toSet()
    }

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

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.Group, id: GroupId) {
        assertPermission(
            user = user,
            getAclRoles = { acl.getRolesForUnitGroup(user, id).roles },
            action = action,
            mapping = permittedRoleActions::groupActions
        )
    }

    fun getPermittedGroupActions(user: AuthenticatedUser, id: GroupId): Set<Action.Group> {
        val aclRoles: Set<UserRole> by lazy {
            acl.getRolesForUnitGroup(user, id).roles
        }
        return Action.Group.values().asSequence().filter {
            hasPermission(
                user = user,
                getAclRoles = { aclRoles },
                action = it,
                mapping = permittedRoleActions::groupActions
            )
        }.toSet()
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.GroupPlacement, id: GroupPlacementId) {
        assertPermission(
            user = user,
            getAclRoles = { acl.getRolesForGroupPlacement(user, id).roles },
            action = action,
            mapping = permittedRoleActions::groupPlacementActions
        )
    }

    fun getPermittedGroupPlacementActions(user: AuthenticatedUser, id: GroupPlacementId): Set<Action.GroupPlacement> {
        val aclRoles: Set<UserRole> by lazy {
            acl.getRolesForGroupPlacement(user, id).roles
        }
        return Action.GroupPlacement.values().asSequence().filter {
            hasPermission(
                user = user,
                getAclRoles = { aclRoles },
                action = it,
                mapping = permittedRoleActions::groupPlacementActions
            )
        }.toSet()
    }

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

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.Placement, id: PlacementId) {
        assertPermission(
            user = user,
            getAclRoles = { acl.getRolesForPlacement(user, id).roles },
            action = action,
            mapping = permittedRoleActions::placementActions
        )
    }

    fun getPermittedPlacementActions(user: AuthenticatedUser, id: PlacementId): Set<Action.Placement> {
        val aclRoles: Set<UserRole> by lazy {
            acl.getRolesForPlacement(user, id).roles
        }
        return Action.Placement.values().asSequence().filter {
            hasPermission(
                user = user,
                getAclRoles = { aclRoles },
                action = it,
                mapping = permittedRoleActions::placementActions
            )
        }.toSet()
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.ServiceNeed, id: ServiceNeedId) {
        assertPermission(
            user = user,
            getAclRoles = { acl.getRolesForServiceNeed(user, id).roles },
            action = action,
            mapping = permittedRoleActions::serviceNeedActions
        )
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.Unit, id: DaycareId) {
        assertPermission(
            user = user,
            getAclRoles = { acl.getRolesForUnit(user, id).roles },
            action = action,
            mapping = permittedRoleActions::unitActions
        )
    }

    fun hasPermissionFor(user: AuthenticatedUser, action: Action.Unit, id: DaycareId): Boolean {
        return hasPermission(
            user = user,
            getAclRoles = { acl.getRolesForUnit(user, id).roles },
            action = action,
            mapping = permittedRoleActions::unitActions
        )
    }

    fun getPermittedUnitActions(user: AuthenticatedUser, id: DaycareId): Set<Action.Unit> {
        val aclRoles: Set<UserRole> by lazy {
            acl.getRolesForUnit(user, id).roles
        }
        return Action.Unit.values().asSequence().filter {
            hasPermission(
                user = user,
                getAclRoles = { aclRoles },
                action = it,
                mapping = permittedRoleActions::unitActions
            )
        }.toSet()
    }

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
