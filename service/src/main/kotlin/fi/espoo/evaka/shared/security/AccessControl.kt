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
import java.util.UUID

class AccessControl(private val permittedRoleActions: PermittedRoleActions, private val acl: AccessControlList) {

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.Global) {
        assertGlobalPermission(user, action, permittedRoleActions::globalActions)
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

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.VasuDocument, id: VasuDocumentId) {
        assertPermission(
            user = user,
            getAclRoles = { acl.getRolesForVasuDocument(user, id).roles },
            action = action,
            mapping = permittedRoleActions::vasuDocumentActions
        )
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.VasuTemplate, @Suppress("UNUSED_PARAMETER") id: VasuTemplateId) {
        // VasuTemplate actions in Espoo are global so the id parameter is ignored
        assertGlobalPermission(user, action, permittedRoleActions::vasuTemplateActions)
    }

    private inline fun <reified A> assertGlobalPermission(
        user: AuthenticatedUser,
        action: A,
        crossinline mapping: (role: UserRole) -> Set<A>
    ) where A : Action, A : Enum<A> {
        val globalRoles = user.roles - UserRole.SCOPED_ROLES
        if (globalRoles.any { it == UserRole.ADMIN || mapping(it).contains(action) }) {
            return
        }

        throw Forbidden("Permission denied")
    }

    private inline fun <reified A> assertPermission(
        user: AuthenticatedUser,
        getAclRoles: () -> Set<UserRole>,
        action: A,
        crossinline mapping: (role: UserRole) -> Set<A>
    ) where A : Action, A : Enum<A> {
        try {
            assertGlobalPermission(user, action, mapping)
        } catch (e: Forbidden) {
            val roles = getAclRoles()
            if (roles.any { it == UserRole.ADMIN || mapping(it).contains(action) }) {
                return
            }

            throw Forbidden("Permission denied")
        }
    }
}
