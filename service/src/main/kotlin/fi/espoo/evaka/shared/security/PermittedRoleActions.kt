// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.shared.auth.UserRole
import java.util.EnumMap
import java.util.EnumSet

/**
 * Role → action mapping
 */
interface PermittedRoleActions {
    fun globalActions(role: UserRole): Set<Action.Global>
    fun applicationActions(role: UserRole): Set<Action.Application>
    fun assistanceActionActions(role: UserRole): Set<Action.AssistanceAction>
    fun assistanceNeedActions(role: UserRole): Set<Action.AssistanceNeed>
    fun backupCareActions(role: UserRole): Set<Action.BackupCare>
    fun backupPickupActions(role: UserRole): Set<Action.BackupPickup>
    fun childActions(role: UserRole): Set<Action.Child>
    fun dailyNoteActions(role: UserRole): Set<Action.DailyNote>
    fun decisionActions(role: UserRole): Set<Action.Decision>
    fun groupActions(role: UserRole): Set<Action.Group>
    fun mobileDeviceActions(role: UserRole): Set<Action.MobileDevice>
    fun pairingActions(role: UserRole): Set<Action.Pairing>
    fun placementActions(role: UserRole): Set<Action.Placement>
    fun serviceNeedActions(role: UserRole): Set<Action.ServiceNeed>
    fun unitActions(role: UserRole): Set<Action.Unit>
    fun vasuDocumentActions(role: UserRole): Set<Action.VasuDocument>
    fun vasuTemplateActions(role: UserRole): Set<Action.VasuTemplate>
}

/**
 * Role → action mapping based on static data.
 *
 * Uses system defaults, unless some mappings are overridden using constructor parameters
 */
class StaticPermittedRoleActions(
    val global: ActionsByRole<Action.Global> = getDefaults(),
    val application: ActionsByRole<Action.Application> = getDefaults(),
    val assistanceAction: ActionsByRole<Action.AssistanceAction> = getDefaults(),
    val assistanceNeed: ActionsByRole<Action.AssistanceNeed> = getDefaults(),
    val backupCare: ActionsByRole<Action.BackupCare> = getDefaults(),
    val backupPickup: ActionsByRole<Action.BackupPickup> = getDefaults(),
    val child: ActionsByRole<Action.Child> = getDefaults(),
    val dailyNote: ActionsByRole<Action.DailyNote> = getDefaults(),
    val decision: ActionsByRole<Action.Decision> = getDefaults(),
    val group: ActionsByRole<Action.Group> = getDefaults(),
    val mobileDevice: ActionsByRole<Action.MobileDevice> = getDefaults(),
    val pairing: ActionsByRole<Action.Pairing> = getDefaults(),
    val placement: ActionsByRole<Action.Placement> = getDefaults(),
    val serviceNeed: ActionsByRole<Action.ServiceNeed> = getDefaults(),
    val unit: ActionsByRole<Action.Unit> = getDefaults(),
    val vasuDocument: ActionsByRole<Action.VasuDocument> = getDefaults(),
    val vasuTemplate: ActionsByRole<Action.VasuTemplate> = getDefaults(),
) : PermittedRoleActions {
    override fun globalActions(role: UserRole): Set<Action.Global> = global[role] ?: emptySet()
    override fun applicationActions(role: UserRole): Set<Action.Application> = application[role] ?: emptySet()
    override fun assistanceActionActions(role: UserRole): Set<Action.AssistanceAction> = assistanceAction[role] ?: emptySet()
    override fun assistanceNeedActions(role: UserRole): Set<Action.AssistanceNeed> = assistanceNeed[role] ?: emptySet()
    override fun backupCareActions(role: UserRole): Set<Action.BackupCare> = backupCare[role] ?: emptySet()
    override fun backupPickupActions(role: UserRole): Set<Action.BackupPickup> = backupPickup[role] ?: emptySet()
    override fun childActions(role: UserRole): Set<Action.Child> = child[role] ?: emptySet()
    override fun dailyNoteActions(role: UserRole): Set<Action.DailyNote> = dailyNote[role] ?: emptySet()
    override fun decisionActions(role: UserRole): Set<Action.Decision> = decision[role] ?: emptySet()
    override fun groupActions(role: UserRole): Set<Action.Group> = group[role] ?: emptySet()
    override fun mobileDeviceActions(role: UserRole): Set<Action.MobileDevice> = mobileDevice[role] ?: emptySet()
    override fun pairingActions(role: UserRole): Set<Action.Pairing> = pairing[role] ?: emptySet()
    override fun placementActions(role: UserRole): Set<Action.Placement> = placement[role] ?: emptySet()
    override fun serviceNeedActions(role: UserRole): Set<Action.ServiceNeed> = serviceNeed[role] ?: emptySet()
    override fun unitActions(role: UserRole): Set<Action.Unit> = unit[role] ?: emptySet()
    override fun vasuDocumentActions(role: UserRole): Set<Action.VasuDocument> = vasuDocument[role] ?: emptySet()
    override fun vasuTemplateActions(role: UserRole): Set<Action.VasuTemplate> = vasuTemplate[role] ?: emptySet()
}

typealias ActionsByRole<A> = Map<UserRole, Set<A>>
typealias RolesByAction<A> = Map<A, Set<UserRole>>

private inline fun <reified A> RolesByAction<A>.invert(): ActionsByRole<A> where A : Action, A : Enum<A> {
    val result = EnumMap<UserRole, EnumSet<A>>(UserRole::class.java)
    this.entries.forEach { (action, roles) ->
        roles.forEach { role ->
            result[role] = EnumSet.copyOf(result[role] ?: EnumSet.noneOf(A::class.java)).apply {
                add(action)
            }
        }
    }
    return result
}

private inline fun <reified A> getDefaults() where A : Action, A : Enum<A> =
    enumValues<A>().associateWith { it.defaultRoles() }.invert()
