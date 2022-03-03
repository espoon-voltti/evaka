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
import fi.espoo.evaka.shared.auth.UserRole.ADMIN
import fi.espoo.evaka.shared.auth.UserRole.CITIZEN_WEAK
import fi.espoo.evaka.shared.auth.UserRole.DIRECTOR
import fi.espoo.evaka.shared.auth.UserRole.END_USER
import fi.espoo.evaka.shared.auth.UserRole.FINANCE_ADMIN
import fi.espoo.evaka.shared.auth.UserRole.GROUP_STAFF
import fi.espoo.evaka.shared.auth.UserRole.MOBILE
import fi.espoo.evaka.shared.auth.UserRole.REPORT_VIEWER
import fi.espoo.evaka.shared.auth.UserRole.SERVICE_WORKER
import fi.espoo.evaka.shared.auth.UserRole.SPECIAL_EDUCATION_TEACHER
import fi.espoo.evaka.shared.auth.UserRole.STAFF
import fi.espoo.evaka.shared.auth.UserRole.UNIT_SUPERVISOR
import java.util.EnumSet

@ExcludeCodeGen
sealed interface Action {
    sealed interface ScopedAction<I> : Action

    /*
    * Global actions are top/root level actions that cannot be tied to any specific scope.
    * If a global action is permitted to a scoped role then user has to have that role somewhere,
    * e.g. be a unit supervisor in some unit.
    * */
    enum class Global(private val roles: EnumSet<UserRole>) : Action {
        CREATE_VASU_TEMPLATE(),
        READ_VASU_TEMPLATE(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF),

        FETCH_INCOME_STATEMENTS_AWAITING_HANDLER(FINANCE_ADMIN),

        CREATE_PAPER_APPLICATION(SERVICE_WORKER),
        SEARCH_APPLICATION_WITH_ASSISTANCE_NEED(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        SEARCH_APPLICATION_WITHOUT_ASSISTANCE_NEED(SERVICE_WORKER, UNIT_SUPERVISOR),
        READ_PERSON_APPLICATION(SERVICE_WORKER), // Applications summary on person page
        READ_SERVICE_WORKER_APPLICATION_NOTES(SERVICE_WORKER),
        WRITE_SERVICE_WORKER_APPLICATION_NOTES(SERVICE_WORKER),

        SEARCH_PEOPLE(FINANCE_ADMIN, SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),

        READ_FEE_THRESHOLDS(FINANCE_ADMIN),
        CREATE_FEE_THRESHOLDS(FINANCE_ADMIN),

        SEARCH_FEE_DECISIONS(FINANCE_ADMIN),
        GENERATE_FEE_DECISIONS(FINANCE_ADMIN),

        SEARCH_VOUCHER_VALUE_DECISIONS(FINANCE_ADMIN),

        READ_FINANCE_DECISION_HANDLERS(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),

        TRIGGER_SCHEDULED_JOBS,

        READ_PERSONAL_MOBILE_DEVICES(UNIT_SUPERVISOR),
        CREATE_PERSONAL_MOBILE_DEVICE_PAIRING(UNIT_SUPERVISOR),

        SEARCH_INVOICES(FINANCE_ADMIN),
        CREATE_DRAFT_INVOICES(FINANCE_ADMIN),
        CREATE_DRAFT_INVOICES_DEBUG_DIFF,

        READ_ASSISTANCE_ACTION_OPTIONS(DIRECTOR, REPORT_VIEWER, FINANCE_ADMIN, SERVICE_WORKER, UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE),
        READ_ASSISTANCE_BASIS_OPTIONS(DIRECTOR, REPORT_VIEWER, FINANCE_ADMIN, SERVICE_WORKER, UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE),
        READ_SERVICE_NEED_OPTIONS(DIRECTOR, REPORT_VIEWER, FINANCE_ADMIN, SERVICE_WORKER, UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE),
        READ_USER_MESSAGE_ACCOUNTS(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),

        READ_UNITS(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        CREATE_UNIT(),

        READ_DECISION_UNITS(SERVICE_WORKER, UNIT_SUPERVISOR),

        READ_APPLICATIONS_REPORT(SERVICE_WORKER, DIRECTOR, REPORT_VIEWER, UNIT_SUPERVISOR),
        READ_ASSISTANCE_NEEDS_AND_ACTIONS_REPORT(SERVICE_WORKER, DIRECTOR, REPORT_VIEWER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        READ_CHILD_AGE_AND_LANGUAGE_REPORT(SERVICE_WORKER, DIRECTOR, REPORT_VIEWER, SPECIAL_EDUCATION_TEACHER),
        READ_CHILD_IN_DIFFERENT_ADDRESS_REPORT(SERVICE_WORKER, FINANCE_ADMIN),
        READ_DECISIONS_REPORT(SERVICE_WORKER, DIRECTOR, REPORT_VIEWER),
        READ_DUPLICATE_PEOPLE_REPORT(),
        READ_ENDED_PLACEMENTS_REPORT(SERVICE_WORKER, FINANCE_ADMIN),
        READ_FAMILY_CONFLICT_REPORT(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR),
        READ_INVOICE_REPORT(FINANCE_ADMIN),
        READ_MISSING_HEAD_OF_FAMILY_REPORT(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR),
        READ_MISSING_SERVICE_NEED_REPORT(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR),
        READ_OCCUPANCY_REPORT(SERVICE_WORKER, DIRECTOR, REPORT_VIEWER, UNIT_SUPERVISOR),
        READ_PARTNERS_IN_DIFFERENT_ADDRESS_REPORT(SERVICE_WORKER, FINANCE_ADMIN),
        READ_PLACEMENT_SKETCHING_REPORT(SERVICE_WORKER),
        READ_PRESENCE_REPORT(DIRECTOR, REPORT_VIEWER),
        READ_RAW_REPORT(REPORT_VIEWER),
        READ_SERVICE_NEED_REPORT(SERVICE_WORKER, DIRECTOR, REPORT_VIEWER, UNIT_SUPERVISOR),
        READ_SERVICE_VOUCHER_REPORT(DIRECTOR, REPORT_VIEWER, FINANCE_ADMIN, UNIT_SUPERVISOR),
        READ_STARTING_PLACEMENTS_REPORT(SERVICE_WORKER, FINANCE_ADMIN),
        READ_SEXTET_REPORT(DIRECTOR, REPORT_VIEWER),
        READ_VARDA_REPORT(),

        UPDATE_SETTINGS(ADMIN),

        READ_INCOME_TYPES(FINANCE_ADMIN),
        READ_INVOICE_CODES(FINANCE_ADMIN),

        READ_UNIT_FEATURES(),
        READ_OWN_CHILDREN(END_USER),

        CREATE_HOLIDAY_PERIOD,
        READ_HOLIDAY_PERIOD,
        READ_HOLIDAY_PERIODS(CITIZEN_WEAK, END_USER),
        DELETE_HOLIDAY_PERIOD,
        UPDATE_HOLIDAY_PERIOD,
        CREATE_HOLIDAY_ABSENCE(END_USER),
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }

    enum class Application(private val roles: EnumSet<UserRole>) : ScopedAction<ApplicationId> {
        READ_WITH_ASSISTANCE_NEED(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        READ_WITHOUT_ASSISTANCE_NEED(SERVICE_WORKER, UNIT_SUPERVISOR),
        UPDATE(SERVICE_WORKER),

        SEND(SERVICE_WORKER),
        CANCEL(SERVICE_WORKER),

        MOVE_TO_WAITING_PLACEMENT(SERVICE_WORKER),
        RETURN_TO_SENT(SERVICE_WORKER),
        VERIFY(SERVICE_WORKER),

        READ_PLACEMENT_PLAN_DRAFT(SERVICE_WORKER),
        CREATE_PLACEMENT_PLAN(SERVICE_WORKER),
        CANCEL_PLACEMENT_PLAN(SERVICE_WORKER),

        READ_DECISION_DRAFT(SERVICE_WORKER),
        UPDATE_DECISION_DRAFT(SERVICE_WORKER),
        SEND_DECISIONS_WITHOUT_PROPOSAL(SERVICE_WORKER),
        SEND_PLACEMENT_PROPOSAL(SERVICE_WORKER),
        WITHDRAW_PLACEMENT_PROPOSAL(SERVICE_WORKER),
        RESPOND_TO_PLACEMENT_PROPOSAL(SERVICE_WORKER, UNIT_SUPERVISOR),

        CONFIRM_DECISIONS_MAILED(SERVICE_WORKER),
        ACCEPT_DECISION(SERVICE_WORKER, UNIT_SUPERVISOR),
        REJECT_DECISION(SERVICE_WORKER, UNIT_SUPERVISOR),

        READ_NOTES(SERVICE_WORKER),
        CREATE_NOTE(SERVICE_WORKER),

        UPLOAD_ATTACHMENT(SERVICE_WORKER)
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class ApplicationNote(private val roles: EnumSet<UserRole>) : ScopedAction<ApplicationNoteId> {
        UPDATE(SERVICE_WORKER),
        DELETE(SERVICE_WORKER)
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class AssistanceAction(private val roles: EnumSet<UserRole>) : ScopedAction<AssistanceActionId> {
        UPDATE(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        DELETE(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        READ_PRE_PRESCHOOL_ASSISTANCE_ACTION(SPECIAL_EDUCATION_TEACHER);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class AssistanceNeed(private val roles: EnumSet<UserRole>) : ScopedAction<AssistanceNeedId> {
        UPDATE(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        DELETE(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        READ_PRE_PRESCHOOL_ASSISTANCE_NEED(SPECIAL_EDUCATION_TEACHER);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Attachment(private val roles: EnumSet<UserRole>) : ScopedAction<AttachmentId> {
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
    enum class BackupCare(private val roles: EnumSet<UserRole>) : ScopedAction<BackupCareId> {
        UPDATE(SERVICE_WORKER, UNIT_SUPERVISOR),
        DELETE(SERVICE_WORKER, UNIT_SUPERVISOR);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class BackupPickup(private val roles: EnumSet<UserRole>) : ScopedAction<BackupPickupId> {
        UPDATE(UNIT_SUPERVISOR, STAFF),
        DELETE(UNIT_SUPERVISOR, STAFF);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Child(private val roles: EnumSet<UserRole>) : ScopedAction<ChildId> {
        READ(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN, STAFF, SPECIAL_EDUCATION_TEACHER),

        CREATE_ABSENCE(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        READ_ABSENCES(UNIT_SUPERVISOR, FINANCE_ADMIN),
        READ_FUTURE_ABSENCES(UNIT_SUPERVISOR, FINANCE_ADMIN, MOBILE),
        DELETE_ABSENCE(UNIT_SUPERVISOR, STAFF),

        READ_ADDITIONAL_INFO(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN, STAFF, SPECIAL_EDUCATION_TEACHER),
        UPDATE_ADDITIONAL_INFO(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN, STAFF, SPECIAL_EDUCATION_TEACHER),

        READ_APPLICATION(SERVICE_WORKER),

        CREATE_ASSISTANCE_ACTION(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        READ_ASSISTANCE_ACTION(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),

        CREATE_ASSISTANCE_NEED(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),
        READ_ASSISTANCE_NEED(SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER),

        CREATE_ATTENDANCE_RESERVATION(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF),

        CREATE_BACKUP_CARE(SERVICE_WORKER, UNIT_SUPERVISOR),
        READ_BACKUP_CARE(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN, STAFF, SPECIAL_EDUCATION_TEACHER),

        CREATE_BACKUP_PICKUP(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        READ_BACKUP_PICKUP(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),

        READ_NOTES(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE),
        CREATE_DAILY_NOTE(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE),
        CREATE_STICKY_NOTE(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE),

        READ_DAILY_SERVICE_TIMES(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF),
        UPDATE_DAILY_SERVICE_TIMES(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        DELETE_DAILY_SERVICE_TIMES(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),

        READ_PLACEMENT(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF),
        TERMINATE_PLACEMENT,

        READ_FAMILY_CONTACTS(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        UPDATE_FAMILY_CONTACT_DETAILS(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        UPDATE_FAMILY_CONTACT_PRIORITY(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),

        READ_GUARDIANS(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),

        CREATE_FEE_ALTERATION(FINANCE_ADMIN),
        READ_FEE_ALTERATIONS(FINANCE_ADMIN),

        READ_CHILD_RECIPIENTS(FINANCE_ADMIN, SERVICE_WORKER, UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF),
        UPDATE_CHILD_RECIPIENT(SERVICE_WORKER, UNIT_SUPERVISOR),

        CREATE_VASU_DOCUMENT(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, GROUP_STAFF),
        READ_VASU_DOCUMENT(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, GROUP_STAFF),

        CREATE_PEDAGOGICAL_DOCUMENT(UNIT_SUPERVISOR, STAFF, GROUP_STAFF, SPECIAL_EDUCATION_TEACHER),
        READ_PEDAGOGICAL_DOCUMENTS(UNIT_SUPERVISOR, STAFF, GROUP_STAFF, SPECIAL_EDUCATION_TEACHER),

        READ_SENSITIVE_INFO(UNIT_SUPERVISOR, STAFF, GROUP_STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE),

        UPLOAD_IMAGE(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF, MOBILE),
        DELETE_IMAGE(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF, MOBILE);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class ChildDailyNote(private val roles: EnumSet<UserRole>) : ScopedAction<ChildDailyNoteId> {
        UPDATE(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE),
        DELETE(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class ChildImage(private val roles: EnumSet<UserRole>) : ScopedAction<ChildImageId> {
        DOWNLOAD(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF, MOBILE);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class ChildStickyNote(private val roles: EnumSet<UserRole>) : ScopedAction<ChildStickyNoteId> {
        UPDATE(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE),
        DELETE(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Decision(private val roles: EnumSet<UserRole>) : ScopedAction<DecisionId> {
        DOWNLOAD_PDF(SERVICE_WORKER, UNIT_SUPERVISOR)
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class FeeAlteration(private val roles: EnumSet<UserRole>) : ScopedAction<FeeAlterationId> {
        UPDATE(FINANCE_ADMIN),
        DELETE(FINANCE_ADMIN)
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class FeeDecision(private val roles: EnumSet<UserRole>) : ScopedAction<FeeDecisionId> {
        READ(FINANCE_ADMIN),
        UPDATE(FINANCE_ADMIN)
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class FeeThresholds(private val roles: EnumSet<UserRole>) : ScopedAction<FeeThresholdsId> {
        UPDATE(FINANCE_ADMIN)
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Group(private val roles: EnumSet<UserRole>) : ScopedAction<GroupId> {
        UPDATE(UNIT_SUPERVISOR),
        DELETE(SERVICE_WORKER, UNIT_SUPERVISOR),

        CREATE_ABSENCES(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        READ_ABSENCES(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        DELETE_ABSENCES(UNIT_SUPERVISOR, STAFF),

        READ_STAFF_ATTENDANCES(FINANCE_ADMIN, UNIT_SUPERVISOR, STAFF, MOBILE, SPECIAL_EDUCATION_TEACHER),
        UPDATE_STAFF_ATTENDANCES(UNIT_SUPERVISOR, STAFF, MOBILE),

        READ_CARETAKERS(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        CREATE_CARETAKERS(UNIT_SUPERVISOR),
        UPDATE_CARETAKERS(UNIT_SUPERVISOR),
        DELETE_CARETAKERS(UNIT_SUPERVISOR),

        CREATE_NOTE(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE),
        READ_NOTES(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE)
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class GroupNote(private val roles: EnumSet<UserRole>) : ScopedAction<GroupNoteId> {
        UPDATE(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE),
        DELETE(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER, MOBILE);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class GroupPlacement(private val roles: EnumSet<UserRole>) : ScopedAction<GroupPlacementId> {
        UPDATE(UNIT_SUPERVISOR),
        DELETE(UNIT_SUPERVISOR);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Income(private val roles: EnumSet<UserRole>) : ScopedAction<IncomeId> {
        UPDATE(FINANCE_ADMIN),
        DELETE(FINANCE_ADMIN);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class IncomeStatement(private val roles: EnumSet<UserRole>) : ScopedAction<IncomeStatementId> {
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
    enum class Invoice(private val roles: EnumSet<UserRole>) : ScopedAction<InvoiceId> {
        READ(FINANCE_ADMIN),
        UPDATE(FINANCE_ADMIN),
        SEND(FINANCE_ADMIN),
        DELETE(FINANCE_ADMIN),
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class MessageContent(private val roles: EnumSet<UserRole>) : ScopedAction<MessageContentId> {
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class MessageDraft(private val roles: EnumSet<UserRole>) : ScopedAction<MessageDraftId> {
        UPLOAD_ATTACHMENT,
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class MobileDevice(private val roles: EnumSet<UserRole>) : ScopedAction<MobileDeviceId> {
        UPDATE_NAME(UNIT_SUPERVISOR),
        DELETE(UNIT_SUPERVISOR)
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Pairing(private val roles: EnumSet<UserRole>) : ScopedAction<PairingId> {
        POST_RESPONSE(UNIT_SUPERVISOR)
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Parentship(private val roles: EnumSet<UserRole>) : ScopedAction<ParentshipId> {
        DELETE(FINANCE_ADMIN),
        DELETE_CONFLICTED_PARENTSHIP(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),
        READ(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),
        RETRY(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),
        UPDATE(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Partnership(private val roles: EnumSet<UserRole>) : ScopedAction<PartnershipId> {
        DELETE(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),
        READ(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),
        RETRY(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),
        UPDATE(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class PedagogicalDocument(private val roles: EnumSet<UserRole>) : ScopedAction<PedagogicalDocumentId> {
        CREATE_ATTACHMENT(UNIT_SUPERVISOR, STAFF, GROUP_STAFF, SPECIAL_EDUCATION_TEACHER),
        DELETE(UNIT_SUPERVISOR, STAFF, GROUP_STAFF, SPECIAL_EDUCATION_TEACHER),
        READ(UNIT_SUPERVISOR, STAFF, GROUP_STAFF, SPECIAL_EDUCATION_TEACHER),
        UPDATE(UNIT_SUPERVISOR, STAFF, GROUP_STAFF, SPECIAL_EDUCATION_TEACHER);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Person(private val roles: EnumSet<UserRole>) : ScopedAction<PersonId> {
        ADD_SSN(SERVICE_WORKER, FINANCE_ADMIN),
        CREATE_INCOME(FINANCE_ADMIN),
        CREATE_PARENTSHIP(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),
        CREATE_PARTNERSHIP(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN),
        DISABLE_SSN(),
        GENERATE_RETROACTIVE_FEE_DECISIONS(FINANCE_ADMIN),
        GENERATE_RETROACTIVE_VOUCHER_VALUE_DECISIONS(FINANCE_ADMIN),
        READ_CHILD_PLACEMENT_PERIODS(FINANCE_ADMIN),
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
    enum class Placement(private val roles: EnumSet<UserRole>) : ScopedAction<PlacementId> {
        UPDATE(SERVICE_WORKER, UNIT_SUPERVISOR),
        DELETE(SERVICE_WORKER, UNIT_SUPERVISOR),

        CREATE_GROUP_PLACEMENT(UNIT_SUPERVISOR),

        CREATE_SERVICE_NEED(UNIT_SUPERVISOR),

        READ_TERMINATIONS,
        TERMINATE();

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class ServiceNeed(private val roles: EnumSet<UserRole>) : ScopedAction<ServiceNeedId> {
        UPDATE(UNIT_SUPERVISOR),
        DELETE(UNIT_SUPERVISOR);

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }
    enum class Unit(private val roles: EnumSet<UserRole>) : ScopedAction<DaycareId> {
        READ(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        READ_BASIC(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
        READ_DETAILED(SERVICE_WORKER, UNIT_SUPERVISOR),
        READ_CAPACITY_STATS(SERVICE_WORKER, FINANCE_ADMIN, UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER),
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
    enum class VasuDocument(private val roles: EnumSet<UserRole>) : ScopedAction<VasuDocumentId> {
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

    enum class VasuDocumentFollowup(private val roles: EnumSet<UserRole>) : Action {
        UPDATE(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, GROUP_STAFF),
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }

    enum class VasuTemplate(private val roles: EnumSet<UserRole>) : ScopedAction<VasuTemplateId> {
        READ(),
        COPY(),
        UPDATE(),
        DELETE();

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }

    enum class VoucherValueDecision(private val roles: EnumSet<UserRole>) : ScopedAction<VoucherValueDecisionId> {
        READ(FINANCE_ADMIN),
        UPDATE(FINANCE_ADMIN)
        ;

        constructor(vararg roles: UserRole) : this(roles.toEnumSet())
        override fun toString(): String = "${javaClass.name}.$name"
        override fun defaultRoles(): Set<UserRole> = roles
    }

    /**
     * Roles allowed to perform this action by default.
     *
     * Can be empty if permission checks for this action are not based on roles.
     */
    fun defaultRoles(): Set<UserRole>
}

private fun Array<out UserRole>.toEnumSet() = EnumSet.noneOf(UserRole::class.java).also {
    it.addAll(this)
}
