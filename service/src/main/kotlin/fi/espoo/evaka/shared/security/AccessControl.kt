// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.assistanceaction.getAssistanceActionById
import fi.espoo.evaka.assistanceneed.getAssistanceNeedById
import fi.espoo.evaka.attachment.citizenHasPermissionThroughPedagogicalDocument
import fi.espoo.evaka.attachment.isOwnAttachment
import fi.espoo.evaka.attachment.wasUploadedByAnyEmployee
import fi.espoo.evaka.childimages.hasPermissionForChildImage
import fi.espoo.evaka.incomestatement.isChildIncomeStatement
import fi.espoo.evaka.incomestatement.isOwnIncomeStatement
import fi.espoo.evaka.messaging.hasPermissionForAttachmentThroughMessageContent
import fi.espoo.evaka.messaging.hasPermissionForAttachmentThroughMessageDraft
import fi.espoo.evaka.messaging.hasPermissionForMessageDraft
import fi.espoo.evaka.pedagogicaldocument.citizenHasPermissionForPedagogicalDocument
import fi.espoo.evaka.pis.employeePinIsCorrect
import fi.espoo.evaka.pis.service.getGuardianChildIds
import fi.espoo.evaka.pis.updateEmployeePinFailureCountAndCheckIfLocked
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.getPlacementsForChild
import fi.espoo.evaka.shared.ApplicationNoteId
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.BackupPickupId
import fi.espoo.evaka.shared.ChildDailyNoteId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.ChildStickyNoteId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupNoteId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.VasuDocumentFollowupEntryId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.security.actionrule.ActionRuleMapping
import fi.espoo.evaka.shared.security.actionrule.DatabaseActionRule
import fi.espoo.evaka.shared.security.actionrule.StaticActionRule
import fi.espoo.evaka.shared.utils.enumSetOf
import fi.espoo.evaka.shared.utils.toEnumSet
import fi.espoo.evaka.vasu.getVasuFollowupEntries
import fi.espoo.evaka.vasu.getVasuFollowupEntry
import org.intellij.lang.annotations.Language
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo
import java.util.EnumSet
import java.util.UUID

class AccessControl(
    private val permittedRoleActions: PermittedRoleActions,
    private val actionRuleMapping: ActionRuleMapping,
    private val acl: AccessControlList,
    private val jdbi: Jdbi
) {
    private val applicationAttachment = ActionConfig(
        """
SELECT attachment.id, role
FROM attachment
JOIN placement_plan ON attachment.application_id = placement_plan.application_id
JOIN daycare ON placement_plan.unit_id = daycare.id AND daycare.round_the_clock
JOIN daycare_acl_view ON daycare.id = daycare_acl_view.daycare_id
WHERE employee_id = :userId
  AND attachment.type = 'EXTENDED_CARE'
        """.trimIndent(),
        "attachment.id",
        permittedRoleActions::attachmentActions,
    )

    private val assistanceAction = ActionConfig(
        """
SELECT ac.id, role
FROM child_acl_view acl
JOIN assistance_action ac ON acl.child_id = ac.child_id
WHERE acl.employee_id = :userId
        """.trimIndent(),
        "ac.id",
        permittedRoleActions::assistanceActionActions
    )

    private val assistanceNeed = ActionConfig(
        """
SELECT an.id, role
FROM child_acl_view acl
JOIN assistance_need an ON acl.child_id = an.child_id
WHERE acl.employee_id = :userId
        """.trimIndent(),
        "an.id",
        permittedRoleActions::assistanceNeedActions
    )

    private val backupPickup = ActionConfig(
        """
SELECT bp.id, role
FROM child_acl_view acl
JOIN backup_pickup bp ON acl.child_id = bp.child_id
WHERE acl.employee_id = :userId
        """.trimIndent(),
        "bp.id",
        permittedRoleActions::backupPickupActions
    )
    private val child = ActionConfig(
        """
SELECT child_id AS id, role
FROM child_acl_view
WHERE employee_id = :userId
        """.trimIndent(),
        "child_id",
        permittedRoleActions::childActions
    )
    private val childDailyNote = ActionConfig(
        """
SELECT cdn.id, role
FROM child_acl_view
JOIN child_daily_note cdn ON child_acl_view.child_id = cdn.child_id
WHERE employee_id = :userId
        """.trimIndent(),
        "cdn.id",
        permittedRoleActions::childDailyNoteActions
    )
    private val childImage = ActionConfig(
        """
SELECT img.id, role
FROM child_acl_view
JOIN child_images img ON child_acl_view.child_id = img.child_id
WHERE employee_id = :userId
        """.trimIndent(),
        "img.id",
        permittedRoleActions::childImageActions
    )
    private val childStickyNote = ActionConfig(
        """
SELECT csn.id, role
FROM child_acl_view
JOIN child_sticky_note csn ON child_acl_view.child_id = csn.child_id
WHERE employee_id = :userId
        """.trimIndent(),
        "csn.id",
        permittedRoleActions::childStickyNoteActions
    )
    private val decision = ActionConfig(
        """
SELECT decision.id, role
FROM decision
JOIN daycare_acl_view ON decision.unit_id = daycare_acl_view.daycare_id
WHERE employee_id = :userId
        """.trimIndent(),
        "decision.id",
        permittedRoleActions::decisionActions
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
    private val groupNote = ActionConfig(
        """
SELECT gn.id, role
FROM daycare_group_acl_view
JOIN group_note gn ON gn.group_id = daycare_group_acl_view.daycare_group_id
WHERE employee_id = :userId
        """.trimIndent(),
        "gn.id",
        permittedRoleActions::groupNoteActions
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
    private val mobileDevice = ActionConfig(
        """
SELECT d.id, role
FROM daycare_acl_view dav
JOIN mobile_device d ON dav.daycare_id = d.unit_id OR dav.employee_id = d.employee_id
WHERE dav.employee_id = :userId
        """.trimIndent(),
        "d.id",
        permittedRoleActions::mobileDeviceActions
    )
    private val pairing = ActionConfig(
        """
SELECT p.id, role
FROM daycare_acl_view dav
JOIN pairing p ON dav.daycare_id = p.unit_id OR dav.employee_id = p.employee_id
WHERE dav.employee_id = :userId
        """.trimIndent(),
        "p.id",
        permittedRoleActions::pairingActions
    )
    private val parentship = ActionConfig(
        """
SELECT fridge_child.id, role
FROM fridge_child
JOIN person_acl_view ON fridge_child.head_of_child = person_acl_view.person_id OR fridge_child.child_id = person_acl_view.person_id
WHERE employee_id = :userId
""",
        "fridge_child.id",
        permittedRoleActions::parentshipActions
    )
    private val partnership = ActionConfig(
        """
SELECT fridge_partner.partnership_id AS id, role
FROM fridge_partner
JOIN person_acl_view ON fridge_partner.person_id = person_acl_view.person_id
WHERE employee_id = :userId
""",
        "fridge_partner.partnership_id",
        permittedRoleActions::partnershipActions
    )
    private val pedagogicalAttachment = ActionConfig(
        """
SELECT attachment.id, role
FROM attachment
JOIN pedagogical_document pd ON attachment.pedagogical_document_id = pd.id
JOIN child_acl_view ON pd.child_id = child_acl_view.child_id
WHERE employee_id = :userId
        """.trimIndent(),
        "attachment.id",
        permittedRoleActions::attachmentActions,
    )
    private val person = ActionConfig(
        """
SELECT person_id AS id, role
FROM person_acl_view
WHERE employee_id = :userId
        """.trimIndent(),
        "person_id",
        permittedRoleActions::personActions
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
    private val serviceNeed = ActionConfig(
        """
SELECT service_need.id, role
FROM service_need
JOIN placement ON placement.id = service_need.placement_id
JOIN daycare_acl_view ON placement.unit_id = daycare_acl_view.daycare_id
WHERE employee_id = :userId
        """.trimIndent(),
        "service_need.id",
        permittedRoleActions::serviceNeedActions
    )
    private val vasuDocument = ActionConfig(
        """
SELECT curriculum_document.id AS id, role
FROM curriculum_document
JOIN child_acl_view ON curriculum_document.child_id = child_acl_view.child_id
WHERE employee_id = :userId
        """.trimIndent(),
        "curriculum_document.id",
        permittedRoleActions::vasuDocumentActions
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
        @Suppress("DEPRECATION")
        EmployeeFeatures(
            applications = user.hasOneOfRoles(
                UserRole.ADMIN,
                UserRole.SERVICE_WORKER,
                UserRole.SPECIAL_EDUCATION_TEACHER
            ),
            employees = user.hasOneOfRoles(UserRole.ADMIN),
            financeBasics = user.hasOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN),
            finance = user.hasOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN),
            holidayPeriods = user.hasOneOfRoles(UserRole.ADMIN),
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
                UserRole.REPORT_VIEWER,
                UserRole.SERVICE_WORKER,
                UserRole.FINANCE_ADMIN,
                UserRole.UNIT_SUPERVISOR,
                UserRole.SPECIAL_EDUCATION_TEACHER
            ),
            settings = user.isAdmin,
            unitFeatures = user.hasOneOfRoles(UserRole.ADMIN),
            units = user.hasOneOfRoles(
                UserRole.ADMIN,
                UserRole.SERVICE_WORKER,
                UserRole.FINANCE_ADMIN,
                UserRole.UNIT_SUPERVISOR,
                UserRole.STAFF,
                UserRole.SPECIAL_EDUCATION_TEACHER
            ),
            createUnits = hasPermissionFor(user, Action.Global.CREATE_UNIT),
            vasuTemplates = user.hasOneOfRoles(UserRole.ADMIN),
            personalMobileDevice = user.hasOneOfRoles(UserRole.UNIT_SUPERVISOR),

            // Everyone else except FINANCE_ADMIN
            pinCode = user.hasOneOfRoles(
                UserRole.ADMIN,
                UserRole.REPORT_VIEWER,
                UserRole.DIRECTOR,
                UserRole.SERVICE_WORKER,
                UserRole.UNIT_SUPERVISOR,
                UserRole.STAFF,
                UserRole.SPECIAL_EDUCATION_TEACHER,
                UserRole.GROUP_STAFF,
            )
        )

    private fun isMessagingEnabled(user: AuthenticatedUser): Boolean {
        @Suppress("DEPRECATION")
        return acl.getRolesForPilotFeature(user, PilotFeature.MESSAGING)
            .hasOneOfRoles(UserRole.STAFF, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER)
    }

    fun requireGuardian(user: AuthenticatedUser, childIds: Set<ChildId>) {
        val dependants = Database(jdbi).connect { db ->
            db.read {
                it.createQuery(
                    "SELECT child_id FROM guardian g WHERE g.guardian_id = :userId"
                ).bind("userId", user.id).mapTo<ChildId>().list()
            }
        }

        if (childIds.any { !dependants.contains(it) }) {
            throw Forbidden("Not a guardian of a child")
        }
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.StaticAction) = checkPermissionFor(user, action).assert()
    fun hasPermissionFor(user: AuthenticatedUser, action: Action.StaticAction): Boolean = checkPermissionFor(user, action).isPermitted()
    private fun checkPermissionFor(user: AuthenticatedUser, action: Action.StaticAction): AccessControlDecision {
        if (user.isAdmin) {
            return AccessControlDecision.PermittedToAdmin
        }
        for (rule in actionRuleMapping.rulesOf(action)) {
            if (rule.isPermitted(user)) {
                return AccessControlDecision.Permitted(rule)
            }
        }
        return AccessControlDecision.None
    }

    fun getPermittedGlobalActions(user: AuthenticatedUser): Set<Action.Global> {
        if (user.isAdmin) {
            return EnumSet.allOf(Action.Global::class.java)
        }

        return Action.Global.values()
            .filter { action -> checkPermissionFor(user, action).isPermitted() }
            .toEnumSet()
    }

    fun <T> requirePermissionFor(user: AuthenticatedUser, action: Action.ScopedAction<T>, vararg targets: T) = Database(jdbi).connect { dbc ->
        checkPermissionFor(dbc, user, action, targets.asIterable()).values.forEach { it.assert() }
    }

    fun <T> hasPermissionFor(user: AuthenticatedUser, action: Action.ScopedAction<T>, vararg targets: T): Boolean = Database(jdbi).connect { dbc ->
        checkPermissionFor(dbc, user, action, targets.asIterable()).values.all { it.isPermitted() }
    }

    private fun <T> checkPermissionFor(
        dbc: Database.Connection,
        user: AuthenticatedUser,
        action: Action.ScopedAction<T>,
        targets: Iterable<T>
    ): Map<T, AccessControlDecision> {
        if (user.isAdmin) {
            return targets.associateWith { AccessControlDecision.PermittedToAdmin }
        }
        val decisions = Decisions(targets.toSet())
        val rules = actionRuleMapping.rulesOf(action).sortedByDescending { it is StaticActionRule }.iterator()
        while (rules.hasNext() && decisions.undecided.isNotEmpty()) {
            when (val rule = rules.next()) {
                is StaticActionRule -> if (rule.isPermitted(user)) {
                    decisions.decideAll(AccessControlDecision.Permitted(rule))
                }
                is DatabaseActionRule<in T, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val query = rule.query as DatabaseActionRule.Query<T, Any?>
                    dbc.read { tx -> query.execute(tx, user, decisions.undecided) }
                        .forEach { (target, deferred) -> decisions.decide(target, deferred.evaluate(rule.params)) }
                }
            }
        }
        return decisions.finish()
    }

    inline fun <T, reified A> getPermittedActions(
        tx: Database.Read,
        user: AuthenticatedUser,
        targets: Iterable<T>
    ) where A : Action.ScopedAction<T>, A : Enum<A> = getPermittedActions(tx, user, A::class.java, targets)

    fun <T, A> getPermittedActions(
        tx: Database.Read,
        user: AuthenticatedUser,
        actionClass: Class<A>,
        targets: Iterable<T>
    ): Map<T, Set<A>> where A : Action.ScopedAction<T>, A : Enum<A> {
        val allActions: Set<A> = EnumSet.allOf(actionClass)
        if (user.isAdmin) {
            return targets.associateWith { allActions }
        }
        val undecidedActions = EnumSet.allOf(actionClass)
        val permittedActions = EnumSet.noneOf(actionClass)
        for (action in allActions) {
            val staticRules = actionRuleMapping.rulesOf(action).mapNotNull { it as? StaticActionRule }
            if (staticRules.any { it.isPermitted(user) }) {
                permittedActions += action
                undecidedActions -= action
            }
        }

        val databaseRuleTypes = EnumSet.copyOf(undecidedActions)
            .flatMap { action ->
                actionRuleMapping.rulesOf(action).mapNotNull { it as? DatabaseActionRule<in T, *> }
            }
            .distinctBy { it.params.javaClass }
            .iterator()

        val result = targets.associateWith { EnumSet.copyOf(permittedActions) }

        while (undecidedActions.isNotEmpty() && databaseRuleTypes.hasNext()) {
            val ruleType = databaseRuleTypes.next()
            @Suppress("UNCHECKED_CAST")
            val deferred = ruleType.query.execute(tx, user, targets.toSet()) as Map<T, DatabaseActionRule.Deferred<Any?>>

            for (action in EnumSet.copyOf(undecidedActions)) {
                val compatibleRules = actionRuleMapping.rulesOf(action)
                    .mapNotNull { it as? DatabaseActionRule<in T, *> }
                    .filter { it.params.javaClass == ruleType.params.javaClass }
                for (rule in compatibleRules) {
                    for (target in targets) {
                        if (deferred[target]?.evaluate(rule.params)?.isPermitted() == true) {
                            result[target]?.add(action)
                        }
                    }
                }
            }
        }
        return result
    }

    private class Decisions<T>(targets: Iterable<T>) {
        private val result = mutableMapOf<T, AccessControlDecision>()
        var undecided: Set<T> = targets.toSet()
            private set

        fun decideAll(decision: AccessControlDecision) {
            if (decision != AccessControlDecision.None) {
                result += undecided.associateWith { decision }
                undecided = emptySet()
            }
        }
        fun decide(target: T, decision: AccessControlDecision) {
            if (decision != AccessControlDecision.None) {
                result[target] = decision
                undecided = undecided - target
            }
        }
        fun finish(): Map<T, AccessControlDecision> = result + undecided.associateWith { AccessControlDecision.None }
    }

    fun <I> requirePermissionFor(user: AuthenticatedUser, action: Action.LegacyScopedAction<I>, vararg ids: I) {
        if (!hasPermissionFor(user, action, *ids)) {
            throw Forbidden()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <A : Action.LegacyScopedAction<I>, I> hasPermissionFor(user: AuthenticatedUser, action: A, vararg ids: I): Boolean =
        when (action) {
            is Action.AssistanceAction -> ids.all { id -> hasPermissionForInternal(user, action, id as AssistanceActionId) }
            is Action.AssistanceNeed -> ids.all { id -> hasPermissionForInternal(user, action, id as AssistanceNeedId) }
            is Action.Attachment -> ids.all { id -> hasPermissionForInternal(user, action, id as AttachmentId) }
            is Action.BackupPickup -> this.backupPickup.hasPermission(user, action, *ids as Array<BackupPickupId>)
            is Action.ChildDailyNote -> this.childDailyNote.hasPermission(user, action, *ids as Array<ChildDailyNoteId>)
            is Action.ChildImage -> ids.all { id -> hasPermissionForInternal(user, action, id as ChildImageId) }
            is Action.ChildStickyNote -> this.childStickyNote.hasPermission(user, action, *ids as Array<ChildStickyNoteId>)
            is Action.Decision -> this.decision.hasPermission(user, action, *ids as Array<DecisionId>)
            is Action.FeeAlteration -> hasPermissionUsingGlobalRoles(user, action, mapping = permittedRoleActions::feeAlterationActions)
            is Action.FeeDecision -> hasPermissionUsingGlobalRoles(user, action, mapping = permittedRoleActions::feeDecisionActions)
            is Action.FeeThresholds -> hasPermissionUsingGlobalRoles(user, action, mapping = permittedRoleActions::feeThresholdsActions)
            is Action.GroupNote -> this.groupNote.hasPermission(user, action, *ids as Array<GroupNoteId>)
            is Action.GroupPlacement -> this.groupPlacement.hasPermission(user, action, *ids as Array<GroupPlacementId>)
            is Action.Group -> this.group.hasPermission(user, action, *ids as Array<GroupId>)
            is Action.Income -> hasPermissionUsingGlobalRoles(user, action, mapping = permittedRoleActions::incomeActions)
            is Action.Invoice -> hasPermissionUsingGlobalRoles(user, action, mapping = permittedRoleActions::invoiceActions)
            is Action.MessageDraft -> ids.all { id -> hasPermissionForInternal(user, action, id as MessageDraftId) }
            is Action.MobileDevice -> this.mobileDevice.hasPermission(user, action, *ids as Array<MobileDeviceId>)
            is Action.Pairing -> this.pairing.hasPermission(user, action, *ids as Array<PairingId>)
            is Action.Parentship -> this.parentship.hasPermission(user, action, *ids as Array<ParentshipId>)
            is Action.Partnership -> this.partnership.hasPermission(user, action, *ids as Array<PartnershipId>)
            is Action.Person -> ids.all { id -> hasPermissionForInternal(user, action, id as PersonId) }
            is Action.Placement -> this.placement.hasPermission(user, action, *ids as Array<PlacementId>)
            is Action.ServiceNeed -> this.serviceNeed.hasPermission(user, action, *ids as Array<ServiceNeedId>)
            is Action.Unit -> this.unit.hasPermission(user, action, *ids as Array<DaycareId>)
            is Action.VasuDocument -> this.vasuDocument.hasPermission(user, action, *ids as Array<VasuDocumentId>)
            is Action.VasuTemplate -> hasPermissionUsingGlobalRoles(user, action, permittedRoleActions::vasuTemplateActions)
            is Action.VoucherValueDecision -> hasPermissionUsingGlobalRoles(user, action, mapping = permittedRoleActions::voucherValueDecisionActions)
            else -> error("Unsupported action type")
        }.exhaust()

    fun getPermittedApplicationNoteActions(
        user: AuthenticatedUser,
        ids: Collection<ApplicationNoteId>
    ): Map<ApplicationNoteId, Set<Action.ApplicationNote>> = ids.associateWith { id ->
        Action.ApplicationNote.values()
            .filter { action -> hasPermissionFor(user, action, id) }
            .toSet()
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.AssistanceAction, id: AssistanceActionId) {
        assertPermission(
            user = user,
            getAclRoles = { @Suppress("DEPRECATION") acl.getRolesForAssistanceAction(user, id).roles },
            action = action,
            mapping = permittedRoleActions::assistanceActionActions
        )
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.AssistanceNeed, id: AssistanceNeedId) {
        assertPermission(
            user = user,
            getAclRoles = { @Suppress("DEPRECATION") acl.getRolesForAssistanceNeed(user, id).roles },
            action = action,
            mapping = permittedRoleActions::assistanceNeedActions
        )
    }

    private fun hasPermissionForInternal(
        user: AuthenticatedUser,
        action: Action.AssistanceAction,
        id: AssistanceActionId
    ) =
        when (user) {
            is AuthenticatedUser.Employee -> when (action) {
                Action.AssistanceAction.READ_PRE_PRESCHOOL_ASSISTANCE_ACTION ->
                    // If child is or has been in preschool, do not show pre preschool daycare assistance actions for non admin or SEO
                    if (this.assistanceAction.hasPermission(user, action, id)) true
                    else {
                        Database(jdbi).connect { db ->
                            db.read {
                                it.getAssistanceActionById(id).let { assistanceAction ->
                                    val preschoolPlacements = it.getPlacementsForChild(assistanceAction.childId).filter {
                                        (it.type == PlacementType.PRESCHOOL || it.type == PlacementType.PRESCHOOL_DAYCARE)
                                    }
                                    preschoolPlacements.isEmpty() ||
                                        preschoolPlacements.any { placement ->
                                            placement.startDate.isBefore(assistanceAction.startDate) || placement.startDate.equals(assistanceAction.startDate)
                                        }
                                }
                            }
                        }
                    }
                else -> hasPermissionUsingAllRoles(user, action, permittedRoleActions::assistanceActionActions)
            }
            else -> hasPermissionUsingAllRoles(user, action, permittedRoleActions::assistanceActionActions)
        }

    private fun hasPermissionForInternal(
        user: AuthenticatedUser,
        action: Action.AssistanceNeed,
        id: AssistanceNeedId
    ) =
        when (user) {
            is AuthenticatedUser.Employee -> when (action) {
                Action.AssistanceNeed.READ_PRE_PRESCHOOL_ASSISTANCE_NEED ->
                    // If child is or has been in preschool, do not show pre preschool daycare assistance actions for non admin or SEO
                    if (this.assistanceNeed.hasPermission(user, action, id)) true
                    else {
                        Database(jdbi).connect { db ->
                            db.read {
                                it.getAssistanceNeedById(id).let { assistanceNeed ->
                                    val preschoolPlacements = it.getPlacementsForChild(assistanceNeed.childId).filter {
                                        (it.type == PlacementType.PRESCHOOL || it.type == PlacementType.PRESCHOOL_DAYCARE)
                                    }
                                    preschoolPlacements.isEmpty() ||
                                        preschoolPlacements.any { placement ->
                                            placement.startDate.isBefore(assistanceNeed.startDate) || placement.startDate.equals(assistanceNeed.startDate)
                                        }
                                }
                            }
                        }
                    }
                else -> hasPermissionUsingAllRoles(user, action, permittedRoleActions::assistanceNeedActions)
            }
            else -> hasPermissionUsingAllRoles(user, action, permittedRoleActions::assistanceNeedActions)
        }

    private fun hasPermissionForInternal(user: AuthenticatedUser, action: Action.Attachment, id: AttachmentId) =
        when (user) {
            is AuthenticatedUser.Citizen -> when (action) {
                Action.Attachment.READ_APPLICATION_ATTACHMENT,
                Action.Attachment.DELETE_APPLICATION_ATTACHMENT,
                Action.Attachment.READ_INCOME_STATEMENT_ATTACHMENT,
                Action.Attachment.DELETE_INCOME_STATEMENT_ATTACHMENT ->
                    Database(jdbi).connect { db -> db.read { it.isOwnAttachment(id, user) } }
                Action.Attachment.READ_MESSAGE_CONTENT_ATTACHMENT ->
                    Database(jdbi).connect { db ->
                        db.read {
                            it.hasPermissionForAttachmentThroughMessageContent(
                                user,
                                id
                            )
                        }
                    }
                Action.Attachment.READ_MESSAGE_DRAFT_ATTACHMENT,
                Action.Attachment.DELETE_MESSAGE_CONTENT_ATTACHMENT,
                Action.Attachment.DELETE_MESSAGE_DRAFT_ATTACHMENT,
                Action.Attachment.DELETE_PEDAGOGICAL_DOCUMENT_ATTACHMENT -> false
                Action.Attachment.READ_PEDAGOGICAL_DOCUMENT_ATTACHMENT ->
                    Database(jdbi).connect { db ->
                        db.read {
                            it.citizenHasPermissionThroughPedagogicalDocument(
                                user,
                                id
                            )
                        }
                    }
            }
            is AuthenticatedUser.WeakCitizen -> when (action) {
                Action.Attachment.READ_MESSAGE_CONTENT_ATTACHMENT ->
                    Database(jdbi).connect { db ->
                        db.read {
                            it.hasPermissionForAttachmentThroughMessageContent(
                                user,
                                id
                            )
                        }
                    }
                Action.Attachment.READ_APPLICATION_ATTACHMENT,
                Action.Attachment.READ_INCOME_STATEMENT_ATTACHMENT,
                Action.Attachment.READ_MESSAGE_DRAFT_ATTACHMENT,
                Action.Attachment.DELETE_APPLICATION_ATTACHMENT,
                Action.Attachment.DELETE_INCOME_STATEMENT_ATTACHMENT,
                Action.Attachment.DELETE_MESSAGE_CONTENT_ATTACHMENT,
                Action.Attachment.DELETE_MESSAGE_DRAFT_ATTACHMENT,
                Action.Attachment.READ_PEDAGOGICAL_DOCUMENT_ATTACHMENT,
                Action.Attachment.DELETE_PEDAGOGICAL_DOCUMENT_ATTACHMENT -> false
            }
            is AuthenticatedUser.Employee -> when (action) {
                Action.Attachment.READ_APPLICATION_ATTACHMENT ->
                    this.applicationAttachment.hasPermission(user, action, id)
                Action.Attachment.DELETE_APPLICATION_ATTACHMENT ->
                    this.applicationAttachment.hasPermission(
                        user,
                        action,
                        id
                    ) && Database(jdbi).connect { db -> db.read { it.wasUploadedByAnyEmployee(id) } }
                Action.Attachment.READ_INCOME_STATEMENT_ATTACHMENT ->
                    hasPermissionUsingGlobalRoles(user, action, permittedRoleActions::attachmentActions)
                Action.Attachment.DELETE_INCOME_STATEMENT_ATTACHMENT ->
                    hasPermissionUsingGlobalRoles(user, action, permittedRoleActions::attachmentActions) && Database(
                        jdbi
                    ).connect { db -> db.read { it.wasUploadedByAnyEmployee(id) } }
                Action.Attachment.READ_MESSAGE_DRAFT_ATTACHMENT,
                Action.Attachment.DELETE_MESSAGE_DRAFT_ATTACHMENT ->
                    Database(jdbi).connect { db ->
                        db.read {
                            it.hasPermissionForAttachmentThroughMessageDraft(
                                user,
                                id
                            )
                        }
                    }
                Action.Attachment.READ_MESSAGE_CONTENT_ATTACHMENT ->
                    Database(jdbi).connect { db ->
                        db.read {
                            it.hasPermissionForAttachmentThroughMessageContent(
                                user,
                                id
                            )
                        }
                    }
                Action.Attachment.DELETE_MESSAGE_CONTENT_ATTACHMENT -> false
                Action.Attachment.READ_PEDAGOGICAL_DOCUMENT_ATTACHMENT ->
                    this.pedagogicalAttachment.hasPermission(user, action, id)
                Action.Attachment.DELETE_PEDAGOGICAL_DOCUMENT_ATTACHMENT ->
                    this.pedagogicalAttachment.hasPermission(user, action, id)
            }
            is AuthenticatedUser.MobileDevice -> false
            AuthenticatedUser.SystemInternalUser -> false
        }

    private fun hasPermissionForInternal(user: AuthenticatedUser, action: Action.ChildImage, id: ChildImageId): Boolean =
        when (user) {
            is AuthenticatedUser.WeakCitizen -> false
            is AuthenticatedUser.Citizen -> when (action) {
                Action.ChildImage.DOWNLOAD -> Database(jdbi).connect { db -> db.read { it.hasPermissionForChildImage(user, id) } }
            }
            is AuthenticatedUser.MobileDevice,
            is AuthenticatedUser.Employee -> when (action) {
                Action.ChildImage.DOWNLOAD -> this.childImage.hasPermission(user, action, id)
            }
            AuthenticatedUser.SystemInternalUser -> false
        }

    fun getPermittedAssistanceActionActions(
        user: AuthenticatedUser,
        ids: Collection<AssistanceActionId>
    ): Map<AssistanceActionId, Set<Action.AssistanceAction>> = this.assistanceAction.getPermittedActions(user, ids)

    fun getPermittedAssistanceNeedActions(
        user: AuthenticatedUser,
        ids: Collection<AssistanceNeedId>
    ): Map<AssistanceNeedId, Set<Action.AssistanceNeed>> = this.assistanceNeed.getPermittedActions(user, ids)

    private fun requirePinLogin(user: AuthenticatedUser) {
        if (user is AuthenticatedUser.MobileDevice && user.employeeId == null) throw Forbidden(
            "PIN login required",
            "PIN_LOGIN_REQUIRED"
        )
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.Child, id: ChildId) {
        when (user) {
            is AuthenticatedUser.Employee,
            is AuthenticatedUser.MobileDevice -> {
                when (action) {
                    Action.Child.READ_SENSITIVE_INFO -> requirePinLogin(user)
                    else -> Unit
                }
                assertPermission(
                    user = user,
                    getAclRoles = { @Suppress("DEPRECATION") acl.getRolesForChild(user, id).roles },
                    action = action,
                    mapping = permittedRoleActions::childActions
                )
            }
            is AuthenticatedUser.Citizen -> {
                when (action) {
                    Action.Child.READ,
                    Action.Child.READ_PLACEMENT -> requireGuardian(user, setOf(id))
                    else -> throw Forbidden()
                }
            }
            else -> throw Forbidden()
        }
    }

    fun getPermittedChildActions(user: AuthenticatedUser, ids: Collection<ChildId>): Map<ChildId, Set<Action.Child>> =
        this.child.getPermittedActions(user, ids)

    fun getPermittedPersonActions(
        user: AuthenticatedUser,
        ids: Collection<PersonId>
    ): Map<PersonId, Set<Action.Person>> = ids.associateWith { personId ->
        Action.Person.values()
            .filter { action -> hasPermissionForInternal(user, action, personId) }
            .toSet()
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.PedagogicalDocument, id: PedagogicalDocumentId) {
        when (user) {
            is AuthenticatedUser.Citizen -> when (action) {
                Action.PedagogicalDocument.READ -> {
                    val hasPermission = Database(jdbi).connect { db ->
                        db.read {
                            it.citizenHasPermissionForPedagogicalDocument(
                                user,
                                id
                            )
                        }
                    }
                    if (!hasPermission) throw Forbidden()
                }
                else -> throw Forbidden()
            }
            is AuthenticatedUser.Employee -> when (action) {
                Action.PedagogicalDocument.CREATE_ATTACHMENT,
                Action.PedagogicalDocument.DELETE,
                Action.PedagogicalDocument.READ,
                Action.PedagogicalDocument.UPDATE ->
                    assertPermission(
                        user = user,
                        getAclRoles = { @Suppress("DEPRECATION") acl.getRolesForPedagogicalDocument(user, id).roles },
                        action = action,
                        mapping = permittedRoleActions::pedagogicalDocumentActions
                    )
            }
            else -> throw Forbidden()
        }
    }

    fun getPermittedGroupActions(
        user: AuthenticatedUser,
        ids: Collection<GroupId>
    ): Map<GroupId, Set<Action.Group>> = this.group.getPermittedActions(user, ids)

    fun getPermittedGroupPlacementActions(
        user: AuthenticatedUser,
        ids: Collection<GroupPlacementId>
    ): Map<GroupPlacementId, Set<Action.GroupPlacement>> = this.groupPlacement.getPermittedActions(user, ids)

    private fun hasPermissionForInternal(
        user: AuthenticatedUser,
        action: Action.MessageDraft,
        id: MessageDraftId
    ): Boolean =
        when (user) {
            is AuthenticatedUser.Citizen -> false // drafts are employee-only
            is AuthenticatedUser.Employee -> when (action) {
                Action.MessageDraft.UPLOAD_ATTACHMENT -> Database(jdbi).connect { db ->
                    db.read {
                        it.hasPermissionForMessageDraft(
                            user,
                            id
                        )
                    }
                }
            }
            else -> false
        }

    private fun hasPermissionForInternal(
        user: AuthenticatedUser,
        action: Action.Person,
        id: PersonId
    ): Boolean = when (action) {
        Action.Person.ADD_SSN -> Database(jdbi).connect {
            val ssnAddingDisabled = it.read { tx ->
                tx.createQuery("SELECT ssn_adding_disabled FROM person WHERE id = :id")
                    .bind("id", id)
                    .mapTo<Boolean>()
                    .one()
            }
            if (ssnAddingDisabled) user.isAdmin else this.person.hasPermission(user, action, id)
        }
        else -> this.person.hasPermission(user, action, id)
    }

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.Placement, id: PlacementId) =
        when (user) {
            is AuthenticatedUser.Citizen -> when (action) {
                Action.Placement.TERMINATE -> {
                    Database(jdbi).connect {
                        val childId = it.read { tx ->
                            tx.createQuery("SELECT child_id FROM placement WHERE id = :id")
                                .bind("id", id)
                                .mapTo<ChildId>()
                                .one()
                        }
                        requireGuardian(user, setOf(childId))
                    }
                }
                else -> throw Forbidden()
            }
            is AuthenticatedUser.Employee -> if (!hasPermissionFor(user, action, id)) {
                throw Forbidden()
            } else Unit
            else -> throw Forbidden()
        }

    fun requirePermissionFor(
        user: AuthenticatedUser,
        action: Action.IncomeStatement,
        id: UUID? = null
    ) {
        when (user) {
            is AuthenticatedUser.Citizen -> when (action) {
                Action.IncomeStatement.READ,
                Action.IncomeStatement.READ_ALL_OWN,
                Action.IncomeStatement.READ_START_DATES,
                Action.IncomeStatement.CREATE,
                Action.IncomeStatement.UPDATE,
                Action.IncomeStatement.REMOVE,
                -> Unit

                Action.IncomeStatement.REMOVE_FOR_CHILD,
                Action.IncomeStatement.READ_CHILDS_START_DATES,
                Action.IncomeStatement.READ_ALL_CHILDS,
                Action.IncomeStatement.CREATE_FOR_CHILD,
                Action.IncomeStatement.UPDATE_FOR_CHILD -> {
                    Database(jdbi).connect { db ->
                        if (!db.read { it.getGuardianChildIds(PersonId(user.id)).contains(ChildId(id!!)) }) throw Forbidden()
                    }
                }

                Action.IncomeStatement.READ_CHILDS -> {
                    Database(jdbi).connect { db ->
                        if (!db.read { it.isChildIncomeStatement(user, IncomeStatementId(id!!)) }) throw Forbidden()
                    }
                }

                Action.IncomeStatement.UPLOAD_ATTACHMENT ->
                    Database(jdbi).connect { db ->
                        if (!db.read { it.isOwnIncomeStatement(user, IncomeStatementId(id!!)) || it.isChildIncomeStatement(user, IncomeStatementId(id)) }) throw Forbidden()
                    }
                else -> throw Forbidden()
            }
            is AuthenticatedUser.Employee -> {
                if (!hasPermissionUsingAllRoles(user, action, permittedRoleActions::incomeStatementActions)) throw Forbidden()
            }
            else -> throw Forbidden()
        }
    }

    fun getPermittedPlacementActions(
        user: AuthenticatedUser,
        ids: Collection<PlacementId>
    ): Map<PlacementId, Set<Action.Placement>> = this.placement.getPermittedActions(user, ids)

    fun getPermittedUnitActions(
        user: AuthenticatedUser,
        ids: Collection<DaycareId>
    ): Map<DaycareId, Set<Action.Unit>> = this.unit.getPermittedActions(user, ids)

    fun requirePermissionFor(user: AuthenticatedUser, action: Action.VasuDocumentFollowup, id: VasuDocumentFollowupEntryId) {
        if (action != Action.VasuDocumentFollowup.UPDATE) {
            throw Forbidden()
        }
        val mapping = permittedRoleActions::vasuDocumentFollowupActions

        val globalRoles = when (user) {
            is AuthenticatedUser.Employee -> user.globalRoles
            else -> user.roles
        }
        if (globalRoles.any { it == UserRole.ADMIN }) {
            return
        }

        val roles = @Suppress("DEPRECATION") acl.getRolesForVasuDocument(user, id.first).roles
        if (roles.any { mapping(it).contains(action) }) {
            return
        }

        Database(jdbi).connect { db ->
            db.read { tx ->
                val entry = tx.getVasuFollowupEntry(id)
                if (entry.authorId != user.id) {
                    throw Forbidden()
                }
            }
        }
    }

    fun getPermittedVasuFollowupActions(user: AuthenticatedUser, id: VasuDocumentId): Map<UUID, Set<Action.VasuDocumentFollowup>> {
        val action = Action.VasuDocumentFollowup.UPDATE
        val mapping = permittedRoleActions::vasuDocumentFollowupActions

        val globalRoles = when (user) {
            is AuthenticatedUser.Employee -> user.globalRoles
            else -> user.roles
        }

        @Suppress("DEPRECATION")
        val roles = acl.getRolesForVasuDocument(user, id).roles
        val hasPermissionThroughRole = globalRoles.any { it == UserRole.ADMIN } ||
            roles.any { mapping(it).contains(action) }

        val entries = Database(jdbi).connect { db ->
            db.read { tx ->
                tx.getVasuFollowupEntries(id)
            }
        }

        return entries.associate { entry ->
            entry.id to if (hasPermissionThroughRole || entry.authorId == user.id) {
                setOf(Action.VasuDocumentFollowup.UPDATE)
            } else {
                setOf()
            }
        }
    }

    private inline fun <reified A, reified I> ActionConfig<A>.getPermittedActions(
        user: AuthenticatedUser,
        ids: Collection<I>
    ): Map<I, Set<A>> where A : Action.LegacyScopedAction<I>, A : Enum<A> {
        val globalActions = enumValues<A>().asSequence()
            .filter { action -> hasPermissionThroughGlobalRole(user, action) }.toEnumSet()

        val result = ids.associateTo(linkedMapOf()) { (it to enumSetOf(*globalActions.toTypedArray())) }
        if (user is AuthenticatedUser.Employee) {
            val scopedActions = EnumSet.allOf(A::class.java).also { it -= globalActions }
            if (scopedActions.isNotEmpty()) {
                Database(jdbi).connect { db ->
                    db.read { tx ->
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
        }
        return result
    }

    private inline fun <reified A, reified I> ActionConfig<A>.hasPermissionThroughGlobalRole(
        user: AuthenticatedUser,
        action: A
    ): Boolean where A : Action.LegacyScopedAction<I>, A : Enum<A> {
        val globalRoles = when (user) {
            is AuthenticatedUser.Employee -> user.globalRoles
            else -> user.roles
        }
        return globalRoles.any { it == UserRole.ADMIN || mapping(it).contains(action) }
    }

    private inline fun <reified A, reified I> ActionConfig<A>.hasPermission(
        user: AuthenticatedUser,
        action: A,
        vararg ids: I
    ): Boolean where A : Action.LegacyScopedAction<I>, A : Enum<A> {
        if (hasPermissionThroughGlobalRole(user, action)) return true
        return Database(jdbi).connect { db ->
            db.read { tx ->
                val idToRoles = getRolesForAll(tx, user, *ids)
                ids.all { id -> (idToRoles[id] ?: emptySet()).any { mapping(it).contains(action) } }
            }
        }
    }

    private inline fun <reified A : Action.LegacyScopedAction<I>, reified I> ActionConfig<A>.getRolesForAll(
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
        is AuthenticatedUser.MobileDevice -> tx.createQuery("${this.query} AND ${this.idExpression} = ANY(:ids)")
            .bind("userId", user.id).bind("ids", ids)
            .reduceRows(ids.associateTo(linkedMapOf()) { it to enumSetOf<UserRole>() }) { acc, row ->
                acc[row.mapColumn("id")]!! += row.mapColumn<UserRole>("role")
                acc
            }
        else -> ids.associate { (it to enumSetOf(*user.roles.toTypedArray())) }
    }

    private inline fun <reified A> assertPermission(
        user: AuthenticatedUser,
        getAclRoles: () -> Set<UserRole>,
        action: A,
        crossinline mapping: (role: UserRole) -> Set<A>
    ) where A : Action, A : Enum<A> {
        val globalRoles = when (user) {
            is AuthenticatedUser.Employee -> user.globalRoles
            else -> user.roles
        }
        if (globalRoles.any { it == UserRole.ADMIN || mapping(it).contains(action) })
            return

        if (UserRole.SCOPED_ROLES.any { mapping(it).contains(action) }) {
            // the outer if-clause avoids unnecessary db query if no scoped role gives permission anyway
            if (getAclRoles().any { mapping(it).contains(action) }) {
                return
            }
        }

        throw Forbidden()
    }

    private inline fun <reified A> hasPermissionThroughRoles(
        roles: Set<UserRole>,
        action: A,
        crossinline mapping: (role: UserRole) -> Set<A>
    ): Boolean where A : Action, A : Enum<A> =
        roles.any { it == UserRole.ADMIN || mapping(it).contains(action) }

    private inline fun <reified A> hasPermissionUsingGlobalRoles(
        user: AuthenticatedUser,
        action: A,
        crossinline mapping: (role: UserRole) -> Set<A>
    ): Boolean where A : Action, A : Enum<A> =
        if (user is AuthenticatedUser.Employee) hasPermissionThroughRoles(user.globalRoles, action, mapping)
        else false

    private inline fun <reified A> hasPermissionUsingAllRoles(
        user: AuthenticatedUser,
        action: A,
        crossinline mapping: (role: UserRole) -> Set<A>
    ): Boolean where A : Action, A : Enum<A> =
        if (user is AuthenticatedUser.Employee) hasPermissionThroughRoles(
            user.globalRoles + user.allScopedRoles,
            action,
            mapping
        )
        else if (user is AuthenticatedUser.Citizen) hasPermissionThroughRoles(user.roles, action, mapping)
        else false

    enum class PinError {
        PIN_LOCKED,
        WRONG_PIN
    }

    fun verifyPinCode(
        employeeId: EmployeeId,
        pinCode: String
    ) {
        val errorCode = Database(jdbi).connect {
            it.transaction { tx ->
                if (tx.employeePinIsCorrect(employeeId, pinCode)) {
                    null
                } else {
                    val locked = tx.updateEmployeePinFailureCountAndCheckIfLocked(employeeId)
                    if (locked) PinError.PIN_LOCKED else PinError.WRONG_PIN
                }
            }
        }

        // throw must be outside transaction to not rollback failure count increase
        if (errorCode != null) throw Forbidden("Invalid pin code", errorCode.name)
    }
}

private data class ActionConfig<A>(
    @Language("sql") val query: String,
    val idExpression: String,
    val mapping: (role: UserRole) -> Set<A>
)
