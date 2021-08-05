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
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.domain.Forbidden
import java.util.UUID

class AccessControl(private val permittedRoleActions: PermittedRoleActions, private val acl: AccessControlList) {
    fun requirePermissionFor(user: AuthenticatedUser, action: Action.Global) {
        assertPermission(user.roles, action, permittedRoleActions::globalActions)
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.Application, id: ApplicationId) {
        val roles = acl.getRolesForApplication(user, id).roles
        assertPermission(roles, action, permittedRoleActions::applicationActions)
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.AssistanceAction, id: AssistanceActionId) {
        val roles = acl.getRolesForAssistanceAction(user, id).roles
        assertPermission(roles, action, permittedRoleActions::assistanceActionActions)
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.AssistanceNeed, id: AssistanceNeedId) {
        val roles = acl.getRolesForAssistanceNeed(user, id).roles
        assertPermission(roles, action, permittedRoleActions::assistanceNeedActions)
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.BackupCare, id: BackupCareId) {
        val roles = acl.getRolesForBackupCare(user, id).roles
        assertPermission(roles, action, permittedRoleActions::backupCareActions)
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.BackupPickup, id: BackupPickupId) {
        val roles = acl.getRolesForBackupPickup(user, id).roles
        assertPermission(roles, action, permittedRoleActions::backupPickupActions)
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.Child, id: UUID) {
        val roles = acl.getRolesForChild(user, id).roles
        assertPermission(roles, action, permittedRoleActions::childActions)
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.DailyNote, id: DaycareDailyNoteId) {
        val roles = acl.getRolesForDailyNote(user, id).roles
        assertPermission(roles, action, permittedRoleActions::dailyNoteActions)
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.Decision, id: DecisionId) {
        val roles = acl.getRolesForDecision(user, id).roles
        assertPermission(roles, action, permittedRoleActions::decisionActions)
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.Group, id: GroupId) {
        val roles = acl.getRolesForUnitGroup(user, id).roles
        assertPermission(roles, action, permittedRoleActions::groupActions)
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.MobileDevice, id: MobileDeviceId) {
        val roles = acl.getRolesForMobileDevice(user, id).roles
        assertPermission(roles, action, permittedRoleActions::mobileDeviceActions)
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.Pairing, id: PairingId) {
        val roles = acl.getRolesForPairing(user, id).roles
        assertPermission(roles, action, permittedRoleActions::pairingActions)
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.Placement, id: PlacementId) {
        val roles = acl.getRolesForPlacement(user, id).roles
        assertPermission(roles, action, permittedRoleActions::placementActions)
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.ServiceNeed, id: ServiceNeedId) {
        val roles = acl.getRolesForServiceNeed(user, id).roles
        assertPermission(roles, action, permittedRoleActions::serviceNeedActions)
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.Unit, id: DaycareId) {
        val roles = acl.getRolesForUnit(user, id).roles
        assertPermission(roles, action, permittedRoleActions::unitActions)
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.VasuDocument, id: UUID) {
        val roles = acl.getRolesForVasuDocument(user, id).roles
        assertPermission(roles, action, permittedRoleActions::vasuDocumentActions)
    }

    private inline fun <reified A> assertPermission(
        roles: Set<UserRole>,
        action: A,
        crossinline mapping: (role: UserRole) -> Set<A>
    ) where A : Action, A : Enum<A> {
        if (!roles.any { it == UserRole.ADMIN || mapping(it).contains(action) }) {
            throw Forbidden("Permission denied")
        }
    }
}
