// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.ExcludeCodeGen
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ApplicationNoteId
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.BackupPickupId
import fi.espoo.evaka.shared.ChildDailyNoteId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.ChildStickyNoteId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.FeeAlterationId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.FeeThresholdsId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupNoteId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.MessageContentId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.VasuTemplateId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.UserRole.DIRECTOR
import fi.espoo.evaka.shared.auth.UserRole.FINANCE_ADMIN
import fi.espoo.evaka.shared.auth.UserRole.GROUP_STAFF
import fi.espoo.evaka.shared.auth.UserRole.MOBILE
import fi.espoo.evaka.shared.auth.UserRole.REPORT_VIEWER
import fi.espoo.evaka.shared.auth.UserRole.SERVICE_WORKER
import fi.espoo.evaka.shared.auth.UserRole.SPECIAL_EDUCATION_TEACHER
import fi.espoo.evaka.shared.auth.UserRole.STAFF
import fi.espoo.evaka.shared.auth.UserRole.UNIT_SUPERVISOR
import fi.espoo.evaka.shared.security.actionrule.HasGlobalRole
import fi.espoo.evaka.shared.security.actionrule.HasRoleInAnyUnit
import fi.espoo.evaka.shared.security.actionrule.HasRoleInChildPlacementGroup
import fi.espoo.evaka.shared.security.actionrule.HasRoleInChildPlacementUnit
import fi.espoo.evaka.shared.security.actionrule.HasRoleInGroupUnit
import fi.espoo.evaka.shared.security.actionrule.IsChildGuardian
import fi.espoo.evaka.shared.security.actionrule.IsCitizen
import fi.espoo.evaka.shared.security.actionrule.IsCitizensOwn
import fi.espoo.evaka.shared.security.actionrule.IsMobile
import fi.espoo.evaka.shared.security.actionrule.IsMobileInChildPlacementUnit
import fi.espoo.evaka.shared.security.actionrule.IsMobileInGroupUnit
import fi.espoo.evaka.shared.security.actionrule.ScopedActionRule
import fi.espoo.evaka.shared.security.actionrule.StaticActionRule
import fi.espoo.evaka.shared.utils.toEnumSet
import java.util.EnumSet

@ExcludeCodeGen
sealed interface Action {
    sealed interface StaticAction : Action {
        val defaultRules: Array<out StaticActionRule>
    }
    sealed interface ScopedAction<T> : Action {
        val defaultRules: Array<out ScopedActionRule<in T>>
    }

    sealed interface LegacyAction : Action {
        /**
         * Roles allowed to perform this action by default.
         *
         * Can be empty if permission checks for this action are not based on roles.
         */
        fun defaultRoles(): Set<UserRole>
    }
    sealed interface LegacyScopedAction<I> : LegacyAction

    enum class Global(override vararg val defaultRules: StaticActionRule) : StaticAction {
        CREATE_VASU_TEMPLATE,
        READ_VASU_TEMPLATE(HasRoleInAnyUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF)),

        FETCH_INCOME_STATEMENTS_AWAITING_HANDLER(HasGlobalRole(FINANCE_ADMIN)),

        CREATE_PAPER_APPLICATION(HasGlobalRole(SERVICE_WORKER)),
        READ_PERSON_APPLICATION(HasGlobalRole(SERVICE_WORKER)), // Applications summary on person page
        READ_SERVICE_WORKER_APPLICATION_NOTES(HasGlobalRole(SERVICE_WORKER)),
        WRITE_SERVICE_WORKER_APPLICATION_NOTES(HasGlobalRole(SERVICE_WORKER)),

        SEARCH_PEOPLE(HasGlobalRole(FINANCE_ADMIN, SERVICE_WORKER), HasRoleInAnyUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)),

        READ_FEE_THRESHOLDS(HasGlobalRole(FINANCE_ADMIN)),
        CREATE_FEE_THRESHOLDS(HasGlobalRole(FINANCE_ADMIN)),

        SEARCH_FEE_DECISIONS(HasGlobalRole(FINANCE_ADMIN)),
        GENERATE_FEE_DECISIONS(HasGlobalRole(FINANCE_ADMIN)),

        SEARCH_VOUCHER_VALUE_DECISIONS(HasGlobalRole(FINANCE_ADMIN)),

        READ_FINANCE_DECISION_HANDLERS(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasRoleInAnyUnit(UNIT_SUPERVISOR)),

        TRIGGER_SCHEDULED_JOBS,

        READ_PERSONAL_MOBILE_DEVICES(HasRoleInAnyUnit(UNIT_SUPERVISOR)),
        CREATE_PERSONAL_MOBILE_DEVICE_PAIRING(HasRoleInAnyUnit(UNIT_SUPERVISOR)),

        SEARCH_INVOICES(HasGlobalRole(FINANCE_ADMIN)),
        CREATE_DRAFT_INVOICES(HasGlobalRole(FINANCE_ADMIN)),

        READ_ASSISTANCE_ACTION_OPTIONS(HasGlobalRole(DIRECTOR, REPORT_VIEWER, FINANCE_ADMIN, SERVICE_WORKER), HasRoleInAnyUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER), IsMobile(requirePinLogin = false)),
        READ_ASSISTANCE_BASIS_OPTIONS(HasGlobalRole(DIRECTOR, REPORT_VIEWER, FINANCE_ADMIN, SERVICE_WORKER), HasRoleInAnyUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER), IsMobile(requirePinLogin = false)),
        READ_SERVICE_NEED_OPTIONS(HasGlobalRole(DIRECTOR, REPORT_VIEWER, FINANCE_ADMIN, SERVICE_WORKER), HasRoleInAnyUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER), IsMobile(requirePinLogin = false)),
        READ_USER_MESSAGE_ACCOUNTS(HasRoleInAnyUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER)),

        READ_UNITS(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasRoleInAnyUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER)),
        CREATE_UNIT,

        READ_DECISION_UNITS(HasGlobalRole(SERVICE_WORKER), HasRoleInAnyUnit(UNIT_SUPERVISOR)),

        READ_APPLICATIONS_REPORT(HasGlobalRole(SERVICE_WORKER, DIRECTOR, REPORT_VIEWER), HasRoleInAnyUnit(UNIT_SUPERVISOR)),
        READ_ASSISTANCE_NEEDS_AND_ACTIONS_REPORT(HasGlobalRole(SERVICE_WORKER, DIRECTOR, REPORT_VIEWER), HasRoleInAnyUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)),
        READ_CHILD_AGE_AND_LANGUAGE_REPORT(HasGlobalRole(SERVICE_WORKER, DIRECTOR, REPORT_VIEWER), HasRoleInAnyUnit(SPECIAL_EDUCATION_TEACHER)),
        READ_CHILD_IN_DIFFERENT_ADDRESS_REPORT(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN)),
        READ_DECISIONS_REPORT(HasGlobalRole(SERVICE_WORKER, DIRECTOR, REPORT_VIEWER)),
        READ_DUPLICATE_PEOPLE_REPORT,
        READ_ENDED_PLACEMENTS_REPORT(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN)),
        READ_FAMILY_CONFLICT_REPORT(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasRoleInAnyUnit(UNIT_SUPERVISOR)),
        READ_INVOICE_REPORT(HasGlobalRole(FINANCE_ADMIN)),
        READ_MISSING_HEAD_OF_FAMILY_REPORT(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasRoleInAnyUnit(UNIT_SUPERVISOR)),
        READ_MISSING_SERVICE_NEED_REPORT(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasRoleInAnyUnit(UNIT_SUPERVISOR)),
        READ_OCCUPANCY_REPORT(HasGlobalRole(SERVICE_WORKER, DIRECTOR, REPORT_VIEWER), HasRoleInAnyUnit(UNIT_SUPERVISOR)),
        READ_PARTNERS_IN_DIFFERENT_ADDRESS_REPORT(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN)),
        READ_PLACEMENT_SKETCHING_REPORT(HasGlobalRole(SERVICE_WORKER)),
        READ_PRESENCE_REPORT(HasGlobalRole(DIRECTOR, REPORT_VIEWER)),
        READ_RAW_REPORT(HasGlobalRole(REPORT_VIEWER)),
        READ_SERVICE_NEED_REPORT(HasGlobalRole(SERVICE_WORKER, DIRECTOR, REPORT_VIEWER), HasRoleInAnyUnit(UNIT_SUPERVISOR)),
        READ_SERVICE_VOUCHER_REPORT(HasGlobalRole(DIRECTOR, REPORT_VIEWER, FINANCE_ADMIN), HasRoleInAnyUnit(UNIT_SUPERVISOR)),
        READ_STARTING_PLACEMENTS_REPORT(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN)),
        READ_SEXTET_REPORT(HasGlobalRole(DIRECTOR, REPORT_VIEWER)),
        READ_VARDA_REPORT,

        UPDATE_SETTINGS,

        READ_INCOME_TYPES(HasGlobalRole(FINANCE_ADMIN)),
        READ_INVOICE_CODES(HasGlobalRole(FINANCE_ADMIN)),

        READ_UNIT_FEATURES,
        READ_OWN_CHILDREN(IsCitizen(allowWeakLogin = false)),

        CREATE_HOLIDAY_PERIOD,
        READ_HOLIDAY_PERIOD,
        READ_HOLIDAY_PERIODS(IsCitizen(allowWeakLogin = true)),
        DELETE_HOLIDAY_PERIOD,
        UPDATE_HOLIDAY_PERIOD,
        CREATE_HOLIDAY_ABSENCE(IsCitizen(allowWeakLogin = false));

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class Application(override vararg val defaultRules: ScopedActionRule<in ApplicationId>) : ScopedAction<ApplicationId> {
        READ_WITH_ASSISTANCE_NEED(HasGlobalRole(SERVICE_WORKER), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).application),
        READ_WITHOUT_ASSISTANCE_NEED(HasGlobalRole(SERVICE_WORKER), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR).application),
        UPDATE(HasGlobalRole(SERVICE_WORKER)),

        SEND(HasGlobalRole(SERVICE_WORKER)),
        CANCEL(HasGlobalRole(SERVICE_WORKER)),

        MOVE_TO_WAITING_PLACEMENT(HasGlobalRole(SERVICE_WORKER)),
        RETURN_TO_SENT(HasGlobalRole(SERVICE_WORKER)),
        VERIFY(HasGlobalRole(SERVICE_WORKER)),

        READ_PLACEMENT_PLAN_DRAFT(HasGlobalRole(SERVICE_WORKER)),
        CREATE_PLACEMENT_PLAN(HasGlobalRole(SERVICE_WORKER)),
        CANCEL_PLACEMENT_PLAN(HasGlobalRole(SERVICE_WORKER)),

        READ_DECISION_DRAFT(HasGlobalRole(SERVICE_WORKER)),
        UPDATE_DECISION_DRAFT(HasGlobalRole(SERVICE_WORKER)),
        SEND_DECISIONS_WITHOUT_PROPOSAL(HasGlobalRole(SERVICE_WORKER)),
        SEND_PLACEMENT_PROPOSAL(HasGlobalRole(SERVICE_WORKER)),
        WITHDRAW_PLACEMENT_PROPOSAL(HasGlobalRole(SERVICE_WORKER)),
        RESPOND_TO_PLACEMENT_PROPOSAL(HasGlobalRole(SERVICE_WORKER), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR).application),

        CONFIRM_DECISIONS_MAILED(HasGlobalRole(SERVICE_WORKER)),
        ACCEPT_DECISION(HasGlobalRole(SERVICE_WORKER), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR).application),
        REJECT_DECISION(HasGlobalRole(SERVICE_WORKER), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR).application),

        READ_NOTES(HasGlobalRole(SERVICE_WORKER)),
        CREATE_NOTE(HasGlobalRole(SERVICE_WORKER)),

        UPLOAD_ATTACHMENT(HasGlobalRole(SERVICE_WORKER), IsCitizensOwn(allowWeakLogin = false).application);

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class ApplicationNote(override vararg val defaultRules: ScopedActionRule<in ApplicationNoteId>) : ScopedAction<ApplicationNoteId> {
        UPDATE(HasGlobalRole(SERVICE_WORKER)),
        DELETE(HasGlobalRole(SERVICE_WORKER));

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class AssistanceAction(override vararg val defaultRules: ScopedActionRule<in AssistanceActionId>) : ScopedAction<AssistanceActionId> {
        UPDATE(HasGlobalRole(SERVICE_WORKER), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).assistanceAction),
        DELETE(HasGlobalRole(SERVICE_WORKER), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).assistanceAction),
        READ_PRE_PRESCHOOL_ASSISTANCE_ACTION(HasRoleInChildPlacementUnit(SPECIAL_EDUCATION_TEACHER).assistanceAction);

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class AssistanceNeed(override vararg val defaultRules: ScopedActionRule<in AssistanceNeedId>) : ScopedAction<AssistanceNeedId> {
        UPDATE(HasGlobalRole(SERVICE_WORKER), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).assistanceNeed),
        DELETE(HasGlobalRole(SERVICE_WORKER), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).assistanceNeed),
        READ_PRE_PRESCHOOL_ASSISTANCE_NEED(HasRoleInChildPlacementUnit(SPECIAL_EDUCATION_TEACHER).assistanceNeed);

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class Attachment(private val roles: EnumSet<UserRole>) : LegacyScopedAction<AttachmentId> {
        READ_APPLICATION_ATTACHMENT(SERVICE_WORKER, UNIT_SUPERVISOR),
        READ_INCOME_STATEMENT_ATTACHMENT(FINANCE_ADMIN),
        READ_MESSAGE_CONTENT_ATTACHMENT,
        READ_MESSAGE_DRAFT_ATTACHMENT,
        READ_PEDAGOGICAL_DOCUMENT_ATTACHMENT(UNIT_SUPERVISOR, STAFF, GROUP_STAFF, SPECIAL_EDUCATION_TEACHER),
        DELETE_APPLICATION_ATTACHMENT(SERVICE_WORKER),
        DELETE_INCOME_STATEMENT_ATTACHMENT(FINANCE_ADMIN),
        DELETE_MESSAGE_CONTENT_ATTACHMENT,
        DELETE_MESSAGE_DRAFT_ATTACHMENT,
        DELETE_PEDAGOGICAL_DOCUMENT_ATTACHMENT(UNIT_SUPERVISOR, STAFF, GROUP_STAFF, SPECIAL_EDUCATION_TEACHER)
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class BackupCare(override vararg val defaultRules: ScopedActionRule<in BackupCareId>) : ScopedAction<BackupCareId> {
        UPDATE(HasGlobalRole(SERVICE_WORKER), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR).backupCare),
        DELETE(HasGlobalRole(SERVICE_WORKER), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR).backupCare);

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class BackupPickup(override vararg val defaultRules: ScopedActionRule<in BackupPickupId>) : ScopedAction<BackupPickupId> {
        UPDATE(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF).backupPickup),
        DELETE(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF).backupPickup);

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class Child(override vararg val defaultRules: ScopedActionRule<in ChildId>) : ScopedAction<ChildId> {
        READ(
            HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN),
            HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).child,
            IsChildGuardian(allowWeakLogin = false).child
        ),

        CREATE_ABSENCE(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).child),
        READ_ABSENCES(HasGlobalRole(FINANCE_ADMIN), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR).child),
        READ_FUTURE_ABSENCES(HasGlobalRole(FINANCE_ADMIN), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR).child, IsMobileInChildPlacementUnit(requirePinLogin = false).child),
        DELETE_ABSENCE(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF).child),
        DELETE_ABSENCE_RANGE(IsMobileInChildPlacementUnit(requirePinLogin = false).child),

        READ_ADDITIONAL_INFO(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).child),
        UPDATE_ADDITIONAL_INFO(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).child),

        READ_APPLICATION(HasGlobalRole(SERVICE_WORKER)),

        CREATE_ASSISTANCE_ACTION(HasGlobalRole(SERVICE_WORKER), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).child),
        READ_ASSISTANCE_ACTION(HasGlobalRole(SERVICE_WORKER), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).child),

        CREATE_ASSISTANCE_NEED(HasGlobalRole(SERVICE_WORKER), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).child),
        READ_ASSISTANCE_NEED(HasGlobalRole(SERVICE_WORKER), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).child),

        CREATE_ATTENDANCE_RESERVATION(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).child),

        CREATE_BACKUP_CARE(HasGlobalRole(SERVICE_WORKER), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR).child),
        READ_BACKUP_CARE(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).child),

        CREATE_BACKUP_PICKUP(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).child),
        READ_BACKUP_PICKUP(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).child),

        READ_NOTES(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).child, IsMobileInChildPlacementUnit(requirePinLogin = false).child),
        CREATE_DAILY_NOTE(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).child, IsMobileInChildPlacementUnit(requirePinLogin = false).child),
        CREATE_STICKY_NOTE(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).child, IsMobileInChildPlacementUnit(requirePinLogin = false).child),

        READ_DAILY_SERVICE_TIMES(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).child),
        UPDATE_DAILY_SERVICE_TIMES(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).child),
        DELETE_DAILY_SERVICE_TIMES(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).child),

        READ_PLACEMENT(
            HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN),
            HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).child,
            IsChildGuardian(allowWeakLogin = false).child
        ),

        READ_FAMILY_CONTACTS(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).child),
        UPDATE_FAMILY_CONTACT_DETAILS(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).child),
        UPDATE_FAMILY_CONTACT_PRIORITY(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).child),

        READ_GUARDIANS(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR).child),

        CREATE_FEE_ALTERATION(HasGlobalRole(FINANCE_ADMIN)),
        READ_FEE_ALTERATIONS(HasGlobalRole(FINANCE_ADMIN)),

        READ_CHILD_RECIPIENTS(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).child),
        UPDATE_CHILD_RECIPIENT(HasGlobalRole(SERVICE_WORKER), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR).child),

        CREATE_VASU_DOCUMENT(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).child, HasRoleInChildPlacementGroup(GROUP_STAFF).child),
        READ_VASU_DOCUMENT(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).child, HasRoleInChildPlacementGroup(GROUP_STAFF).child),

        CREATE_PEDAGOGICAL_DOCUMENT(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).child, HasRoleInChildPlacementGroup(GROUP_STAFF).child),
        READ_PEDAGOGICAL_DOCUMENTS(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).child, HasRoleInChildPlacementGroup(GROUP_STAFF).child),

        READ_SENSITIVE_INFO(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).child, IsMobileInChildPlacementUnit(requirePinLogin = true).child, HasRoleInChildPlacementGroup(GROUP_STAFF).child),

        UPLOAD_IMAGE(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).child, IsMobileInChildPlacementUnit(requirePinLogin = false).child),
        DELETE_IMAGE(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).child, IsMobileInChildPlacementUnit(requirePinLogin = false).child);

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class ChildDailyNote(private val roles: EnumSet<UserRole>) : LegacyScopedAction<ChildDailyNoteId> {
        UPDATE(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE),
        DELETE(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class ChildImage(override vararg val defaultRules: ScopedActionRule<ChildImageId>) : ScopedAction<ChildImageId> {
        DOWNLOAD(
            HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).childImage,
            IsMobileInChildPlacementUnit(requirePinLogin = false).childImage,
            IsChildGuardian(allowWeakLogin = false).childImage
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class ChildStickyNote(private val roles: EnumSet<UserRole>) : LegacyScopedAction<ChildStickyNoteId> {
        UPDATE(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE),
        DELETE(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Decision(private val roles: EnumSet<UserRole>) : LegacyScopedAction<DecisionId> {
        DOWNLOAD_PDF(SERVICE_WORKER, UNIT_SUPERVISOR)
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class FeeAlteration(private val roles: EnumSet<UserRole>) : LegacyScopedAction<FeeAlterationId> {
        UPDATE(FINANCE_ADMIN),
        DELETE(FINANCE_ADMIN)
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class FeeDecision(private val roles: EnumSet<UserRole>) : LegacyScopedAction<FeeDecisionId> {
        READ(FINANCE_ADMIN),
        UPDATE(FINANCE_ADMIN)
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class FeeThresholds(private val roles: EnumSet<UserRole>) : LegacyScopedAction<FeeThresholdsId> {
        UPDATE(FINANCE_ADMIN)
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Group(override vararg val defaultRules: ScopedActionRule<in GroupId>) : ScopedAction<GroupId> {
        UPDATE(HasRoleInGroupUnit(UNIT_SUPERVISOR).group),
        DELETE(HasGlobalRole(SERVICE_WORKER), HasRoleInGroupUnit(UNIT_SUPERVISOR).group),

        CREATE_ABSENCES(HasRoleInGroupUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).group),
        READ_ABSENCES(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasRoleInGroupUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).group),
        DELETE_ABSENCES(HasRoleInGroupUnit(UNIT_SUPERVISOR, STAFF).group),

        READ_STAFF_ATTENDANCES(HasGlobalRole(FINANCE_ADMIN), HasRoleInGroupUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).group, IsMobileInGroupUnit(requirePinLogin = false).group),
        UPDATE_STAFF_ATTENDANCES(HasRoleInGroupUnit(UNIT_SUPERVISOR, STAFF).group, IsMobileInGroupUnit(requirePinLogin = false).group),

        READ_CARETAKERS(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasRoleInGroupUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).group),
        CREATE_CARETAKERS(HasRoleInGroupUnit(UNIT_SUPERVISOR).group),
        UPDATE_CARETAKERS(HasRoleInGroupUnit(UNIT_SUPERVISOR).group),
        DELETE_CARETAKERS(HasRoleInGroupUnit(UNIT_SUPERVISOR).group),

        CREATE_NOTE(HasRoleInGroupUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).group, IsMobileInGroupUnit(requirePinLogin = false).group),
        READ_NOTES(HasRoleInGroupUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).group, IsMobileInGroupUnit(requirePinLogin = false).group),

        MARK_DEPARTURE(IsMobileInGroupUnit(requirePinLogin = false).group),
        MARK_EXTERNAL_DEPARTURE(IsMobileInGroupUnit(requirePinLogin = false).group),
        MARK_ARRIVAL(IsMobileInGroupUnit(requirePinLogin = false).group),
        MARK_EXTERNAL_ARRIVAL(IsMobileInGroupUnit(requirePinLogin = false).group);

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class GroupNote(private val roles: EnumSet<UserRole>) : LegacyScopedAction<GroupNoteId> {
        UPDATE(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE),
        DELETE(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class GroupPlacement(private val roles: EnumSet<UserRole>) : LegacyScopedAction<GroupPlacementId> {
        UPDATE(UNIT_SUPERVISOR),
        DELETE(UNIT_SUPERVISOR);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Income(private val roles: EnumSet<UserRole>) : LegacyScopedAction<IncomeId> {
        UPDATE(FINANCE_ADMIN),
        DELETE(FINANCE_ADMIN);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class IncomeStatement(private val roles: EnumSet<UserRole>) : LegacyScopedAction<IncomeStatementId> {
        UPDATE_HANDLED(FINANCE_ADMIN),
        UPLOAD_ATTACHMENT(FINANCE_ADMIN),
        READ,
        READ_CHILDS,
        READ_ALL_OWN,
        READ_ALL_CHILDS,
        READ_START_DATES,
        READ_CHILDS_START_DATES,
        CREATE,
        CREATE_FOR_CHILD,
        UPDATE,
        UPDATE_FOR_CHILD,
        REMOVE,
        REMOVE_FOR_CHILD,
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Invoice(override vararg val defaultRules: ScopedActionRule<in InvoiceId>) : ScopedAction<InvoiceId> {
        READ(HasGlobalRole(FINANCE_ADMIN)),
        UPDATE(HasGlobalRole(FINANCE_ADMIN)),
        SEND(HasGlobalRole(FINANCE_ADMIN)),
        DELETE(HasGlobalRole(FINANCE_ADMIN)),
        ;

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class MessageContent(private val roles: EnumSet<UserRole>) : LegacyScopedAction<MessageContentId> {
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class MessageDraft(private val roles: EnumSet<UserRole>) : LegacyScopedAction<MessageDraftId> {
        UPLOAD_ATTACHMENT,
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class MobileDevice(private val roles: EnumSet<UserRole>) : LegacyScopedAction<MobileDeviceId> {
        UPDATE_NAME(UNIT_SUPERVISOR),
        DELETE(UNIT_SUPERVISOR)
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Pairing(private val roles: EnumSet<UserRole>) : LegacyScopedAction<PairingId> {
        POST_RESPONSE(UNIT_SUPERVISOR)
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Parentship(private val roles: EnumSet<UserRole>) : LegacyScopedAction<ParentshipId> {
        DELETE(FINANCE_ADMIN),
        DELETE_CONFLICTED_PARENTSHIP(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),
        READ(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),
        RETRY(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),
        UPDATE(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Partnership(private val roles: EnumSet<UserRole>) : LegacyScopedAction<PartnershipId> {
        DELETE(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),
        READ(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),
        RETRY(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),
        UPDATE(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class PedagogicalDocument(override vararg val defaultRules: ScopedActionRule<in PedagogicalDocumentId>) : ScopedAction<PedagogicalDocumentId> {
        CREATE_ATTACHMENT(
            HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).pedagogicalDocument,
            HasRoleInChildPlacementGroup(GROUP_STAFF).pedagogicalDocument
        ),
        DELETE(
            HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).pedagogicalDocument,
            HasRoleInChildPlacementGroup(GROUP_STAFF).pedagogicalDocument
        ),
        READ(
            HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).pedagogicalDocument,
            HasRoleInChildPlacementGroup(GROUP_STAFF).pedagogicalDocument,
            IsChildGuardian(allowWeakLogin = false).pedagogicalDocument
        ),
        UPDATE(
            HasRoleInChildPlacementUnit(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).pedagogicalDocument,
            HasRoleInChildPlacementGroup(GROUP_STAFF).pedagogicalDocument
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class Person(private val roles: EnumSet<UserRole>) : LegacyScopedAction<PersonId> {
        ADD_SSN(SERVICE_WORKER, FINANCE_ADMIN),
        CREATE_INCOME(FINANCE_ADMIN),
        CREATE_PARENTSHIP(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),
        CREATE_PARTNERSHIP(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),
        DISABLE_SSN(),
        GENERATE_RETROACTIVE_FEE_DECISIONS(FINANCE_ADMIN),
        GENERATE_RETROACTIVE_VOUCHER_VALUE_DECISIONS(FINANCE_ADMIN),
        READ_FAMILY_OVERVIEW(FINANCE_ADMIN, UNIT_SUPERVISOR),
        READ_FEE_DECISIONS(FINANCE_ADMIN),
        READ_INCOME(FINANCE_ADMIN),
        READ_INCOME_STATEMENTS(FINANCE_ADMIN),
        READ_INVOICES(FINANCE_ADMIN),
        READ_INVOICE_ADDRESS(FINANCE_ADMIN),
        READ_OPH_OID(DIRECTOR, UNIT_SUPERVISOR),
        READ_PARENTSHIPS(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),
        READ_PARTNERSHIPS(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),
        READ(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        READ_VOUCHER_VALUE_DECISIONS(FINANCE_ADMIN),
        UPDATE(FINANCE_ADMIN, SERVICE_WORKER, UNIT_SUPERVISOR),
        UPDATE_INVOICE_ADDRESS(FINANCE_ADMIN),
        UPDATE_OPH_OID;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Placement(override vararg val defaultRules: ScopedActionRule<in PlacementId>) : ScopedAction<PlacementId> {
        UPDATE(HasGlobalRole(SERVICE_WORKER), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR).placement),
        DELETE(HasGlobalRole(SERVICE_WORKER), HasRoleInChildPlacementUnit(UNIT_SUPERVISOR).placement),

        CREATE_GROUP_PLACEMENT(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR).placement),

        CREATE_SERVICE_NEED(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR).placement),

        TERMINATE(IsChildGuardian(allowWeakLogin = false).placement);

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class ServiceNeed(override vararg val defaultRules: ScopedActionRule<in ServiceNeedId>) : ScopedAction<ServiceNeedId> {
        UPDATE(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR).serviceNeed),
        DELETE(HasRoleInChildPlacementUnit(UNIT_SUPERVISOR).serviceNeed);

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class Unit(private val roles: EnumSet<UserRole>) : LegacyScopedAction<DaycareId> {
        READ(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        READ_BASIC(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        READ_DETAILED(SERVICE_WORKER, UNIT_SUPERVISOR),
        READ_GROUPS(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        READ_CHILD_CAPACITY_FACTORS(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        UPDATE,

        READ_STAFF_ATTENDANCES(MOBILE),

        READ_OCCUPANCIES(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, MOBILE),

        READ_ATTENDANCE_RESERVATIONS(UNIT_SUPERVISOR, STAFF),

        READ_BACKUP_CARE(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN, STAFF),

        CREATE_PLACEMENT(SERVICE_WORKER, UNIT_SUPERVISOR),
        READ_PLACEMENT(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF),

        READ_PLACEMENT_PLAN(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR),

        ACCEPT_PLACEMENT_PROPOSAL(SERVICE_WORKER, UNIT_SUPERVISOR),

        CREATE_GROUP(UNIT_SUPERVISOR),

        READ_MOBILE_STATS(MOBILE),
        READ_MOBILE_INFO(MOBILE),

        READ_MOBILE_DEVICES(UNIT_SUPERVISOR),
        CREATE_MOBILE_DEVICE_PAIRING(UNIT_SUPERVISOR),

        READ_ACL(UNIT_SUPERVISOR),
        INSERT_ACL_UNIT_SUPERVISOR(),
        DELETE_ACL_UNIT_SUPERVISOR(),
        INSERT_ACL_SPECIAL_EDUCATION_TEACHER(),
        DELETE_ACL_SPECIAL_EDUCATION_TEACHER(),
        INSERT_ACL_STAFF(UNIT_SUPERVISOR),
        DELETE_ACL_STAFF(UNIT_SUPERVISOR),
        UPDATE_STAFF_GROUP_ACL(UNIT_SUPERVISOR),

        READ_FAMILY_CONTACT_REPORT(UNIT_SUPERVISOR),
        READ_SERVICE_VOUCHER_VALUES_REPORT(FINANCE_ADMIN, UNIT_SUPERVISOR),

        UPDATE_FEATURES(),

        READ_TERMINATED_PLACEMENTS(UNIT_SUPERVISOR),
        READ_MISSING_GROUP_PLACEMENTS(SERVICE_WORKER, UNIT_SUPERVISOR);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class VasuDocument(private val roles: EnumSet<UserRole>) : LegacyScopedAction<VasuDocumentId> {
        READ(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, GROUP_STAFF),
        UPDATE(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, GROUP_STAFF),
        EVENT_PUBLISHED(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, GROUP_STAFF),
        EVENT_MOVED_TO_READY(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, GROUP_STAFF),
        EVENT_RETURNED_TO_READY(UNIT_SUPERVISOR),
        EVENT_MOVED_TO_REVIEWED(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, GROUP_STAFF),
        EVENT_RETURNED_TO_REVIEWED(),
        EVENT_MOVED_TO_CLOSED(),
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }

    enum class VasuDocumentFollowup(private val roles: EnumSet<UserRole>) : LegacyAction {
        UPDATE(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, GROUP_STAFF),
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }

    enum class VasuTemplate(private val roles: EnumSet<UserRole>) : LegacyScopedAction<VasuTemplateId> {
        READ(),
        COPY(),
        UPDATE(),
        DELETE();

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }

    enum class VoucherValueDecision(private val roles: EnumSet<UserRole>) : LegacyScopedAction<VoucherValueDecisionId> {
        READ(FINANCE_ADMIN),
        UPDATE(FINANCE_ADMIN)
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
}
