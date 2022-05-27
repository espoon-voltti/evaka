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
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.FeeAlterationId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.FeeThresholdsId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupNoteId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.InvoiceCorrectionId
import fi.espoo.evaka.shared.InvoiceId
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
import fi.espoo.evaka.shared.VasuTemplateId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.auth.UserRole.DIRECTOR
import fi.espoo.evaka.shared.auth.UserRole.FINANCE_ADMIN
import fi.espoo.evaka.shared.auth.UserRole.REPORT_VIEWER
import fi.espoo.evaka.shared.auth.UserRole.SERVICE_WORKER
import fi.espoo.evaka.shared.auth.UserRole.SPECIAL_EDUCATION_TEACHER
import fi.espoo.evaka.shared.auth.UserRole.STAFF
import fi.espoo.evaka.shared.auth.UserRole.UNIT_SUPERVISOR
import fi.espoo.evaka.shared.security.actionrule.HasGlobalRole
import fi.espoo.evaka.shared.security.actionrule.HasGroupRole
import fi.espoo.evaka.shared.security.actionrule.HasUnitRole
import fi.espoo.evaka.shared.security.actionrule.IsCitizen
import fi.espoo.evaka.shared.security.actionrule.IsEmployee
import fi.espoo.evaka.shared.security.actionrule.IsMobile
import fi.espoo.evaka.shared.security.actionrule.ScopedActionRule
import fi.espoo.evaka.shared.security.actionrule.UnscopedActionRule

@ExcludeCodeGen
sealed interface Action {
    sealed interface UnscopedAction : Action {
        val defaultRules: Array<out UnscopedActionRule>
    }
    sealed interface ScopedAction<T> : Action {
        val defaultRules: Array<out ScopedActionRule<in T>>
    }

    enum class Global(override vararg val defaultRules: UnscopedActionRule) : UnscopedAction {
        CREATE_VASU_TEMPLATE,
        READ_VASU_TEMPLATE(HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inAnyUnit()),

        FETCH_INCOME_STATEMENTS_AWAITING_HANDLER(HasGlobalRole(FINANCE_ADMIN)),

        CREATE_PAPER_APPLICATION(HasGlobalRole(SERVICE_WORKER)),
        READ_PERSON_APPLICATION(HasGlobalRole(SERVICE_WORKER)), // Applications summary on person page
        READ_SERVICE_WORKER_APPLICATION_NOTES(HasGlobalRole(SERVICE_WORKER)),
        WRITE_SERVICE_WORKER_APPLICATION_NOTES(HasGlobalRole(SERVICE_WORKER)),

        CREATE_PERSON(HasGlobalRole(FINANCE_ADMIN, SERVICE_WORKER)),
        CREATE_PERSON_FROM_VTJ(HasGlobalRole(FINANCE_ADMIN, SERVICE_WORKER)),
        SEARCH_PEOPLE(HasGlobalRole(FINANCE_ADMIN, SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inAnyUnit()),
        SEARCH_PEOPLE_UNRESTRICTED(HasGlobalRole(FINANCE_ADMIN, SERVICE_WORKER)),

        READ_FEE_THRESHOLDS(HasGlobalRole(FINANCE_ADMIN)),
        CREATE_FEE_THRESHOLDS(HasGlobalRole(FINANCE_ADMIN)),

        SEARCH_FEE_DECISIONS(HasGlobalRole(FINANCE_ADMIN)),
        GENERATE_FEE_DECISIONS(HasGlobalRole(FINANCE_ADMIN)),

        SEARCH_VOUCHER_VALUE_DECISIONS(HasGlobalRole(FINANCE_ADMIN)),

        READ_FINANCE_DECISION_HANDLERS(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inAnyUnit()),

        TRIGGER_SCHEDULED_JOBS,

        READ_PERSONAL_MOBILE_DEVICES(HasUnitRole(UNIT_SUPERVISOR).inAnyUnit()),
        CREATE_PERSONAL_MOBILE_DEVICE_PAIRING(HasUnitRole(UNIT_SUPERVISOR).inAnyUnit()),

        SEARCH_INVOICES(HasGlobalRole(FINANCE_ADMIN)),
        CREATE_DRAFT_INVOICES(HasGlobalRole(FINANCE_ADMIN)),

        READ_ASSISTANCE_ACTION_OPTIONS(HasGlobalRole(DIRECTOR, REPORT_VIEWER, FINANCE_ADMIN, SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inAnyUnit(), IsMobile(requirePinLogin = false).any()),
        READ_ASSISTANCE_BASIS_OPTIONS(HasGlobalRole(DIRECTOR, REPORT_VIEWER, FINANCE_ADMIN, SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inAnyUnit(), IsMobile(requirePinLogin = false).any()),
        READ_SERVICE_NEED_OPTIONS(HasGlobalRole(DIRECTOR, REPORT_VIEWER, FINANCE_ADMIN, SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inAnyUnit(), IsMobile(requirePinLogin = false).any()),
        READ_USER_MESSAGE_ACCOUNTS(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inAnyUnit()),

        READ_UNITS(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inAnyUnit()),
        CREATE_UNIT,

        READ_DECISION_UNITS(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inAnyUnit()),

        READ_APPLICATIONS_REPORT(HasGlobalRole(SERVICE_WORKER, DIRECTOR, REPORT_VIEWER), HasUnitRole(UNIT_SUPERVISOR).inAnyUnit()),
        READ_ASSISTANCE_NEEDS_AND_ACTIONS_REPORT(HasGlobalRole(SERVICE_WORKER, DIRECTOR, REPORT_VIEWER), HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inAnyUnit()),
        READ_CHILD_AGE_AND_LANGUAGE_REPORT(HasGlobalRole(SERVICE_WORKER, DIRECTOR, REPORT_VIEWER), HasUnitRole(SPECIAL_EDUCATION_TEACHER).inAnyUnit()),
        READ_CHILD_IN_DIFFERENT_ADDRESS_REPORT(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN)),
        READ_DECISIONS_REPORT(HasGlobalRole(SERVICE_WORKER, DIRECTOR, REPORT_VIEWER)),
        READ_DUPLICATE_PEOPLE_REPORT,
        READ_ENDED_PLACEMENTS_REPORT(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN)),
        READ_FAMILY_CONFLICT_REPORT(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inAnyUnit()),
        READ_INVOICE_REPORT(HasGlobalRole(FINANCE_ADMIN)),
        READ_MISSING_HEAD_OF_FAMILY_REPORT(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inAnyUnit()),
        READ_MISSING_SERVICE_NEED_REPORT(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inAnyUnit()),
        READ_OCCUPANCY_REPORT(HasGlobalRole(SERVICE_WORKER, DIRECTOR, REPORT_VIEWER), HasUnitRole(UNIT_SUPERVISOR).inAnyUnit()),
        READ_PARTNERS_IN_DIFFERENT_ADDRESS_REPORT(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN)),
        READ_PLACEMENT_SKETCHING_REPORT(HasGlobalRole(SERVICE_WORKER)),
        READ_PRESENCE_REPORT(HasGlobalRole(DIRECTOR, REPORT_VIEWER)),
        READ_RAW_REPORT(HasGlobalRole(REPORT_VIEWER)),
        READ_SERVICE_NEED_REPORT(HasGlobalRole(SERVICE_WORKER, DIRECTOR, REPORT_VIEWER), HasUnitRole(UNIT_SUPERVISOR).inAnyUnit()),
        READ_SERVICE_VOUCHER_REPORT(HasGlobalRole(DIRECTOR, REPORT_VIEWER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inAnyUnit()),
        READ_STARTING_PLACEMENTS_REPORT(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN)),
        READ_SEXTET_REPORT(HasGlobalRole(DIRECTOR, REPORT_VIEWER)),
        READ_VARDA_REPORT,

        UPDATE_SETTINGS,

        READ_INCOME_TYPES(HasGlobalRole(FINANCE_ADMIN)),
        READ_INVOICE_CODES(HasGlobalRole(FINANCE_ADMIN)),

        READ_UNIT_FEATURES,

        CREATE_HOLIDAY_PERIOD,
        READ_HOLIDAY_PERIOD,
        READ_HOLIDAY_PERIODS(IsCitizen(allowWeakLogin = true).any()),
        DELETE_HOLIDAY_PERIOD,
        UPDATE_HOLIDAY_PERIOD,

        READ_HOLIDAY_QUESTIONNAIRE,
        READ_HOLIDAY_QUESTIONNAIRES,
        READ_ACTIVE_HOLIDAY_QUESTIONNAIRES(IsCitizen(allowWeakLogin = true).any()),
        CREATE_HOLIDAY_QUESTIONNAIRE,
        DELETE_HOLIDAY_QUESTIONNAIRE,
        UPDATE_HOLIDAY_QUESTIONNAIRE,
        SEND_PATU_REPORT,

        ACCESS_MESSAGING(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inAnyUnit(), IsMobile(requirePinLogin = true).any()),

        CREATE_EMPLOYEE,
        READ_EMPLOYEES(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inAnyUnit()),
        SEARCH_EMPLOYEES;

        override fun toString(): String = "${javaClass.name}.$name"
    }

    sealed interface Citizen : Action {
        enum class Application(override vararg val defaultRules: ScopedActionRule<in ApplicationId>) : ScopedAction<ApplicationId> {
            READ(IsCitizen(allowWeakLogin = false).ownerOfApplication()),
            READ_DECISIONS(IsCitizen(allowWeakLogin = false).ownerOfApplication()),
            DELETE(IsCitizen(allowWeakLogin = false).ownerOfApplication()),
            UPDATE(IsCitizen(allowWeakLogin = false).ownerOfApplication()),
            UPLOAD_ATTACHMENT(IsCitizen(allowWeakLogin = false).ownerOfApplication());

            override fun toString(): String = "${javaClass.name}.$name"
        }
        enum class Child(override vararg val defaultRules: ScopedActionRule<in ChildId>) : ScopedAction<ChildId> {
            READ(IsCitizen(allowWeakLogin = false).guardianOfChild()),
            READ_PLACEMENT_STATUS_BY_APPLICATION_TYPE(IsCitizen(allowWeakLogin = false).guardianOfChild()),
            READ_DUPLICATE_APPLICATIONS(IsCitizen(allowWeakLogin = false).guardianOfChild()),
            CREATE_ABSENCE(IsCitizen(allowWeakLogin = true).guardianOfChild()),

            CREATE_HOLIDAY_ABSENCE(IsCitizen(allowWeakLogin = true).guardianOfChild()),
            CREATE_RESERVATION(IsCitizen(allowWeakLogin = true).guardianOfChild()),

            READ_PLACEMENT(IsCitizen(allowWeakLogin = false).guardianOfChild()),

            CREATE_INCOME_STATEMENT(IsCitizen(allowWeakLogin = false).guardianOfChild()),
            READ_INCOME_STATEMENTS(IsCitizen(allowWeakLogin = false).guardianOfChild()),

            CREATE_APPLICATION(IsCitizen(allowWeakLogin = false).guardianOfChild());

            override fun toString(): String = "${javaClass.name}.$name"
        }

        enum class Decision(override vararg val defaultRules: ScopedActionRule<in DecisionId>) : ScopedAction<DecisionId> {
            DOWNLOAD_PDF(IsCitizen(allowWeakLogin = false).ownerOfApplicationOfSentDecision());
        }
        enum class IncomeStatement(override vararg val defaultRules: ScopedActionRule<in IncomeStatementId>) : ScopedAction<IncomeStatementId> {
            READ(IsCitizen(allowWeakLogin = false).ownerOfIncomeStatement(), IsCitizen(allowWeakLogin = false).guardianOfChildOfIncomeStatement()),
            UPDATE(IsCitizen(allowWeakLogin = false).ownerOfIncomeStatement(), IsCitizen(allowWeakLogin = false).guardianOfChildOfIncomeStatement()),
            DELETE(IsCitizen(allowWeakLogin = false).ownerOfIncomeStatement(), IsCitizen(allowWeakLogin = false).guardianOfChildOfIncomeStatement()),

            UPLOAD_ATTACHMENT(IsCitizen(allowWeakLogin = false).ownerOfIncomeStatement(), IsCitizen(allowWeakLogin = false).guardianOfChildOfIncomeStatement());

            override fun toString(): String = "${javaClass.name}.$name"
        }
        enum class PedagogicalDocument(override vararg val defaultRules: ScopedActionRule<in PedagogicalDocumentId>) : ScopedAction<PedagogicalDocumentId> {
            READ(IsCitizen(allowWeakLogin = false).guardianOfChildOfPedagogicalDocument());

            override fun toString(): String = "${javaClass.name}.$name"
        }
        enum class Person(override vararg val defaultRules: ScopedActionRule<in PersonId>) : ScopedAction<PersonId> {
            CREATE_INCOME_STATEMENT(IsCitizen(allowWeakLogin = false).self()),
            READ_APPLICATIONS(IsCitizen(allowWeakLogin = false).self()),
            READ_CHILDREN(IsCitizen(allowWeakLogin = false).self()),
            READ_DECISIONS(IsCitizen(allowWeakLogin = false).self()),
            READ_RESERVATIONS(IsCitizen(allowWeakLogin = true).self()),
            READ_INCOME_STATEMENTS(IsCitizen(allowWeakLogin = false).self()),
            READ_VTJ_DETAILS(IsCitizen(allowWeakLogin = true).self()),
            UPDATE_PERSONAL_DATA(IsCitizen(allowWeakLogin = false).self());

            override fun toString(): String = "${javaClass.name}.$name"
        }
        enum class Placement(override vararg val defaultRules: ScopedActionRule<in PlacementId>) : ScopedAction<PlacementId> {
            TERMINATE(IsCitizen(allowWeakLogin = false).guardianOfChildOfPlacement());

            override fun toString(): String = "${javaClass.name}.$name"
        }
    }

    enum class Application(override vararg val defaultRules: ScopedActionRule<in ApplicationId>) : ScopedAction<ApplicationId> {
        READ(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inPlacementPlanUnitOfApplication()),
        READ_IF_HAS_ASSISTANCE_NEED(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inPlacementPlanUnitOfApplication(), HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPreferredUnitOfApplication()),
        UPDATE(HasGlobalRole(SERVICE_WORKER)),

        SEND(HasGlobalRole(SERVICE_WORKER), IsCitizen(allowWeakLogin = false).ownerOfApplication()),
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
        RESPOND_TO_PLACEMENT_PROPOSAL(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inPlacementPlanUnitOfApplication()),

        READ_DECISIONS(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inAnyUnit()),
        CONFIRM_DECISIONS_MAILED(HasGlobalRole(SERVICE_WORKER)),
        ACCEPT_DECISION(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inPlacementPlanUnitOfApplication(), IsCitizen(allowWeakLogin = false).ownerOfApplication()),
        REJECT_DECISION(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inPlacementPlanUnitOfApplication(), IsCitizen(allowWeakLogin = false).ownerOfApplication()),

        READ_NOTES(HasGlobalRole(SERVICE_WORKER)),
        CREATE_NOTE(HasGlobalRole(SERVICE_WORKER), HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPreferredUnitOfApplication(), HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPlacementPlanUnitOfApplication()),
        READ_SPECIAL_EDUCATION_TEACHER_NOTES(HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPreferredUnitOfApplication(), HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPlacementPlanUnitOfApplication()),

        READ_ATTACHMENTS(HasGlobalRole(SERVICE_WORKER)),
        UPLOAD_ATTACHMENT(HasGlobalRole(SERVICE_WORKER));

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class ApplicationNote(override vararg val defaultRules: ScopedActionRule<in ApplicationNoteId>) : ScopedAction<ApplicationNoteId> {
        UPDATE(HasGlobalRole(SERVICE_WORKER), IsEmployee.authorOfApplicationNote()),
        DELETE(HasGlobalRole(SERVICE_WORKER), IsEmployee.authorOfApplicationNote());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class AssistanceAction(override vararg val defaultRules: ScopedActionRule<in AssistanceActionId>) : ScopedAction<AssistanceActionId> {
        UPDATE(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfAssistanceAction()),
        DELETE(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfAssistanceAction()),
        READ_PRE_PRESCHOOL_ASSISTANCE_ACTION(HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfAssistanceAction());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class AssistanceNeed(override vararg val defaultRules: ScopedActionRule<in AssistanceNeedId>) : ScopedAction<AssistanceNeedId> {
        UPDATE(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfAssistanceNeed()),
        DELETE(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfAssistanceNeed()),
        READ_PRE_PRESCHOOL_ASSISTANCE_NEED(HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfAssistanceNeed());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class Attachment(override vararg val defaultRules: ScopedActionRule<in AttachmentId>) : ScopedAction<AttachmentId> {
        READ_APPLICATION_ATTACHMENT(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inUnitOfApplicationAttachment(), IsCitizen(allowWeakLogin = false).uploaderOfAttachment()),
        READ_INCOME_STATEMENT_ATTACHMENT(HasGlobalRole(FINANCE_ADMIN), IsCitizen(allowWeakLogin = false).uploaderOfAttachment()),
        READ_INCOME_ATTACHMENT(HasGlobalRole(FINANCE_ADMIN)),
        READ_MESSAGE_CONTENT_ATTACHMENT(
            IsEmployee.hasPermissionForAttachmentThroughMessageContent(),
            IsCitizen(allowWeakLogin = true).hasPermissionForAttachmentThroughMessageContent(),
        ),
        READ_MESSAGE_DRAFT_ATTACHMENT(IsEmployee.hasPermissionForAttachmentThroughMessageDraft()),
        READ_PEDAGOGICAL_DOCUMENT_ATTACHMENT(
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementUnitOfChildOfPedagogicalDocumentOfAttachment(),
            IsCitizen(allowWeakLogin = false).guardianOfChildOfPedagogicalDocumentOfAttachment()
        ),
        DELETE_APPLICATION_ATTACHMENT(HasGlobalRole(SERVICE_WORKER).andAttachmentWasUploadedByAnyEmployee(), IsCitizen(allowWeakLogin = false).uploaderOfAttachment()),
        DELETE_INCOME_STATEMENT_ATTACHMENT(HasGlobalRole(FINANCE_ADMIN).andAttachmentWasUploadedByAnyEmployee(), IsCitizen(allowWeakLogin = false).uploaderOfAttachment()),
        DELETE_INCOME_ATTACHMENT(HasGlobalRole(FINANCE_ADMIN)),
        DELETE_MESSAGE_CONTENT_ATTACHMENT,
        DELETE_MESSAGE_DRAFT_ATTACHMENT(IsEmployee.hasPermissionForAttachmentThroughMessageDraft()),
        DELETE_PEDAGOGICAL_DOCUMENT_ATTACHMENT(
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementUnitOfChildOfPedagogicalDocumentOfAttachment(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class BackupCare(override vararg val defaultRules: ScopedActionRule<in BackupCareId>) : ScopedAction<BackupCareId> {
        UPDATE(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfBackupCare()),
        DELETE(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfBackupCare());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class BackupPickup(override vararg val defaultRules: ScopedActionRule<in BackupPickupId>) : ScopedAction<BackupPickupId> {
        UPDATE(HasUnitRole(UNIT_SUPERVISOR, STAFF).inPlacementUnitOfChildOfBackupPickup()),
        DELETE(HasUnitRole(UNIT_SUPERVISOR, STAFF).inPlacementUnitOfChildOfBackupPickup());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class Child(override vararg val defaultRules: ScopedActionRule<in ChildId>) : ScopedAction<ChildId> {
        READ(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild()),

        CREATE_ABSENCE(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild()),
        READ_ABSENCES(HasGlobalRole(FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChild()),
        READ_FUTURE_ABSENCES(HasGlobalRole(FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChild(), IsMobile(requirePinLogin = false).inPlacementUnitOfChild()),
        DELETE_ABSENCE(HasUnitRole(UNIT_SUPERVISOR, STAFF).inPlacementUnitOfChild()),
        DELETE_ABSENCE_RANGE(IsMobile(requirePinLogin = false).inPlacementUnitOfChild()),

        READ_ADDITIONAL_INFO(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild()),
        UPDATE_ADDITIONAL_INFO(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild()),

        READ_DECISIONS(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inAnyUnit()),

        READ_APPLICATION(HasGlobalRole(SERVICE_WORKER)),

        CREATE_ASSISTANCE_ACTION(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild()),
        READ_ASSISTANCE_ACTION(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild()),

        CREATE_ASSISTANCE_NEED(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild()),
        READ_ASSISTANCE_NEED(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild()),

        CREATE_ATTENDANCE_RESERVATION(HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inPlacementUnitOfChild()),

        CREATE_BACKUP_CARE(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChild()),
        READ_BACKUP_CARE(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild()),

        CREATE_BACKUP_PICKUP(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild()),
        READ_BACKUP_PICKUP(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild()),

        READ_NOTES(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(), IsMobile(requirePinLogin = false).inPlacementUnitOfChild()),
        CREATE_DAILY_NOTE(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(), IsMobile(requirePinLogin = false).inPlacementUnitOfChild()),
        CREATE_STICKY_NOTE(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(), IsMobile(requirePinLogin = false).inPlacementUnitOfChild()),

        READ_DAILY_SERVICE_TIMES(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inPlacementUnitOfChild()),
        UPDATE_DAILY_SERVICE_TIMES(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild()),
        DELETE_DAILY_SERVICE_TIMES(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild()),

        READ_PLACEMENT(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inPlacementUnitOfChild()),

        READ_FAMILY_CONTACTS(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild()),
        UPDATE_FAMILY_CONTACT_DETAILS(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild()),
        UPDATE_FAMILY_CONTACT_PRIORITY(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild()),

        READ_GUARDIANS(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChild()),

        CREATE_FEE_ALTERATION(HasGlobalRole(FINANCE_ADMIN)),
        READ_FEE_ALTERATIONS(HasGlobalRole(FINANCE_ADMIN)),

        READ_CHILD_RECIPIENTS(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inPlacementUnitOfChild()),
        UPDATE_CHILD_RECIPIENT(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChild()),

        CREATE_VASU_DOCUMENT(
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementUnitOfChild(),
            HasGroupRole(STAFF).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementGroupOfChild()
        ),
        READ_VASU_DOCUMENT(
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementUnitOfChild(),
            HasGroupRole(STAFF).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementGroupOfChild()
        ),

        CREATE_PEDAGOGICAL_DOCUMENT(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementUnitOfChild()),
        READ_PEDAGOGICAL_DOCUMENTS(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementUnitOfChild()),

        READ_SENSITIVE_INFO(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(), IsMobile(requirePinLogin = true).inPlacementUnitOfChild()),

        UPLOAD_IMAGE(HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inPlacementUnitOfChild(), IsMobile(requirePinLogin = false).inPlacementUnitOfChild()),
        DELETE_IMAGE(HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inPlacementUnitOfChild(), IsMobile(requirePinLogin = false).inPlacementUnitOfChild());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class ChildDailyNote(override vararg val defaultRules: ScopedActionRule<in ChildDailyNoteId>) : ScopedAction<ChildDailyNoteId> {
        UPDATE(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfChildDailyNote(), IsMobile(requirePinLogin = false).inPlacementUnitOfChildOfChildDailyNote()),
        DELETE(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfChildDailyNote(), IsMobile(requirePinLogin = false).inPlacementUnitOfChildOfChildDailyNote());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class ChildImage(override vararg val defaultRules: ScopedActionRule<ChildImageId>) : ScopedAction<ChildImageId> {
        DOWNLOAD(
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inPlacementUnitOfChildOfChildImage(),
            IsMobile(requirePinLogin = false).inPlacementUnitOfChildOfChildImage(),
            IsCitizen(allowWeakLogin = true).guardianOfChildOfChildImage()
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class ChildStickyNote(override vararg val defaultRules: ScopedActionRule<in ChildStickyNoteId>) : ScopedAction<ChildStickyNoteId> {
        UPDATE(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfChildStickyNote(), IsMobile(requirePinLogin = false).inPlacementUnitOfChildOfChildStickyNote()),
        DELETE(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfChildStickyNote(), IsMobile(requirePinLogin = false).inPlacementUnitOfChildOfChildStickyNote());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class Decision(override vararg val defaultRules: ScopedActionRule<in DecisionId>) : ScopedAction<DecisionId> {
        DOWNLOAD_PDF(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfDecision());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class Employee(override vararg val defaultRules: ScopedActionRule<in EmployeeId>) : ScopedAction<EmployeeId> {
        READ(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN, DIRECTOR, REPORT_VIEWER), IsEmployee.self(), HasUnitRole(UNIT_SUPERVISOR, STAFF).inAnyUnit()),
        READ_DETAILS,
        DELETE,
        UPDATE;
        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class FeeAlteration(override vararg val defaultRules: ScopedActionRule<in FeeAlterationId>) : ScopedAction<FeeAlterationId> {
        UPDATE(HasGlobalRole(FINANCE_ADMIN)),
        DELETE(HasGlobalRole(FINANCE_ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class FeeDecision(override vararg val defaultRules: ScopedActionRule<in FeeDecisionId>) : ScopedAction<FeeDecisionId> {
        READ(HasGlobalRole(FINANCE_ADMIN)),
        UPDATE(HasGlobalRole(FINANCE_ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class FeeThresholds(override vararg val defaultRules: ScopedActionRule<in FeeThresholdsId>) : ScopedAction<FeeThresholdsId> {
        UPDATE(HasGlobalRole(FINANCE_ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class Group(override vararg val defaultRules: ScopedActionRule<in GroupId>) : ScopedAction<GroupId> {
        UPDATE(HasUnitRole(UNIT_SUPERVISOR).inUnitOfGroup()),
        DELETE(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inUnitOfGroup()),

        CREATE_ABSENCES(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inUnitOfGroup()),
        READ_ABSENCES(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inUnitOfGroup()),
        DELETE_ABSENCES(HasUnitRole(UNIT_SUPERVISOR, STAFF).inUnitOfGroup()),

        READ_STAFF_ATTENDANCES(HasGlobalRole(FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inUnitOfGroup(), IsMobile(requirePinLogin = false).inUnitOfGroup()),
        UPDATE_STAFF_ATTENDANCES(HasUnitRole(UNIT_SUPERVISOR, STAFF).inUnitOfGroup(), IsMobile(requirePinLogin = false).inUnitOfGroup()),

        READ_CARETAKERS(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inUnitOfGroup()),
        CREATE_CARETAKERS(HasUnitRole(UNIT_SUPERVISOR).inUnitOfGroup()),
        UPDATE_CARETAKERS(HasUnitRole(UNIT_SUPERVISOR).inUnitOfGroup()),
        DELETE_CARETAKERS(HasUnitRole(UNIT_SUPERVISOR).inUnitOfGroup()),

        CREATE_NOTE(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inUnitOfGroup(), IsMobile(requirePinLogin = false).inUnitOfGroup()),
        READ_NOTES(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inUnitOfGroup(), IsMobile(requirePinLogin = false).inUnitOfGroup()),

        MARK_DEPARTURE(IsMobile(requirePinLogin = false).inUnitOfGroup()),
        MARK_EXTERNAL_DEPARTURE(IsMobile(requirePinLogin = false).inUnitOfGroup()),
        MARK_ARRIVAL(IsMobile(requirePinLogin = false).inUnitOfGroup()),
        MARK_EXTERNAL_ARRIVAL(IsMobile(requirePinLogin = false).inUnitOfGroup());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class GroupNote(override vararg val defaultRules: ScopedActionRule<in GroupNoteId>) : ScopedAction<GroupNoteId> {
        UPDATE(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inUnitOfGroupNote(), IsMobile(requirePinLogin = false).inUnitOfGroupNote()),
        DELETE(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inUnitOfGroupNote(), IsMobile(requirePinLogin = false).inUnitOfGroupNote());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class GroupPlacement(override vararg val defaultRules: ScopedActionRule<in GroupPlacementId>) : ScopedAction<GroupPlacementId> {
        UPDATE(HasUnitRole(UNIT_SUPERVISOR).inUnitOfGroupPlacement()),
        DELETE(HasUnitRole(UNIT_SUPERVISOR).inUnitOfGroupPlacement());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class Income(override vararg val defaultRules: ScopedActionRule<in IncomeId>) : ScopedAction<IncomeId> {
        UPDATE(HasGlobalRole(FINANCE_ADMIN)),
        DELETE(HasGlobalRole(FINANCE_ADMIN)),
        UPLOAD_ATTACHMENT(HasGlobalRole(FINANCE_ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class IncomeStatement(override vararg val defaultRules: ScopedActionRule<in IncomeStatementId>) : ScopedAction<IncomeStatementId> {
        UPDATE_HANDLED(HasGlobalRole(FINANCE_ADMIN)),
        UPLOAD_ATTACHMENT(HasGlobalRole(FINANCE_ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class Invoice(override vararg val defaultRules: ScopedActionRule<in InvoiceId>) : ScopedAction<InvoiceId> {
        READ(HasGlobalRole(FINANCE_ADMIN)),
        UPDATE(HasGlobalRole(FINANCE_ADMIN)),
        SEND(HasGlobalRole(FINANCE_ADMIN)),
        DELETE(HasGlobalRole(FINANCE_ADMIN)),
        ;

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class InvoiceCorrection(override vararg val defaultRules: ScopedActionRule<in InvoiceCorrectionId>) : ScopedAction<InvoiceCorrectionId> {
        DELETE(HasGlobalRole(FINANCE_ADMIN)),
        UPDATE_NOTE(HasGlobalRole(FINANCE_ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class MessageDraft(override vararg val defaultRules: ScopedActionRule<in MessageDraftId>) : ScopedAction<MessageDraftId> {
        UPLOAD_ATTACHMENT(IsEmployee.hasPermissionForMessageDraft());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class MobileDevice(override vararg val defaultRules: ScopedActionRule<in MobileDeviceId>) : ScopedAction<MobileDeviceId> {
        UPDATE_NAME(HasUnitRole(UNIT_SUPERVISOR).inUnitOfMobileDevice(), IsEmployee.ownerOfMobileDevice()),
        DELETE(HasUnitRole(UNIT_SUPERVISOR).inUnitOfMobileDevice(), IsEmployee.ownerOfMobileDevice());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class Pairing(override vararg val defaultRules: ScopedActionRule<in PairingId>) : ScopedAction<PairingId> {
        POST_RESPONSE(HasUnitRole(UNIT_SUPERVISOR).inUnitOfPairing(), IsEmployee.ownerOfPairing());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class Parentship(override vararg val defaultRules: ScopedActionRule<in ParentshipId>) : ScopedAction<ParentshipId> {
        DELETE(HasGlobalRole(FINANCE_ADMIN)),
        DELETE_CONFLICTED_PARENTSHIP(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfParentship()),
        READ(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfParentship()),
        RETRY(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfParentship()),
        UPDATE(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfParentship());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class Partnership(override vararg val defaultRules: ScopedActionRule<in PartnershipId>) : ScopedAction<PartnershipId> {
        DELETE(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPartnership()),
        READ(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPartnership()),
        RETRY(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPartnership()),
        UPDATE(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPartnership());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class PedagogicalDocument(override vararg val defaultRules: ScopedActionRule<in PedagogicalDocumentId>) : ScopedAction<PedagogicalDocumentId> {
        CREATE_ATTACHMENT(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementUnitOfChildOfPedagogicalDocument()),
        DELETE(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementUnitOfChildOfPedagogicalDocument()),
        UPDATE(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementUnitOfChildOfPedagogicalDocument());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class Person(override vararg val defaultRules: ScopedActionRule<in PersonId>) : ScopedAction<PersonId> {
        ADD_SSN(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN)),
        CREATE_INCOME(HasGlobalRole(FINANCE_ADMIN)),
        CREATE_INVOICE_CORRECTION(HasGlobalRole(FINANCE_ADMIN)),
        CREATE_PARENTSHIP(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPerson()),
        CREATE_PARTNERSHIP(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPerson()),
        DELETE,
        DISABLE_SSN_ADDING(HasGlobalRole(SERVICE_WORKER)),
        ENABLE_SSN_ADDING,
        GENERATE_RETROACTIVE_FEE_DECISIONS(HasGlobalRole(FINANCE_ADMIN)),
        GENERATE_RETROACTIVE_VOUCHER_VALUE_DECISIONS(HasGlobalRole(FINANCE_ADMIN)),
        MERGE,
        READ_CHILD_PLACEMENT_PERIODS(HasGlobalRole(FINANCE_ADMIN)),
        READ_DECISIONS(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inAnyUnit()),
        READ_FAMILY_OVERVIEW(HasGlobalRole(FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPerson()),
        READ_FEE_DECISIONS(HasGlobalRole(FINANCE_ADMIN)),
        READ_INCOME(HasGlobalRole(FINANCE_ADMIN)),
        READ_INCOME_STATEMENTS(HasGlobalRole(FINANCE_ADMIN)),
        READ_INVOICES(HasGlobalRole(FINANCE_ADMIN)),
        READ_INVOICE_ADDRESS(HasGlobalRole(FINANCE_ADMIN)),
        READ_INVOICE_CORRECTIONS(HasGlobalRole(FINANCE_ADMIN)),
        READ_OPH_OID(HasGlobalRole(DIRECTOR), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPerson()),
        READ_PARENTSHIPS(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPerson()),
        READ_PARTNERSHIPS(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPerson()),
        READ(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfPerson()),
        READ_DEPENDANTS(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPerson()),
        READ_VOUCHER_VALUE_DECISIONS(HasGlobalRole(FINANCE_ADMIN)),
        UPDATE(HasGlobalRole(FINANCE_ADMIN, SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR, STAFF).inPlacementUnitOfChildOfPerson()),
        UPDATE_INVOICE_ADDRESS(HasGlobalRole(FINANCE_ADMIN)),
        UPDATE_OPH_OID,
        UPDATE_FROM_VTJ(HasGlobalRole(SERVICE_WORKER));

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class Placement(override vararg val defaultRules: ScopedActionRule<in PlacementId>) : ScopedAction<PlacementId> {
        UPDATE(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPlacement()),
        DELETE(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPlacement()),

        CREATE_GROUP_PLACEMENT(HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPlacement()),

        CREATE_SERVICE_NEED(HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPlacement());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class ServiceNeed(override vararg val defaultRules: ScopedActionRule<in ServiceNeedId>) : ScopedAction<ServiceNeedId> {
        UPDATE(HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfServiceNeed()),
        DELETE(HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfServiceNeed());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class Unit(override vararg val defaultRules: ScopedActionRule<in DaycareId>) : ScopedAction<DaycareId> {
        READ(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inUnit()),
        READ_BASIC(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inUnit()),
        READ_DETAILED(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inUnit()),
        READ_GROUPS(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inUnit()),
        READ_CHILD_CAPACITY_FACTORS(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inUnit()),
        UPDATE,

        READ_CHILD_ATTENDANCES(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR, STAFF).inUnit(), IsMobile(requirePinLogin = false).inUnit()),
        UPDATE_CHILD_ATTENDANCES(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR, STAFF).inUnit(), IsMobile(requirePinLogin = false).inUnit()),

        READ_STAFF_ATTENDANCES(IsMobile(requirePinLogin = false).inUnit(), HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inUnit()),
        UPDATE_STAFF_ATTENDANCES(HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inUnit()),
        DELETE_STAFF_ATTENDANCES(HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inUnit()),

        READ_REALTIME_STAFF_ATTENDANCES(IsMobile(requirePinLogin = false).inUnit()),

        READ_STAFF_OCCUPANCY_COEFFICIENTS(HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inUnit()),
        UPSERT_STAFF_OCCUPANCY_COEFFICIENTS(HasUnitRole(UNIT_SUPERVISOR).inUnit()),

        READ_OCCUPANCIES(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inUnit(), IsMobile(requirePinLogin = false).inUnit()),

        READ_ATTENDANCE_RESERVATIONS(HasUnitRole(UNIT_SUPERVISOR, STAFF).inUnit()),

        READ_BACKUP_CARE(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR, STAFF).inUnit()),

        CREATE_PLACEMENT(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inUnit()),
        READ_PLACEMENT(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inUnit()),

        READ_PLACEMENT_PLAN(HasGlobalRole(SERVICE_WORKER, FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inUnit()),

        ACCEPT_PLACEMENT_PROPOSAL(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inUnit()),

        CREATE_GROUP(HasUnitRole(UNIT_SUPERVISOR).inUnit()),

        READ_MOBILE_STATS(IsMobile(requirePinLogin = false).inUnit()),
        READ_MOBILE_INFO(IsMobile(requirePinLogin = false).inUnit()),

        READ_MOBILE_DEVICES(HasUnitRole(UNIT_SUPERVISOR).inUnit()),
        CREATE_MOBILE_DEVICE_PAIRING(HasUnitRole(UNIT_SUPERVISOR).inUnit()),

        READ_ACL(HasUnitRole(UNIT_SUPERVISOR).inUnit()),
        INSERT_ACL_UNIT_SUPERVISOR,
        DELETE_ACL_UNIT_SUPERVISOR,
        INSERT_ACL_SPECIAL_EDUCATION_TEACHER,
        DELETE_ACL_SPECIAL_EDUCATION_TEACHER,
        INSERT_ACL_STAFF(HasUnitRole(UNIT_SUPERVISOR).inUnit()),
        DELETE_ACL_STAFF(HasUnitRole(UNIT_SUPERVISOR).inUnit()),
        UPDATE_STAFF_GROUP_ACL(HasUnitRole(UNIT_SUPERVISOR).inUnit()),

        READ_FAMILY_CONTACT_REPORT(HasUnitRole(UNIT_SUPERVISOR).inUnit()),
        READ_SERVICE_VOUCHER_VALUES_REPORT(HasGlobalRole(FINANCE_ADMIN), HasUnitRole(UNIT_SUPERVISOR).inUnit()),

        UPDATE_FEATURES,

        READ_RECEIVERS_FOR_NEW_MESSAGE(HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inUnit(), IsMobile(requirePinLogin = false).inUnit()),
        READ_MESSAGING_ACCOUNTS(IsMobile(requirePinLogin = false).inUnit()),
        READ_UNREAD_MESSAGES(IsMobile(requirePinLogin = false).inUnit()),

        READ_TERMINATED_PLACEMENTS(HasUnitRole(UNIT_SUPERVISOR).inUnit()),
        READ_MISSING_GROUP_PLACEMENTS(HasGlobalRole(SERVICE_WORKER), HasUnitRole(UNIT_SUPERVISOR).inUnit());

        override fun toString(): String = "${javaClass.name}.$name"
    }
    enum class VasuDocument(override vararg val defaultRules: ScopedActionRule<in VasuDocumentId>) : ScopedAction<VasuDocumentId> {
        READ(
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementUnitOfChildOfVasuDocument(),
            HasGroupRole(STAFF).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementGroupOfChildOfVasuDocument()
        ),
        UPDATE(
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementUnitOfChildOfVasuDocument(),
            HasGroupRole(STAFF).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementGroupOfChildOfVasuDocument()
        ),
        EVENT_PUBLISHED(
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementUnitOfChildOfVasuDocument(),
            HasGroupRole(STAFF).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementGroupOfChildOfVasuDocument()
        ),
        EVENT_MOVED_TO_READY(
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementUnitOfChildOfVasuDocument(),
            HasGroupRole(STAFF).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementGroupOfChildOfVasuDocument()
        ),
        EVENT_RETURNED_TO_READY(HasUnitRole(UNIT_SUPERVISOR).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementUnitOfChildOfVasuDocument()),
        EVENT_MOVED_TO_REVIEWED(
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementUnitOfChildOfVasuDocument(),
            HasGroupRole(STAFF).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementGroupOfChildOfVasuDocument()
        ),
        EVENT_RETURNED_TO_REVIEWED,
        EVENT_MOVED_TO_CLOSED;

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class VasuDocumentFollowup(override vararg val defaultRules: ScopedActionRule<in VasuDocumentFollowupEntryId>) : ScopedAction<VasuDocumentFollowupEntryId> {
        UPDATE(
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementUnitOfChildOfVasuDocumentFollowupEntry(),
            HasGroupRole(STAFF).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inPlacementGroupOfChildOfVasuDocumentFollowupEntry(),
            IsEmployee.authorOfVasuDocumentFollowupEntry()
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class VasuTemplate(override vararg val defaultRules: ScopedActionRule<in VasuTemplateId>) : ScopedAction<VasuTemplateId> {
        READ,
        COPY,
        UPDATE,
        DELETE;

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class VoucherValueDecision(override vararg val defaultRules: ScopedActionRule<in VoucherValueDecisionId>) : ScopedAction<VoucherValueDecisionId> {
        READ(HasGlobalRole(FINANCE_ADMIN)),
        UPDATE(HasGlobalRole(FINANCE_ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }
}
