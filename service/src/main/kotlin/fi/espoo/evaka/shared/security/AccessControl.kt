// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.attachment.citizenHasPermissionThroughPedagogicalDocument
import fi.espoo.evaka.attachment.isOwnAttachment
import fi.espoo.evaka.attachment.wasUploadedByAnyEmployee
import fi.espoo.evaka.incomestatement.isChildIncomeStatement
import fi.espoo.evaka.incomestatement.isOwnIncomeStatement
import fi.espoo.evaka.messaging.hasPermissionForAttachmentThroughMessageContent
import fi.espoo.evaka.messaging.hasPermissionForAttachmentThroughMessageDraft
import fi.espoo.evaka.pis.employeePinIsCorrect
import fi.espoo.evaka.pis.service.getGuardianChildIds
import fi.espoo.evaka.pis.updateEmployeePinFailureCountAndCheckIfLocked
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.PersonId
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

    fun <T> requirePermissionFor(user: AuthenticatedUser, action: Action.ScopedAction<T>, target: T) =
        requirePermissionFor(user, action, listOf(target))
    fun <T> requirePermissionFor(user: AuthenticatedUser, action: Action.ScopedAction<T>, targets: Iterable<T>) = Database(jdbi).connect { dbc ->
        checkPermissionFor(dbc, user, action, targets).values.forEach { it.assert() }
    }

    fun <T> hasPermissionFor(user: AuthenticatedUser, action: Action.ScopedAction<T>, target: T): Boolean =
        hasPermissionFor(user, action, listOf(target))
    fun <T> hasPermissionFor(user: AuthenticatedUser, action: Action.ScopedAction<T>, targets: Iterable<T>): Boolean = Database(jdbi).connect { dbc ->
        checkPermissionFor(dbc, user, action, targets).values.all { it.isPermitted() }
    }

    fun <T> checkPermissionFor(
        dbc: Database.Connection,
        user: AuthenticatedUser,
        action: Action.ScopedAction<T>,
        target: T
    ): AccessControlDecision = checkPermissionFor(dbc, user, action, listOf(target)).values.first()
    fun <T> checkPermissionFor(
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
        target: T
    ) where A : Action.ScopedAction<T>, A : Enum<A> = getPermittedActions(tx, user, A::class.java, listOf(target)).values.first()
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
            is Action.Attachment -> ids.all { id -> hasPermissionForInternal(user, action, id as AttachmentId) }
            is Action.Parentship -> this.parentship.hasPermission(user, action, *ids as Array<ParentshipId>)
            is Action.Partnership -> this.partnership.hasPermission(user, action, *ids as Array<PartnershipId>)
            is Action.Person -> ids.all { id -> hasPermissionForInternal(user, action, id as PersonId) }
            else -> error("Unsupported action type")
        }.exhaust()

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

    fun getPermittedPersonActions(
        user: AuthenticatedUser,
        ids: Collection<PersonId>
    ): Map<PersonId, Set<Action.Person>> = ids.associateWith { personId ->
        Action.Person.values()
            .filter { action -> hasPermissionForInternal(user, action, personId) }
            .toSet()
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
