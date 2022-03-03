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
    fun attachmentActions(role: UserRole): Set<Action.Attachment>
    fun childDailyNoteActions(role: UserRole): Set<Action.ChildDailyNote>
    fun childStickyNoteActions(role: UserRole): Set<Action.ChildStickyNote>
    fun decisionActions(role: UserRole): Set<Action.Decision>
    fun feeAlterationActions(role: UserRole): Set<Action.FeeAlteration>
    fun feeDecisionActions(role: UserRole): Set<Action.FeeDecision>
    fun feeThresholdsActions(role: UserRole): Set<Action.FeeThresholds>
    fun groupNoteActions(role: UserRole): Set<Action.GroupNote>
    fun groupPlacementActions(role: UserRole): Set<Action.GroupPlacement>
    fun incomeActions(role: UserRole): Set<Action.Income>
    fun incomeStatementActions(role: UserRole): Set<Action.IncomeStatement>
    fun messageDraftActions(role: UserRole): Set<Action.MessageDraft>
    fun mobileDeviceActions(role: UserRole): Set<Action.MobileDevice>
    fun pairingActions(role: UserRole): Set<Action.Pairing>
    fun parentshipActions(role: UserRole): Set<Action.Parentship>
    fun partnershipActions(role: UserRole): Set<Action.Partnership>
    fun personActions(role: UserRole): Set<Action.Person>
    fun unitActions(role: UserRole): Set<Action.Unit>
    fun vasuDocumentActions(role: UserRole): Set<Action.VasuDocument>
    fun vasuDocumentFollowupActions(role: UserRole): Set<Action.VasuDocumentFollowup>
    fun vasuTemplateActions(role: UserRole): Set<Action.VasuTemplate>
    fun voucherValueDecisionActions(role: UserRole): Set<Action.VoucherValueDecision>
}

/**
 * Role → action mapping based on static data.
 *
 * Uses system defaults, unless some mappings are overridden using constructor parameters
 */
class StaticPermittedRoleActions(
    val attachment: ActionsByRole<Action.Attachment> = getDefaults(),
    val childDailyNote: ActionsByRole<Action.ChildDailyNote> = getDefaults(),
    val childStickyNote: ActionsByRole<Action.ChildStickyNote> = getDefaults(),
    val decision: ActionsByRole<Action.Decision> = getDefaults(),
    val feeAlteration: ActionsByRole<Action.FeeAlteration> = getDefaults(),
    val feeDecision: ActionsByRole<Action.FeeDecision> = getDefaults(),
    val feeThresholds: ActionsByRole<Action.FeeThresholds> = getDefaults(),
    val groupNote: ActionsByRole<Action.GroupNote> = getDefaults(),
    val groupPlacement: ActionsByRole<Action.GroupPlacement> = getDefaults(),
    val income: ActionsByRole<Action.Income> = getDefaults(),
    val incomeStatement: ActionsByRole<Action.IncomeStatement> = getDefaults(),
    val messageDraft: ActionsByRole<Action.MessageDraft> = getDefaults(),
    val mobileDevice: ActionsByRole<Action.MobileDevice> = getDefaults(),
    val pairing: ActionsByRole<Action.Pairing> = getDefaults(),
    val parentship: ActionsByRole<Action.Parentship> = getDefaults(),
    val partnership: ActionsByRole<Action.Partnership> = getDefaults(),
    val person: ActionsByRole<Action.Person> = getDefaults(),
    val unit: ActionsByRole<Action.Unit> = getDefaults(),
    val vasuDocument: ActionsByRole<Action.VasuDocument> = getDefaults(),
    val vasuDocumentFollowup: ActionsByRole<Action.VasuDocumentFollowup> = getDefaults(),
    val vasuTemplate: ActionsByRole<Action.VasuTemplate> = getDefaults(),
    val voucherValueDecision: ActionsByRole<Action.VoucherValueDecision> = getDefaults(),
) : PermittedRoleActions {
    override fun attachmentActions(role: UserRole): Set<Action.Attachment> = attachment[role] ?: emptySet()
    override fun childDailyNoteActions(role: UserRole): Set<Action.ChildDailyNote> = childDailyNote[role] ?: emptySet()
    override fun childStickyNoteActions(role: UserRole): Set<Action.ChildStickyNote> = childStickyNote[role] ?: emptySet()
    override fun decisionActions(role: UserRole): Set<Action.Decision> = decision[role] ?: emptySet()
    override fun feeAlterationActions(role: UserRole): Set<Action.FeeAlteration> = feeAlteration[role] ?: emptySet()
    override fun feeDecisionActions(role: UserRole): Set<Action.FeeDecision> = feeDecision[role] ?: emptySet()
    override fun feeThresholdsActions(role: UserRole): Set<Action.FeeThresholds> = feeThresholds[role] ?: emptySet()
    override fun groupNoteActions(role: UserRole): Set<Action.GroupNote> = groupNote[role] ?: emptySet()
    override fun groupPlacementActions(role: UserRole): Set<Action.GroupPlacement> = groupPlacement[role] ?: emptySet()
    override fun incomeActions(role: UserRole): Set<Action.Income> = income[role] ?: emptySet()
    override fun incomeStatementActions(role: UserRole): Set<Action.IncomeStatement> = incomeStatement[role] ?: emptySet()
    override fun messageDraftActions(role: UserRole): Set<Action.MessageDraft> = messageDraft[role] ?: emptySet()
    override fun mobileDeviceActions(role: UserRole): Set<Action.MobileDevice> = mobileDevice[role] ?: emptySet()
    override fun pairingActions(role: UserRole): Set<Action.Pairing> = pairing[role] ?: emptySet()
    override fun parentshipActions(role: UserRole): Set<Action.Parentship> = parentship[role] ?: emptySet()
    override fun partnershipActions(role: UserRole): Set<Action.Partnership> = partnership[role] ?: emptySet()
    override fun personActions(role: UserRole): Set<Action.Person> = person[role] ?: emptySet()
    override fun unitActions(role: UserRole): Set<Action.Unit> = unit[role] ?: emptySet()
    override fun vasuDocumentActions(role: UserRole): Set<Action.VasuDocument> = vasuDocument[role] ?: emptySet()
    override fun vasuDocumentFollowupActions(role: UserRole): Set<Action.VasuDocumentFollowup> = vasuDocumentFollowup[role] ?: emptySet()
    override fun vasuTemplateActions(role: UserRole): Set<Action.VasuTemplate> = vasuTemplate[role] ?: emptySet()
    override fun voucherValueDecisionActions(role: UserRole): Set<Action.VoucherValueDecision> = voucherValueDecision[role] ?: emptySet()
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

private inline fun <reified A> getDefaults() where A : Action.LegacyAction, A : Enum<A> =
    enumValues<A>().associateWith { it.defaultRoles() }.invert()
