// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.utils.enumSetOf
import java.util.EnumMap
import java.util.EnumSet

/**
 * Role → action mapping
 */
interface PermittedRoleActions {
    fun globalActions(role: UserRole): Set<Action.Global>
    fun applicationActions(role: UserRole): Set<Action.Application>
    fun applicationNoteActions(role: UserRole): Set<Action.ApplicationNote>
    fun assistanceActionActions(role: UserRole): Set<Action.AssistanceAction>
    fun assistanceNeedActions(role: UserRole): Set<Action.AssistanceNeed>
    fun attachmentActions(role: UserRole): Set<Action.Attachment>
    fun backupCareActions(role: UserRole): Set<Action.BackupCare>
    fun backupPickupActions(role: UserRole): Set<Action.BackupPickup>
    fun childActions(role: UserRole): Set<Action.Child>
    fun childDailyNoteActions(role: UserRole): Set<Action.ChildDailyNote>
    fun childStickyNoteActions(role: UserRole): Set<Action.ChildStickyNote>
    fun childImageActions(role: UserRole): Set<Action.ChildImage>
    fun decisionActions(role: UserRole): Set<Action.Decision>
    fun feeThresholdsActions(role: UserRole): Set<Action.FeeThresholds>
    fun groupActions(role: UserRole): Set<Action.Group>
    fun groupNoteActions(role: UserRole): Set<Action.GroupNote>
    fun groupPlacementActions(role: UserRole): Set<Action.GroupPlacement>
    fun incomeStatementActions(role: UserRole): Set<Action.IncomeStatement>
    fun messageDraftActions(role: UserRole): Set<Action.MessageDraft>
    fun mobileDeviceActions(role: UserRole): Set<Action.MobileDevice>
    fun pairingActions(role: UserRole): Set<Action.Pairing>
    fun parentshipActions(role: UserRole): Set<Action.Parentship>
    fun partnershipActions(role: UserRole): Set<Action.Partnership>
    fun pedagogicalDocumentActions(role: UserRole): Set<Action.PedagogicalDocument>
    fun personActions(role: UserRole): Set<Action.Person>
    fun placementActions(role: UserRole): Set<Action.Placement>
    fun serviceNeedActions(role: UserRole): Set<Action.ServiceNeed>
    fun unitActions(role: UserRole): Set<Action.Unit>
    fun vasuDocumentActions(role: UserRole): Set<Action.VasuDocument>
    fun vasuDocumentFollowupActions(role: UserRole): Set<Action.VasuDocumentFollowup>
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
    val applicationNote: ActionsByRole<Action.ApplicationNote> = getDefaults(),
    val assistanceAction: ActionsByRole<Action.AssistanceAction> = getDefaults(),
    val assistanceNeed: ActionsByRole<Action.AssistanceNeed> = getDefaults(),
    val attachment: ActionsByRole<Action.Attachment> = getDefaults(),
    val backupCare: ActionsByRole<Action.BackupCare> = getDefaults(),
    val backupPickup: ActionsByRole<Action.BackupPickup> = getDefaults(),
    val child: ActionsByRole<Action.Child> = getDefaults(),
    val childDailyNote: ActionsByRole<Action.ChildDailyNote> = getDefaults(),
    val childStickyNote: ActionsByRole<Action.ChildStickyNote> = getDefaults(),
    val childImage: ActionsByRole<Action.ChildImage> = getDefaults(),
    val decision: ActionsByRole<Action.Decision> = getDefaults(),
    val feeThresholds: ActionsByRole<Action.FeeThresholds> = getDefaults(),
    val group: ActionsByRole<Action.Group> = getDefaults(),
    val groupNote: ActionsByRole<Action.GroupNote> = getDefaults(),
    val groupPlacement: ActionsByRole<Action.GroupPlacement> = getDefaults(),
    val incomeStatement: ActionsByRole<Action.IncomeStatement> = getDefaults(),
    val messageDraft: ActionsByRole<Action.MessageDraft> = getDefaults(),
    val mobileDevice: ActionsByRole<Action.MobileDevice> = getDefaults(),
    val pairing: ActionsByRole<Action.Pairing> = getDefaults(),
    val parentship: ActionsByRole<Action.Parentship> = getDefaults(),
    val partnership: ActionsByRole<Action.Partnership> = getDefaults(),
    val pedagogicalDocument: ActionsByRole<Action.PedagogicalDocument> = getDefaults(),
    val person: ActionsByRole<Action.Person> = getDefaults(),
    val placement: ActionsByRole<Action.Placement> = getDefaults(),
    val serviceNeed: ActionsByRole<Action.ServiceNeed> = getDefaults(),
    val unit: ActionsByRole<Action.Unit> = getDefaults(),
    val vasuDocument: ActionsByRole<Action.VasuDocument> = getDefaults(),
    val vasuDocumentFollowup: ActionsByRole<Action.VasuDocumentFollowup> = getDefaults(),
    val vasuTemplate: ActionsByRole<Action.VasuTemplate> = getDefaults(),
) : PermittedRoleActions {
    override fun globalActions(role: UserRole): Set<Action.Global> = global[role] ?: emptySet()
    override fun applicationActions(role: UserRole): Set<Action.Application> = application[role] ?: emptySet()
    override fun applicationNoteActions(role: UserRole): Set<Action.ApplicationNote> = applicationNote[role] ?: emptySet()
    override fun assistanceActionActions(role: UserRole): Set<Action.AssistanceAction> = assistanceAction[role] ?: emptySet()
    override fun assistanceNeedActions(role: UserRole): Set<Action.AssistanceNeed> = assistanceNeed[role] ?: emptySet()
    override fun attachmentActions(role: UserRole): Set<Action.Attachment> = attachment[role] ?: emptySet()
    override fun backupCareActions(role: UserRole): Set<Action.BackupCare> = backupCare[role] ?: emptySet()
    override fun backupPickupActions(role: UserRole): Set<Action.BackupPickup> = backupPickup[role] ?: emptySet()
    override fun childActions(role: UserRole): Set<Action.Child> = child[role] ?: emptySet()
    override fun childDailyNoteActions(role: UserRole): Set<Action.ChildDailyNote> = childDailyNote[role] ?: emptySet()
    override fun childStickyNoteActions(role: UserRole): Set<Action.ChildStickyNote> = childStickyNote[role] ?: emptySet()
    override fun childImageActions(role: UserRole): Set<Action.ChildImage> = childImage[role] ?: emptySet()
    override fun decisionActions(role: UserRole): Set<Action.Decision> = decision[role] ?: emptySet()
    override fun feeThresholdsActions(role: UserRole): Set<Action.FeeThresholds> = feeThresholds[role] ?: emptySet()
    override fun groupActions(role: UserRole): Set<Action.Group> = group[role] ?: emptySet()
    override fun groupNoteActions(role: UserRole): Set<Action.GroupNote> = groupNote[role] ?: emptySet()
    override fun groupPlacementActions(role: UserRole): Set<Action.GroupPlacement> = groupPlacement[role] ?: emptySet()
    override fun incomeStatementActions(role: UserRole): Set<Action.IncomeStatement> = incomeStatement[role] ?: emptySet()
    override fun messageDraftActions(role: UserRole): Set<Action.MessageDraft> = messageDraft[role] ?: emptySet()
    override fun mobileDeviceActions(role: UserRole): Set<Action.MobileDevice> = mobileDevice[role] ?: emptySet()
    override fun pairingActions(role: UserRole): Set<Action.Pairing> = pairing[role] ?: emptySet()
    override fun parentshipActions(role: UserRole): Set<Action.Parentship> = parentship[role] ?: emptySet()
    override fun partnershipActions(role: UserRole): Set<Action.Partnership> = partnership[role] ?: emptySet()
    override fun pedagogicalDocumentActions(role: UserRole): Set<Action.PedagogicalDocument> = pedagogicalDocument[role] ?: emptySet()
    override fun personActions(role: UserRole): Set<Action.Person> = person[role] ?: emptySet()
    override fun placementActions(role: UserRole): Set<Action.Placement> = placement[role] ?: emptySet()
    override fun serviceNeedActions(role: UserRole): Set<Action.ServiceNeed> = serviceNeed[role] ?: emptySet()
    override fun unitActions(role: UserRole): Set<Action.Unit> = unit[role] ?: emptySet()
    override fun vasuDocumentActions(role: UserRole): Set<Action.VasuDocument> = vasuDocument[role] ?: emptySet()
    override fun vasuDocumentFollowupActions(role: UserRole): Set<Action.VasuDocumentFollowup> = vasuDocumentFollowup[role] ?: emptySet()
    override fun vasuTemplateActions(role: UserRole): Set<Action.VasuTemplate> = vasuTemplate[role] ?: emptySet()
}

typealias ActionsByRole<A> = Map<UserRole, Set<A>>
typealias RolesByAction<A> = Map<A, Set<UserRole>>

private inline fun <reified A> RolesByAction<A>.invert(): ActionsByRole<A> where A : Action, A : Enum<A> {
    val result = EnumMap<UserRole, EnumSet<A>>(UserRole::class.java)
    this.entries.forEach { (action, roles) ->
        roles.forEach { role ->
            result[role] = EnumSet.copyOf(result[role] ?: enumSetOf()).apply {
                add(action)
            }
        }
    }
    return result
}

private inline fun <reified A> getDefaults() where A : Action, A : Enum<A> =
    enumValues<A>().associateWith { it.defaultRoles() }.invert()
