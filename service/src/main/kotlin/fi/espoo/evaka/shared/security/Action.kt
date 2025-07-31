// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.AbsenceApplicationId
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ApplicationNoteId
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.AssistanceFactorId
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.AssistanceNeedVoucherCoefficientId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.BackupPickupId
import fi.espoo.evaka.shared.CalendarEventId
import fi.espoo.evaka.shared.CalendarEventTimeId
import fi.espoo.evaka.shared.ChildDailyNoteId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.ChildStickyNoteId
import fi.espoo.evaka.shared.ClubTermId
import fi.espoo.evaka.shared.DailyServiceTimeNotificationId
import fi.espoo.evaka.shared.DailyServiceTimesId
import fi.espoo.evaka.shared.DaycareAssistanceId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.FeeAlterationId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.FeeThresholdsId
import fi.espoo.evaka.shared.FinanceNoteId
import fi.espoo.evaka.shared.FosterParentId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupNoteId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.InvoiceCorrectionId
import fi.espoo.evaka.shared.InvoiceId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.OtherAssistanceMeasureId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.PaymentId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.PreschoolAssistanceId
import fi.espoo.evaka.shared.PreschoolTermId
import fi.espoo.evaka.shared.ServiceApplicationId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.auth.UserRole.ADMIN
import fi.espoo.evaka.shared.auth.UserRole.DIRECTOR
import fi.espoo.evaka.shared.auth.UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY
import fi.espoo.evaka.shared.auth.UserRole.FINANCE_ADMIN
import fi.espoo.evaka.shared.auth.UserRole.FINANCE_STAFF
import fi.espoo.evaka.shared.auth.UserRole.MESSAGING
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

sealed interface Action {
    sealed interface UnscopedAction : Action {
        val defaultRules: Array<out UnscopedActionRule>
    }

    sealed interface ScopedAction<T> : Action {
        val defaultRules: Array<out ScopedActionRule<in T>>
    }

    enum class Global(override vararg val defaultRules: UnscopedActionRule) : UnscopedAction {
        APPLICATIONS_PAGE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER).inAnyUnit(),
        ),
        DOCUMENT_TEMPLATES_PAGE(HasGlobalRole(ADMIN)),
        EMPLOYEES_PAGE(HasGlobalRole(ADMIN)),
        FINANCE_BASICS_PAGE(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        FINANCE_PAGE(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        HOLIDAY_AND_TERM_PERIODS_PAGE(HasGlobalRole(ADMIN)),
        MESSAGES_PAGE(
            HasGlobalRole(ADMIN, MESSAGING, SERVICE_WORKER, FINANCE_ADMIN),
            HasUnitRole(
                    STAFF,
                    UNIT_SUPERVISOR,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .withUnitFeatures(PilotFeature.MESSAGING)
                .inAnyUnit(),
        ),
        PERSON_SEARCH_PAGE(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, FINANCE_STAFF),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inAnyUnit(),
        ),
        REPORTS_PAGE(
            HasGlobalRole(
                ADMIN,
                DIRECTOR,
                REPORT_VIEWER,
                SERVICE_WORKER,
                FINANCE_ADMIN,
                FINANCE_STAFF,
            ),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                    STAFF,
                )
                .inAnyUnit(),
        ),
        SETTINGS_PAGE(HasGlobalRole(ADMIN)),
        UNIT_FEATURES_PAGE(HasGlobalRole(ADMIN)),
        UNITS_PAGE(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, DIRECTOR, FINANCE_STAFF),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inAnyUnit(),
        ),
        PERSONAL_MOBILE_DEVICE_PAGE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR)
                .withUnitProviderTypes(ProviderType.MUNICIPAL, ProviderType.MUNICIPAL_SCHOOL)
                .withUnitFeatures(PilotFeature.MOBILE)
                .inAnyUnit(),
            IsEmployee.ownerOfAnyMobileDevice(),
        ),
        PIN_CODE_PAGE(
            HasGlobalRole(ADMIN),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .withUnitFeatures(PilotFeature.MOBILE)
                .inAnyUnit(),
        ),
        CREATE_DOCUMENT_TEMPLATE(HasGlobalRole(ADMIN)),
        READ_DOCUMENT_TEMPLATE(
            HasGlobalRole(ADMIN, DIRECTOR),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inAnyUnit(),
        ),
        FETCH_INCOME_STATEMENTS_AWAITING_HANDLER(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        CREATE_PAPER_APPLICATION(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        READ_SERVICE_WORKER_APPLICATION_NOTES(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        WRITE_SERVICE_WORKER_APPLICATION_NOTES(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        CREATE_PERSON(HasGlobalRole(ADMIN, FINANCE_ADMIN, SERVICE_WORKER)),
        CREATE_PERSON_FROM_VTJ(HasGlobalRole(ADMIN, FINANCE_ADMIN, SERVICE_WORKER)),
        SEARCH_PEOPLE(
            HasGlobalRole(ADMIN, FINANCE_ADMIN, SERVICE_WORKER, FINANCE_STAFF),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inAnyUnit(),
        ),
        SEARCH_PEOPLE_UNRESTRICTED(
            HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF, SERVICE_WORKER)
        ),
        READ_FEE_THRESHOLDS(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        CREATE_FEE_THRESHOLDS(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        READ_VOUCHER_VALUES(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        CREATE_VOUCHER_VALUE(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        UPDATE_VOUCHER_VALUE(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        DELETE_VOUCHER_VALUE(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        SEARCH_FEE_DECISIONS(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        SEARCH_VOUCHER_VALUE_DECISIONS(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        READ_FINANCE_DECISION_HANDLERS(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, FINANCE_STAFF),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inAnyUnit(),
        ),
        READ_SELECTABLE_FINANCE_DECISION_HANDLERS(
            HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)
        ),
        READ_PERSONAL_MOBILE_DEVICES(IsEmployee.any()),
        CREATE_PERSONAL_MOBILE_DEVICE_PAIRING(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR)
                .withUnitProviderTypes(ProviderType.MUNICIPAL, ProviderType.MUNICIPAL_SCHOOL)
                .inAnyUnit(),
        ),
        SEARCH_INVOICES(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        CREATE_DRAFT_INVOICES(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        SEARCH_PAYMENTS(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        CREATE_DRAFT_PAYMENTS(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        READ_ASSISTANCE_ACTION_OPTIONS(
            HasGlobalRole(
                ADMIN,
                DIRECTOR,
                REPORT_VIEWER,
                FINANCE_ADMIN,
                SERVICE_WORKER,
                FINANCE_STAFF,
            ),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inAnyUnit(),
            IsMobile(requirePinLogin = false).any(),
        ),
        READ_SERVICE_NEED_OPTIONS(
            HasGlobalRole(
                ADMIN,
                DIRECTOR,
                REPORT_VIEWER,
                FINANCE_ADMIN,
                SERVICE_WORKER,
                FINANCE_STAFF,
                MESSAGING,
            ),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inAnyUnit(),
            IsMobile(requirePinLogin = false).any(),
        ),
        READ_UNITS(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, FINANCE_STAFF, REPORT_VIEWER),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inAnyUnit(),
        ),
        CREATE_UNIT(HasGlobalRole(ADMIN)),
        READ_CUSTOMER_FEES_REPORT(HasGlobalRole(ADMIN, FINANCE_ADMIN, REPORT_VIEWER)),
        READ_DECISION_UNITS(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inAnyUnit(),
        ),
        READ_CHILD_DOCUMENT_DECISIONS_REPORT(
            HasGlobalRole(ADMIN, DIRECTOR),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inAnyUnit(),
            IsEmployee.andIsDecisionMakerForAnyChildDocumentDecision(),
        ),
        READ_DECISIONS_REPORT(HasGlobalRole(ADMIN, SERVICE_WORKER, DIRECTOR, REPORT_VIEWER)),
        READ_DUPLICATE_PEOPLE_REPORT(HasGlobalRole(ADMIN)),
        READ_ENDED_PLACEMENTS_REPORT(HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN)),
        READ_INCOMPLETE_INCOMES_REPORT(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        READ_INVOICE_REPORT(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        READ_MISSING_HEAD_OF_FAMILY_REPORT(HasGlobalRole(ADMIN)),
        READ_PLACEMENT_SKETCHING_REPORT(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        READ_PLACEMENT_COUNT_REPORT(HasGlobalRole(ADMIN, DIRECTOR)),
        READ_PRESENCE_REPORT(HasGlobalRole(ADMIN, DIRECTOR, REPORT_VIEWER)),
        READ_RAW_REPORT(HasGlobalRole(ADMIN, REPORT_VIEWER)),
        READ_SEXTET_REPORT(HasGlobalRole(ADMIN, DIRECTOR, REPORT_VIEWER)),
        READ_UNITS_REPORT(HasGlobalRole(ADMIN)),
        READ_TITANIA_ERRORS(HasGlobalRole(ADMIN)),
        READ_VARDA_REPORT(HasGlobalRole(ADMIN)),
        READ_TAMPERE_REGIONAL_SURVEY_REPORT(HasGlobalRole(ADMIN)),
        UPDATE_SETTINGS(HasGlobalRole(ADMIN)),
        READ_SPECIAL_DIET_LIST(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inAnyUnit(),
        ),
        READ_INCOME_TYPES(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        READ_INCOME_COEFFICIENT_MULTIPLIERS(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        READ_INVOICE_CODES(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        READ_UNIT_FEATURES(HasGlobalRole(ADMIN)),
        CREATE_HOLIDAY_PERIOD(HasGlobalRole(ADMIN)),
        READ_HOLIDAY_PERIOD(HasGlobalRole(ADMIN)),
        READ_HOLIDAY_PERIODS(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR).inAnyUnit(),
            IsCitizen(allowWeakLogin = true).any(),
        ),
        DELETE_HOLIDAY_PERIOD(HasGlobalRole(ADMIN)),
        UPDATE_HOLIDAY_PERIOD(HasGlobalRole(ADMIN)),
        READ_HOLIDAY_QUESTIONNAIRE(HasGlobalRole(ADMIN)),
        READ_HOLIDAY_QUESTIONNAIRES(HasGlobalRole(ADMIN), HasUnitRole(UNIT_SUPERVISOR).inAnyUnit()),
        READ_ACTIVE_HOLIDAY_QUESTIONNAIRES(IsCitizen(allowWeakLogin = true).any()),
        CREATE_HOLIDAY_QUESTIONNAIRE(HasGlobalRole(ADMIN)),
        DELETE_HOLIDAY_QUESTIONNAIRE(HasGlobalRole(ADMIN)),
        UPDATE_HOLIDAY_QUESTIONNAIRE(HasGlobalRole(ADMIN)),
        SEND_PATU_REPORT,
        CREATE_EMPLOYEE(HasGlobalRole(ADMIN)),
        READ_EMPLOYEES(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inAnyUnit(),
        ),
        SEARCH_EMPLOYEES(HasGlobalRole(ADMIN)),
        SUBMIT_PATU_REPORT,
        READ_FUTURE_PRESCHOOLERS(HasGlobalRole(ADMIN)),
        READ_NON_SSN_CHILDREN_REPORT(HasGlobalRole(ADMIN, FINANCE_ADMIN, SERVICE_WORKER)),
        VARDA_OPERATIONS(HasGlobalRole(ADMIN)),
        CREATE_PRESCHOOL_TERM(HasGlobalRole(ADMIN)),
        CREATE_CLUB_TERM(HasGlobalRole(ADMIN)),
        READ_SYSTEM_NOTIFICATIONS(HasGlobalRole(ADMIN)),
        UPDATE_SYSTEM_NOTIFICATION(HasGlobalRole(ADMIN)),
        SEND_JAMIX_ORDERS(HasGlobalRole(ADMIN)),
        PLACEMENT_TOOL(HasGlobalRole(ADMIN)),
        OUT_OF_OFFICE_PAGE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR).withUnitFeatures(PilotFeature.MESSAGING).inAnyUnit(),
        ),
        READ_AROMI_ORDERS(HasGlobalRole(ADMIN)),
        SEND_NEKKU_ORDER(HasGlobalRole(ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }

    sealed interface Citizen : Action {
        enum class AbsenceApplication(
            override vararg val defaultRules: ScopedActionRule<in AbsenceApplicationId>
        ) : ScopedAction<AbsenceApplicationId> {
            DELETE(IsCitizen(allowWeakLogin = true).absenceApplicationCreatedBy());

            override fun toString(): String = "${javaClass.name}.$name"
        }

        enum class Application(
            override vararg val defaultRules: ScopedActionRule<in ApplicationId>
        ) : ScopedAction<ApplicationId> {
            READ(
                IsCitizen(allowWeakLogin = false).ownerOfApplication(),
                IsCitizen(allowWeakLogin = false).otherGuardianOfApplication(),
            ),
            READ_DECISIONS(
                IsCitizen(allowWeakLogin = false).ownerOfApplication(),
                IsCitizen(allowWeakLogin = false).otherGuardianOfApplication(),
            ),
            DELETE(IsCitizen(allowWeakLogin = false).ownerOfApplication()),
            UPDATE(IsCitizen(allowWeakLogin = false).ownerOfApplication()),
            UPLOAD_ATTACHMENT(IsCitizen(allowWeakLogin = false).ownerOfApplication());

            override fun toString(): String = "${javaClass.name}.$name"
        }

        enum class AssistanceNeedDecision(
            override vararg val defaultRules: ScopedActionRule<in AssistanceNeedDecisionId>
        ) : ScopedAction<AssistanceNeedDecisionId> {
            READ(
                IsCitizen(allowWeakLogin = false).guardianOfChildOfAssistanceNeedDecision(),
                IsCitizen(allowWeakLogin = false).fosterParentOfChildOfAssistanceNeedDecision(),
            ),
            DOWNLOAD(
                IsCitizen(allowWeakLogin = false).guardianOfChildOfAssistanceNeedDecision(),
                IsCitizen(allowWeakLogin = false).fosterParentOfChildOfAssistanceNeedDecision(),
            ),
            MARK_AS_READ(
                IsCitizen(allowWeakLogin = false).guardianOfChildOfAssistanceNeedDecision(),
                IsCitizen(allowWeakLogin = false).fosterParentOfChildOfAssistanceNeedDecision(),
            );

            override fun toString(): String = "${javaClass.name}.$name"
        }

        enum class AssistanceNeedPreschoolDecision(
            override vararg val defaultRules: ScopedActionRule<in AssistanceNeedPreschoolDecisionId>
        ) : ScopedAction<AssistanceNeedPreschoolDecisionId> {
            READ(
                IsCitizen(allowWeakLogin = false)
                    .guardianOfChildOfAssistanceNeedPreschoolDecision(),
                IsCitizen(allowWeakLogin = false)
                    .fosterParentOfChildOfAssistanceNeedPreschoolDecision(),
            ),
            DOWNLOAD(
                IsCitizen(allowWeakLogin = false)
                    .guardianOfChildOfAssistanceNeedPreschoolDecision(),
                IsCitizen(allowWeakLogin = false)
                    .fosterParentOfChildOfAssistanceNeedPreschoolDecision(),
            ),
            MARK_AS_READ(
                IsCitizen(allowWeakLogin = false)
                    .guardianOfChildOfAssistanceNeedPreschoolDecision(),
                IsCitizen(allowWeakLogin = false)
                    .fosterParentOfChildOfAssistanceNeedPreschoolDecision(),
            );

            override fun toString(): String = "${javaClass.name}.$name"
        }

        enum class FeeDecision(
            override vararg val defaultRules: ScopedActionRule<in FeeDecisionId>
        ) : ScopedAction<FeeDecisionId> {
            DOWNLOAD(IsCitizen(allowWeakLogin = false).liableForFeeDecisionPayment()),
            READ(IsCitizen(allowWeakLogin = false).liableForFeeDecisionPayment()),
        }

        enum class VoucherValueDecision(
            override vararg val defaultRules: ScopedActionRule<in VoucherValueDecisionId>
        ) : ScopedAction<VoucherValueDecisionId> {
            DOWNLOAD(IsCitizen(allowWeakLogin = false).liableForVoucherValueDecisionPayment()),
            READ(IsCitizen(allowWeakLogin = false).liableForVoucherValueDecisionPayment()),
        }

        enum class Child(override vararg val defaultRules: ScopedActionRule<in ChildId>) :
            ScopedAction<ChildId> {
            READ_PLACEMENT_STATUS_BY_APPLICATION_TYPE(
                IsCitizen(allowWeakLogin = false).guardianOfChild(),
                IsCitizen(allowWeakLogin = false).fosterParentOfChild(),
            ),
            READ_DUPLICATE_APPLICATIONS(
                IsCitizen(allowWeakLogin = false).guardianOfChild(),
                IsCitizen(allowWeakLogin = false).fosterParentOfChild(),
            ),
            CREATE_ABSENCE(
                IsCitizen(allowWeakLogin = true).guardianOfChild(),
                IsCitizen(allowWeakLogin = true).fosterParentOfChild(),
            ),
            CREATE_HOLIDAY_ABSENCE(
                IsCitizen(allowWeakLogin = true).guardianOfChild(),
                IsCitizen(allowWeakLogin = true).fosterParentOfChild(),
            ),
            CREATE_RESERVATION(
                IsCitizen(allowWeakLogin = true).guardianOfChild(),
                IsCitizen(allowWeakLogin = true).fosterParentOfChild(),
            ),
            READ_PLACEMENT(
                IsCitizen(allowWeakLogin = false).guardianOfChild(),
                IsCitizen(allowWeakLogin = false).fosterParentOfChild(),
            ),
            CREATE_INCOME_STATEMENT(IsCitizen(allowWeakLogin = false).guardianOfChild()),
            READ_INCOME_STATEMENTS(IsCitizen(allowWeakLogin = false).guardianOfChild()),
            CREATE_APPLICATION(
                IsCitizen(allowWeakLogin = false).guardianOfChild(),
                IsCitizen(allowWeakLogin = false).fosterParentOfChild(),
            ),
            READ_PEDAGOGICAL_DOCUMENTS(
                IsCitizen(allowWeakLogin = false).guardianOfChildWithActiveOrUpcomingPlacement(),
                IsCitizen(allowWeakLogin = false).fosterParentOfChildWithActiveOrUpcomingPlacement(),
            ),
            CREATE_ABSENCE_APPLICATION(
                IsCitizen(allowWeakLogin = true).guardianOfChild(),
                IsCitizen(allowWeakLogin = true).fosterParentOfChild(),
            ),
            READ_ABSENCE_APPLICATIONS(
                IsCitizen(allowWeakLogin = true).guardianOfChild(),
                IsCitizen(allowWeakLogin = true).fosterParentOfChild(),
            ),
            CREATE_SERVICE_APPLICATION(
                IsCitizen(allowWeakLogin = false).guardianOfChild(),
                IsCitizen(allowWeakLogin = false).fosterParentOfChild(),
            ),
            READ_SERVICE_APPLICATIONS(
                IsCitizen(allowWeakLogin = true).guardianOfChild(),
                IsCitizen(allowWeakLogin = true).fosterParentOfChild(),
            ),
            READ_SERVICE_NEEDS(
                IsCitizen(allowWeakLogin = true).guardianOfChild(),
                IsCitizen(allowWeakLogin = true).fosterParentOfChild(),
            ),
            READ_DAILY_SERVICE_TIMES(),
            READ_ATTENDANCE_SUMMARY(
                IsCitizen(allowWeakLogin = true).guardianOfChild(),
                IsCitizen(allowWeakLogin = true).fosterParentOfChild(),
            ),
            READ_CHILD_DOCUMENTS(
                IsCitizen(allowWeakLogin = false).guardianOfChildWithActiveOrUpcomingPlacement(),
                IsCitizen(allowWeakLogin = false).fosterParentOfChildWithActiveOrUpcomingPlacement(),
            ),
            CREATE_CALENDAR_EVENT_TIME_RESERVATION(
                IsCitizen(allowWeakLogin = true).guardianOfChild(),
                IsCitizen(allowWeakLogin = true).fosterParentOfChild(),
            );

            override fun toString(): String = "${javaClass.name}.$name"
        }

        enum class ChildDocument(
            override vararg val defaultRules: ScopedActionRule<in ChildDocumentId>
        ) : ScopedAction<ChildDocumentId> {
            READ(
                IsCitizen(allowWeakLogin = false).guardianOfChildOfPublishedChildDocument(),
                IsCitizen(allowWeakLogin = false).fosterParentOfChildOfPublishedChildDocument(),
            ),
            DOWNLOAD(
                IsCitizen(allowWeakLogin = false).guardianOfChildOfPublishedChildDocument(),
                IsCitizen(allowWeakLogin = false).fosterParentOfChildOfPublishedChildDocument(),
            ),
            UPDATE(
                IsCitizen(allowWeakLogin = false)
                    .guardianOfChildOfPublishedChildDocument(editable = true),
                IsCitizen(allowWeakLogin = false)
                    .fosterParentOfChildOfPublishedChildDocument(editable = true),
            );

            override fun toString(): String = "${javaClass.name}.$name"
        }

        enum class DailyServiceTimeNotification(
            override vararg val defaultRules: ScopedActionRule<in DailyServiceTimeNotificationId>
        ) : ScopedAction<DailyServiceTimeNotificationId> {
            DISMISS(IsCitizen(allowWeakLogin = true).recipientOfDailyServiceTimeNotification())
        }

        enum class Decision(override vararg val defaultRules: ScopedActionRule<in DecisionId>) :
            ScopedAction<DecisionId> {
            READ(
                IsCitizen(allowWeakLogin = false).ownerOfApplicationOfSentDecision(),
                IsCitizen(allowWeakLogin = false).otherGuardianOfApplicationOfSentDecision(),
            ),
            DOWNLOAD_PDF(
                IsCitizen(allowWeakLogin = false).ownerOfApplicationOfSentDecision(),
                IsCitizen(allowWeakLogin = false)
                    .otherGuardianOfApplicationOfSentDecisionWithNoContactInfo(),
            ),
        }

        enum class IncomeStatement(
            override vararg val defaultRules: ScopedActionRule<in IncomeStatementId>
        ) : ScopedAction<IncomeStatementId> {
            READ(
                IsCitizen(allowWeakLogin = false).ownerOfIncomeStatement(),
                IsCitizen(allowWeakLogin = false).guardianOfChildOfIncomeStatement(),
            ),
            UPDATE(
                IsCitizen(allowWeakLogin = false).ownerOfIncomeStatement(),
                IsCitizen(allowWeakLogin = false).guardianOfChildOfIncomeStatement(),
            ),
            DELETE(
                IsCitizen(allowWeakLogin = false).ownerOfIncomeStatement(),
                IsCitizen(allowWeakLogin = false).guardianOfChildOfIncomeStatement(),
            ),
            UPLOAD_ATTACHMENT(
                IsCitizen(allowWeakLogin = false).ownerOfIncomeStatement(),
                IsCitizen(allowWeakLogin = false).guardianOfChildOfIncomeStatement(),
            );

            override fun toString(): String = "${javaClass.name}.$name"
        }

        enum class PedagogicalDocument(
            override vararg val defaultRules: ScopedActionRule<in PedagogicalDocumentId>
        ) : ScopedAction<PedagogicalDocumentId> {
            READ(
                IsCitizen(allowWeakLogin = false).guardianOfChildOfPedagogicalDocument(),
                IsCitizen(allowWeakLogin = false).fosterParentOfChildOfPedagogicalDocument(),
            );

            override fun toString(): String = "${javaClass.name}.$name"
        }

        enum class Person(override vararg val defaultRules: ScopedActionRule<in PersonId>) :
            ScopedAction<PersonId> {
            CREATE_INCOME_STATEMENT(IsCitizen(allowWeakLogin = false).self()),
            READ_APPLICATIONS(IsCitizen(allowWeakLogin = false).self()),
            READ_APPLICATION_CHILDREN(IsCitizen(allowWeakLogin = false).self()),
            READ_APPLICATION_NOTIFICATIONS(IsCitizen(allowWeakLogin = true).self()),
            READ_ASSISTANCE_NEED_DECISIONS(IsCitizen(allowWeakLogin = false).self()),
            READ_ASSISTANCE_NEED_PRESCHOOL_DECISIONS(IsCitizen(allowWeakLogin = false).self()),
            READ_CALENDAR_EVENTS(IsCitizen(allowWeakLogin = true).self()),
            READ_CHILDREN(IsCitizen(allowWeakLogin = true).self()),
            READ_DAILY_SERVICE_TIME_NOTIFICATIONS(IsCitizen(allowWeakLogin = true).self()),
            READ_EXPIRED_INCOME_DATES(IsCitizen(allowWeakLogin = true).self()),
            READ_FINANCE_DECISIONS(IsCitizen(allowWeakLogin = false).self()),
            READ_INCOME_STATEMENTS(IsCitizen(allowWeakLogin = false).self()),
            READ_PARTNER_INCOME_STATEMENT_STATUS(IsCitizen(allowWeakLogin = false).self()),
            READ_PEDAGOGICAL_DOCUMENT_UNREAD_COUNTS(IsCitizen(allowWeakLogin = true).self()),
            READ_RESERVATIONS(IsCitizen(allowWeakLogin = true).self()),
            READ_UNREAD_ASSISTANCE_NEED_DECISION_COUNT(IsCitizen(allowWeakLogin = true).self()),
            READ_UNREAD_ASSISTANCE_NEED_PRESCHOOL_DECISION_COUNT(
                IsCitizen(allowWeakLogin = true).self()
            ),
            READ_CHILD_DOCUMENTS_UNREAD_COUNT(IsCitizen(allowWeakLogin = true).self()),
            UPDATE_PERSONAL_DATA(IsCitizen(allowWeakLogin = false).self()),
            READ_NOTIFICATION_SETTINGS(IsCitizen(allowWeakLogin = true).self()),
            UPDATE_NOTIFICATION_SETTINGS(IsCitizen(allowWeakLogin = true).self()),
            UPDATE_WEAK_LOGIN_CREDENTIALS(IsCitizen(allowWeakLogin = false).self()),
            READ_EMAIL_VERIFICATION_STATUS(IsCitizen(allowWeakLogin = true).self()),
            VERIFY_EMAIL(IsCitizen(allowWeakLogin = false).self());

            override fun toString(): String = "${javaClass.name}.$name"
        }

        enum class Placement(override vararg val defaultRules: ScopedActionRule<in PlacementId>) :
            ScopedAction<PlacementId> {
            TERMINATE(
                IsCitizen(allowWeakLogin = false).guardianOfChildOfPlacement(),
                IsCitizen(allowWeakLogin = false).fosterParentOfChildOfPlacement(),
            );

            override fun toString(): String = "${javaClass.name}.$name"
        }

        enum class ServiceApplication(
            override vararg val defaultRules: ScopedActionRule<in ServiceApplicationId>
        ) : ScopedAction<ServiceApplicationId> {
            DELETE(IsCitizen(allowWeakLogin = false).ownerOfServiceApplication());

            override fun toString(): String = "${javaClass.name}.$name"
        }

        enum class CalendarEventTime(
            override vararg val defaultRules: ScopedActionRule<in CalendarEventTimeId>
        ) : ScopedAction<CalendarEventTimeId> {
            CANCEL_RESERVATION(
                IsCitizen(allowWeakLogin = true).guardianOfChildOfCalendarEventTimeReservation(),
                IsCitizen(allowWeakLogin = true).fosterParentOfChildOfCalendarEventTimeReservation(),
            )
        }
    }

    enum class AbsenceApplication(
        override vararg val defaultRules: ScopedActionRule<in AbsenceApplicationId>
    ) : ScopedAction<AbsenceApplicationId> {
        READ(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfAbsenceApplication(),
            HasGroupRole(STAFF).inPlacementGroupOfChildOfAbsenceApplication(),
        ),
        DECIDE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfAbsenceApplication(),
        ),
        DECIDE_MAX_WEEK(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfAbsenceApplication(),
            HasGroupRole(STAFF).inPlacementGroupOfChildOfAbsenceApplication(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class Application(override vararg val defaultRules: ScopedActionRule<in ApplicationId>) :
        ScopedAction<ApplicationId> {
        READ(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementPlanUnitOfApplication(),
        ),
        READ_METADATA(HasGlobalRole(ADMIN)),
        READ_IF_HAS_ASSISTANCE_NEED(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .inPlacementPlanUnitOfApplication(),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPreferredUnitOfApplication(),
        ),
        UPDATE(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        SEND(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            IsCitizen(allowWeakLogin = false).ownerOfApplication(),
        ),
        CANCEL(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            IsCitizen(allowWeakLogin = false).ownerOfApplication(),
        ),
        MOVE_TO_WAITING_PLACEMENT(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        RETURN_TO_SENT(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        VERIFY(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        READ_PLACEMENT_PLAN_DRAFT(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        CREATE_PLACEMENT_PLAN(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        CANCEL_PLACEMENT_PLAN(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        READ_DECISION_DRAFT(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        UPDATE_DECISION_DRAFT(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        SEND_DECISIONS_WITHOUT_PROPOSAL(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        SEND_PLACEMENT_PROPOSAL(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        WITHDRAW_PLACEMENT_PROPOSAL(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        RESPOND_TO_PLACEMENT_PROPOSAL(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementPlanUnitOfApplication(),
        ),
        CONFIRM_DECISIONS_MAILED(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        ACCEPT_DECISION(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementPlanUnitOfApplication(),
            IsCitizen(allowWeakLogin = false).ownerOfApplication(),
            IsCitizen(allowWeakLogin = false).otherGuardianOfApplicationAndLivesInTheSameAddress(),
        ),
        REJECT_DECISION(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementPlanUnitOfApplication(),
            IsCitizen(allowWeakLogin = false).ownerOfApplication(),
            IsCitizen(allowWeakLogin = false).otherGuardianOfApplicationAndLivesInTheSameAddress(),
        ),
        READ_NOTES(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPreferredUnitOfApplication(),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPlacementPlanUnitOfApplication(),
        ),
        CREATE_NOTE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPreferredUnitOfApplication(),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPlacementPlanUnitOfApplication(),
        ),
        READ_SPECIAL_EDUCATION_TEACHER_NOTES(
            HasGlobalRole(ADMIN),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPreferredUnitOfApplication(),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPlacementPlanUnitOfApplication(),
        ),
        UPLOAD_ATTACHMENT(HasGlobalRole(ADMIN, SERVICE_WORKER));

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class ApplicationNote(
        override vararg val defaultRules: ScopedActionRule<in ApplicationNoteId>
    ) : ScopedAction<ApplicationNoteId> {
        UPDATE(HasGlobalRole(ADMIN, SERVICE_WORKER), IsEmployee.authorOfApplicationNote()),
        DELETE(HasGlobalRole(ADMIN, SERVICE_WORKER), IsEmployee.authorOfApplicationNote());

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class FinanceNote(override vararg val defaultRules: ScopedActionRule<in FinanceNoteId>) :
        ScopedAction<FinanceNoteId> {
        UPDATE(
            HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF),
            IsEmployee.authorOfFinanceNote(),
        ),
        DELETE(
            HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF),
            IsEmployee.authorOfFinanceNote(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class AssistanceAction(
        override vararg val defaultRules: ScopedActionRule<in AssistanceActionId>
    ) : ScopedAction<AssistanceActionId> {
        UPDATE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfAssistanceAction(false),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfAssistanceAction(true),
        ),
        DELETE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfAssistanceAction(false),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfAssistanceAction(true),
        ),
        READ(
            HasGlobalRole(ADMIN),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfAssistanceAction(false),
            HasUnitRole(STAFF, UNIT_SUPERVISOR).inPlacementUnitOfChildOfAssistanceAction(true),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class AssistanceFactor(
        override vararg val defaultRules: ScopedActionRule<in AssistanceFactorId>
    ) : ScopedAction<AssistanceFactorId> {
        UPDATE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfAssistanceFactor(false),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfAssistanceFactor(true),
        ),
        DELETE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfAssistanceFactor(false),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfAssistanceFactor(true),
        ),
        READ(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfAssistanceFactor(false),
            HasUnitRole(STAFF, UNIT_SUPERVISOR).inPlacementUnitOfChildOfAssistanceFactor(true),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class AssistanceNeedDecision(
        override vararg val defaultRules: ScopedActionRule<in AssistanceNeedDecisionId>
    ) : ScopedAction<AssistanceNeedDecisionId> {
        READ_DECISION_MAKER_OPTIONS(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfAssistanceNeedDecision(false),
            HasUnitRole(STAFF, UNIT_SUPERVISOR).inPlacementUnitOfChildOfAssistanceNeedDecision(true),
        ),
        UPDATE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfAssistanceNeedDecision(false),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfAssistanceNeedDecision(true),
        ),
        DELETE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfAssistanceNeedDecision(false),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfAssistanceNeedDecision(true),
        ),
        READ(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfAssistanceNeedDecision(false),
            HasGlobalRole(DIRECTOR).andAssistanceNeedDecisionHasBeenSent(),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfAssistanceNeedDecision(true),
            HasUnitRole(STAFF).inPlacementUnitOfChildOfAcceptedAssistanceNeedDecision(true),
        ),
        SEND(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfAssistanceNeedDecision(false),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfAssistanceNeedDecision(true),
        ),
        REVERT_TO_UNSENT(HasGlobalRole(ADMIN)),
        READ_IN_REPORT(
            HasGlobalRole(ADMIN),
            HasGlobalRole(DIRECTOR).andIsDecisionMakerForAssistanceNeedDecision(),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .inSelectedUnitOfAssistanceNeedDecision(ChildAclConfig(application = false)),
        ),
        READ_METADATA(HasGlobalRole(ADMIN)),
        DECIDE(IsEmployee.andIsDecisionMakerForAssistanceNeedDecision()),
        MARK_AS_OPENED(IsEmployee.andIsDecisionMakerForAssistanceNeedDecision()),
        UPDATE_DECISION_MAKER(HasGlobalRole(ADMIN)),
        ANNUL(HasGlobalRole(ADMIN), IsEmployee.andIsDecisionMakerForAssistanceNeedDecision());

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class AssistanceNeedPreschoolDecision(
        override vararg val defaultRules: ScopedActionRule<in AssistanceNeedPreschoolDecisionId>
    ) : ScopedAction<AssistanceNeedPreschoolDecisionId> {
        READ_DECISION_MAKER_OPTIONS(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfAssistanceNeedPreschoolDecision(),
        ),
        UPDATE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfAssistanceNeedPreschoolDecision(),
        ),
        DELETE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfAssistanceNeedPreschoolDecision(),
        ),
        READ(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasGlobalRole(DIRECTOR).andAssistanceNeedPreschoolDecisionHasBeenSent(),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfAssistanceNeedPreschoolDecision(),
            HasUnitRole(STAFF).inPlacementUnitOfChildOfAcceptedAssistanceNeedPreschoolDecision(),
        ),
        SEND(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfAssistanceNeedPreschoolDecision(),
        ),
        REVERT_TO_UNSENT(HasGlobalRole(ADMIN)),
        READ_IN_REPORT(
            HasGlobalRole(ADMIN),
            HasGlobalRole(DIRECTOR).andIsDecisionMakerForPreschoolAssistanceNeedDecision(),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .inSelectedUnitOfAssistanceNeedPreschoolDecision(
                    ChildAclConfig(application = false)
                ),
        ),
        READ_METADATA(HasGlobalRole(ADMIN)),
        DECIDE(IsEmployee.andIsDecisionMakerForAssistanceNeedPreschoolDecision()),
        MARK_AS_OPENED(IsEmployee.andIsDecisionMakerForAssistanceNeedPreschoolDecision()),
        ANNUL(
            HasGlobalRole(ADMIN),
            IsEmployee.andIsDecisionMakerForAssistanceNeedPreschoolDecision(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class AssistanceNeedVoucherCoefficient(
        override vararg val defaultRules: ScopedActionRule<in AssistanceNeedVoucherCoefficientId>
    ) : ScopedAction<AssistanceNeedVoucherCoefficientId> {
        UPDATE(
            HasGlobalRole(ADMIN),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfAssistanceNeedVoucherCoefficientWithServiceVoucherPlacement(),
        ),
        DELETE(
            HasGlobalRole(ADMIN),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfAssistanceNeedVoucherCoefficientWithServiceVoucherPlacement(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class Attachment(override vararg val defaultRules: ScopedActionRule<in AttachmentId>) :
        ScopedAction<AttachmentId> {
        READ_ORPHAN_ATTACHMENT(IsCitizen(allowWeakLogin = false).uploaderOfAttachment()),
        READ_APPLICATION_ATTACHMENT(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR).inUnitOfApplicationAttachment(),
            IsCitizen(allowWeakLogin = false).uploaderOfAttachment(),
        ),
        READ_INCOME_STATEMENT_ATTACHMENT(
            HasGlobalRole(ADMIN, FINANCE_ADMIN),
            IsCitizen(allowWeakLogin = false).uploaderOfAttachment(),
        ),
        READ_INCOME_ATTACHMENT(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        READ_INVOICE_ATTACHMENT(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        READ_PEDAGOGICAL_DOCUMENT_ATTACHMENT(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementUnitOfChildOfPedagogicalDocumentOfAttachment(),
            IsCitizen(allowWeakLogin = false).guardianOfChildOfPedagogicalDocumentOfAttachment(),
            IsCitizen(allowWeakLogin = false).fosterParentOfChildOfPedagogicalDocumentOfAttachment(),
        ),
        READ_FEE_ALTERATION_ATTACHMENT(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        DELETE_ORPHAN_ATTACHMENT(
            IsEmployee.uploaderOfAttachment(),
            IsCitizen(allowWeakLogin = false).uploaderOfAttachment(),
        ),
        DELETE_APPLICATION_ATTACHMENT(
            HasGlobalRole(ADMIN),
            HasGlobalRole(SERVICE_WORKER).andAttachmentWasUploadedByAnyEmployee(),
            IsCitizen(allowWeakLogin = false).uploaderOfAttachment(),
        ),
        DELETE_INCOME_STATEMENT_ATTACHMENT(
            HasGlobalRole(ADMIN),
            HasGlobalRole(FINANCE_ADMIN).andAttachmentWasUploadedByAnyEmployee(),
            IsCitizen(allowWeakLogin = false).uploaderOfAttachment(),
        ),
        DELETE_INCOME_ATTACHMENT(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        DELETE_INVOICE_ATTACHMENT(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        DELETE_MESSAGE_CONTENT_ATTACHMENT(HasGlobalRole(ADMIN)),
        DELETE_PEDAGOGICAL_DOCUMENT_ATTACHMENT(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementUnitOfChildOfPedagogicalDocumentOfAttachment(),
        ),
        DELETE_FEE_ALTERATION_ATTACHMENTS(HasGlobalRole(ADMIN, FINANCE_ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class BackupCare(override vararg val defaultRules: ScopedActionRule<in BackupCareId>) :
        ScopedAction<BackupCareId> {
        UPDATE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inPlacementUnitOfChildOfBackupCare(),
        ),
        DELETE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inPlacementUnitOfChildOfBackupCare(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class BackupPickup(override vararg val defaultRules: ScopedActionRule<in BackupPickupId>) :
        ScopedAction<BackupPickupId> {
        UPDATE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF).inPlacementUnitOfChildOfBackupPickup(),
        ),
        DELETE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF).inPlacementUnitOfChildOfBackupPickup(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class CalendarEvent(
        override vararg val defaultRules: ScopedActionRule<in CalendarEventId>
    ) : ScopedAction<CalendarEventId> {
        READ(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inUnitOfCalendarEvent(),
        ),
        DELETE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inUnitOfCalendarEvent(),
        ),
        UPDATE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inUnitOfCalendarEvent(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class CalendarEventTime(
        override vararg val defaultRules: ScopedActionRule<in CalendarEventTimeId>
    ) : ScopedAction<CalendarEventTimeId> {
        DELETE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF)
                .inUnitOfCalendarEventTime(),
        ),
        UPDATE_RESERVATION(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF)
                .inUnitOfCalendarEventTime(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class Child(override vararg val defaultRules: ScopedActionRule<in ChildId>) :
        ScopedAction<ChildId> {
        READ(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, FINANCE_STAFF),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inPlacementUnitOfChild(),
        ),
        READ_ABSENCE_APPLICATIONS(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChild(),
            HasGroupRole(STAFF).inPlacementGroupOfChild(),
        ),
        CREATE_ABSENCE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
        ),
        READ_ABSENCES(
            HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChild(),
        ),
        READ_FUTURE_ABSENCES(
            HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChild(),
            IsMobile(requirePinLogin = false).inPlacementUnitOfChild(),
        ),
        READ_ATTENDANCE_REPORT(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
        ),
        DELETE_ABSENCE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
            IsMobile(requirePinLogin = false).inPlacementUnitOfChild(),
        ),
        DELETE_ABSENCE_RANGE(
            HasGlobalRole(ADMIN),
            IsMobile(requirePinLogin = false).inPlacementUnitOfChild(),
        ),
        READ_NON_RESERVABLE_RESERVATIONS(
            IsMobile(requirePinLogin = false).inPlacementUnitOfChild()
        ),
        UPDATE_NON_RESERVABLE_RESERVATIONS(
            IsMobile(requirePinLogin = false).inPlacementUnitOfChild()
        ),
        DELETE_HOLIDAY_RESERVATIONS(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
        ),
        READ_ADDITIONAL_INFO(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inPlacementUnitOfChild(),
        ),
        UPDATE_ADDITIONAL_INFO(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
        ),
        READ_APPLICATION(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        READ_ASSISTANCE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                    SPECIAL_EDUCATION_TEACHER,
                )
                .inPlacementUnitOfChild(),
        ),
        CREATE_ASSISTANCE_FACTOR(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
        ),
        READ_ASSISTANCE_FACTORS(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                    SPECIAL_EDUCATION_TEACHER,
                )
                .inPlacementUnitOfChild(),
        ), // used in UI
        CREATE_DAYCARE_ASSISTANCE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
        ),
        READ_DAYCARE_ASSISTANCES(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                    SPECIAL_EDUCATION_TEACHER,
                )
                .inPlacementUnitOfChild(),
        ), // used in UI
        CREATE_PRESCHOOL_ASSISTANCE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
        ),
        READ_PRESCHOOL_ASSISTANCES(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                    SPECIAL_EDUCATION_TEACHER,
                )
                .inPlacementUnitOfChild(),
        ), // used in UI
        CREATE_ASSISTANCE_ACTION(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
        ),
        READ_ASSISTANCE_ACTION(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                    SPECIAL_EDUCATION_TEACHER,
                )
                .inPlacementUnitOfChild(),
        ), // used in UI
        CREATE_OTHER_ASSISTANCE_MEASURE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
        ),
        READ_OTHER_ASSISTANCE_MEASURES(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                    SPECIAL_EDUCATION_TEACHER,
                )
                .inPlacementUnitOfChild(),
        ), // used in UI
        CREATE_ASSISTANCE_NEED_DECISION(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
        ),
        READ_ASSISTANCE_NEED_DECISIONS(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
        ), // used in UI
        CREATE_ASSISTANCE_NEED_PRESCHOOL_DECISION(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
        ),
        READ_ASSISTANCE_NEED_PRESCHOOL_DECISIONS(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
        ), // used in UI
        READ_ASSISTANCE_NEED_VOUCHER_COEFFICIENTS(
            HasGlobalRole(ADMIN),
            HasGlobalRole(SERVICE_WORKER).andChildHasServiceVoucherPlacement(),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildWithServiceVoucherPlacement(),
        ),
        CREATE_ASSISTANCE_NEED_VOUCHER_COEFFICIENT(
            HasGlobalRole(ADMIN),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
        ),
        CREATE_ATTENDANCE_RESERVATION(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inPlacementUnitOfChild(),
        ),
        UPSERT_CHILD_DATE_PRESENCE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inPlacementUnitOfChild(),
        ),
        CREATE_BACKUP_CARE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inPlacementUnitOfChild(),
        ),
        READ_ONGOING_ATTENDANCE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inPlacementUnitOfChild(),
        ),
        READ_BACKUP_CARE(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, FINANCE_STAFF),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inPlacementUnitOfChild(),
        ),
        CREATE_BACKUP_PICKUP(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
        ),
        READ_BACKUP_PICKUP(
            HasGlobalRole(ADMIN),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inPlacementUnitOfChild(),
        ),
        CREATE_DAILY_NOTE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
            IsMobile(requirePinLogin = false).inPlacementUnitOfChild(),
        ),
        CREATE_STICKY_NOTE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
            IsMobile(requirePinLogin = false).inPlacementUnitOfChild(),
        ),
        READ_DAILY_SERVICE_TIMES(),
        CREATE_DAILY_SERVICE_TIME(),
        READ_PLACEMENT(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, FINANCE_STAFF),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    SPECIAL_EDUCATION_TEACHER,
                    STAFF,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inPlacementUnitOfChild(),
        ),
        READ_SERVICE_APPLICATIONS(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR)
                .withUnitFeatures(PilotFeature.SERVICE_APPLICATIONS)
                .inPlacementUnitOfChild(),
        ),
        READ_FAMILY_CONTACTS(
            HasGlobalRole(ADMIN),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inPlacementUnitOfChild(),
        ),
        UPDATE_FAMILY_CONTACT_DETAILS(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
        ),
        UPDATE_FAMILY_CONTACT_PRIORITY(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChild(),
        ),
        READ_GUARDIANS(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, FINANCE_STAFF),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inPlacementUnitOfChild(),
        ),
        READ_BLOCKED_GUARDIANS(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        CREATE_FEE_ALTERATION(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        READ_FEE_ALTERATIONS(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        CREATE_CHILD_DOCUMENT(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementUnitOfChild(),
            HasGroupRole(STAFF)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementGroupOfChild(),
        ),
        READ_CHILD_DOCUMENT(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementUnitOfChild(),
            HasGroupRole(STAFF)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementGroupOfChild(),
        ),
        CREATE_PEDAGOGICAL_DOCUMENT(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementUnitOfChild(),
        ),
        READ_PEDAGOGICAL_DOCUMENTS(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementUnitOfChild(),
        ),
        READ_BASIC_INFO(
            HasGlobalRole(ADMIN),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inPlacementUnitOfChild(),
            IsMobile(requirePinLogin = true).inPlacementUnitOfChild(),
        ),
        READ_SENSITIVE_INFO(
            HasGlobalRole(ADMIN),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inPlacementUnitOfChild(),
            IsMobile(requirePinLogin = true).inPlacementUnitOfChild(),
        ),
        UPLOAD_IMAGE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inPlacementUnitOfChild(),
            IsMobile(requirePinLogin = false).inPlacementUnitOfChild(),
        ),
        DELETE_IMAGE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inPlacementUnitOfChild(),
            IsMobile(requirePinLogin = false).inPlacementUnitOfChild(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class ChildDailyNote(
        override vararg val defaultRules: ScopedActionRule<in ChildDailyNoteId>
    ) : ScopedAction<ChildDailyNoteId> {
        UPDATE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfChildDailyNote(),
            IsMobile(requirePinLogin = false).inPlacementUnitOfChildOfChildDailyNote(),
        ),
        DELETE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfChildDailyNote(),
            IsMobile(requirePinLogin = false).inPlacementUnitOfChildOfChildDailyNote(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class ChildImage(override vararg val defaultRules: ScopedActionRule<in ChildImageId>) :
        ScopedAction<ChildImageId> {
        DOWNLOAD(
            HasGlobalRole(ADMIN),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    SPECIAL_EDUCATION_TEACHER,
                    STAFF,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inPlacementUnitOfChildOfChildImage(),
            IsMobile(requirePinLogin = false).inPlacementUnitOfChildOfChildImage(),
            IsCitizen(allowWeakLogin = true).guardianOfChildOfChildImage(),
            IsCitizen(allowWeakLogin = true).fosterParentOfChildOfChildImage(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class ChildStickyNote(
        override vararg val defaultRules: ScopedActionRule<in ChildStickyNoteId>
    ) : ScopedAction<ChildStickyNoteId> {
        UPDATE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfChildStickyNote(),
            IsMobile(requirePinLogin = false).inPlacementUnitOfChildOfChildStickyNote(),
        ),
        DELETE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfChildStickyNote(),
            IsMobile(requirePinLogin = false).inPlacementUnitOfChildOfChildStickyNote(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class ClubTerm(override vararg val defaultRules: ScopedActionRule<in ClubTermId>) :
        ScopedAction<ClubTermId> {
        UPDATE(HasGlobalRole(ADMIN)),
        DELETE(HasGlobalRole(ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class DailyServiceTime(
        override vararg val defaultRules: ScopedActionRule<in DailyServiceTimesId>
    ) : ScopedAction<DailyServiceTimesId> {
        UPDATE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfDailyServiceTime(),
        ),
        DELETE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfFutureDailyServiceTime(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class DaycareAssistance(
        override vararg val defaultRules: ScopedActionRule<in DaycareAssistanceId>
    ) : ScopedAction<DaycareAssistanceId> {
        UPDATE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfDaycareAssistance(false),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfDaycareAssistance(true),
        ),
        DELETE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfDaycareAssistance(false),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfDaycareAssistance(true),
        ),
        READ(
            HasGlobalRole(ADMIN),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER).inPlacementUnitOfChildOfDaycareAssistance(false),
            HasUnitRole(STAFF, UNIT_SUPERVISOR).inPlacementUnitOfChildOfDaycareAssistance(true),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class Decision(override vararg val defaultRules: ScopedActionRule<in DecisionId>) :
        ScopedAction<DecisionId> {
        READ(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfDecision(),
        ),
        DOWNLOAD_PDF(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inPlacementUnitOfChildOfDecision(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class DocumentTemplate(
        override vararg val defaultRules: ScopedActionRule<in DocumentTemplateId>
    ) : ScopedAction<DocumentTemplateId> {
        READ(HasGlobalRole(ADMIN)),
        COPY(HasGlobalRole(ADMIN)),
        UPDATE(HasGlobalRole(ADMIN)),
        DELETE(HasGlobalRole(ADMIN)),
        FORCE_UNPUBLISH(HasGlobalRole(ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class Employee(override vararg val defaultRules: ScopedActionRule<in EmployeeId>) :
        ScopedAction<EmployeeId> {
        READ_DETAILS(HasGlobalRole(ADMIN)),
        DELETE(HasGlobalRole(ADMIN)),
        UPDATE_GLOBAL_ROLES(HasGlobalRole(ADMIN)),
        UPDATE_DAYCARE_ROLES(HasGlobalRole(ADMIN)),
        DELETE_DAYCARE_ROLES(HasGlobalRole(ADMIN)),
        UPDATE_STAFF_ATTENDANCES(
            HasGlobalRole(ADMIN, SERVICE_WORKER, DIRECTOR),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inAnyUnit(),
            IsEmployee.self(),
        ),
        ACTIVATE(HasGlobalRole(ADMIN)),
        DEACTIVATE(HasGlobalRole(ADMIN)),
        READ_OPEN_GROUP_ATTENDANCE(
            HasGlobalRole(ADMIN),
            IsMobile(false).isAssociatedWithEmployee(),
            IsEmployee.isInSameUnitWithEmployee(),
        ),
        READ_OUT_OF_OFFICE(HasGlobalRole(ADMIN), HasUnitRole(UNIT_SUPERVISOR).inAnyUnit()),
        UPDATE_OUT_OF_OFFICE(HasGlobalRole(ADMIN), HasUnitRole(UNIT_SUPERVISOR).inAnyUnit()),
        UPDATE_EMAIL(HasGlobalRole(ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class FeeAlteration(
        override vararg val defaultRules: ScopedActionRule<in FeeAlterationId>
    ) : ScopedAction<FeeAlterationId> {
        UPDATE(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        DELETE(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        UPLOAD_ATTACHMENT(HasGlobalRole(ADMIN, FINANCE_ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class FeeDecision(override vararg val defaultRules: ScopedActionRule<in FeeDecisionId>) :
        ScopedAction<FeeDecisionId> {
        READ(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        READ_METADATA(HasGlobalRole(ADMIN)),
        UPDATE(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        IGNORE(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        UNIGNORE(HasGlobalRole(ADMIN, FINANCE_ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class FeeThresholds(
        override vararg val defaultRules: ScopedActionRule<in FeeThresholdsId>
    ) : ScopedAction<FeeThresholdsId> {
        UPDATE(HasGlobalRole(ADMIN, FINANCE_ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class FosterParent(override vararg val defaultRules: ScopedActionRule<in FosterParentId>) :
        ScopedAction<FosterParentId> {
        DELETE(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        UPDATE(HasGlobalRole(ADMIN, SERVICE_WORKER));

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class Group(override vararg val defaultRules: ScopedActionRule<in GroupId>) :
        ScopedAction<GroupId> {
        UPDATE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnitOfGroup(),
        ),
        DELETE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnitOfGroup(),
        ),
        CREATE_ABSENCES(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inUnitOfGroup(),
        ),
        READ_ABSENCES(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, FINANCE_STAFF),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inUnitOfGroup(),
        ),
        DELETE_ABSENCES(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inUnitOfGroup(),
        ),
        READ_STAFF_ATTENDANCES(
            HasGlobalRole(ADMIN, FINANCE_ADMIN),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inUnitOfGroup(),
            IsMobile(requirePinLogin = false).inUnitOfGroup(),
        ),
        UPDATE_STAFF_ATTENDANCES(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF).inUnitOfGroup(),
            IsMobile(requirePinLogin = false).inUnitOfGroup(),
        ),
        READ_CARETAKERS(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inUnitOfGroup(),
        ),
        CREATE_CARETAKERS(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnitOfGroup(),
        ),
        UPDATE_CARETAKERS(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnitOfGroup(),
        ),
        DELETE_CARETAKERS(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnitOfGroup(),
        ),
        CREATE_NOTE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inUnitOfGroup(),
            IsMobile(requirePinLogin = false).inUnitOfGroup(),
        ),
        READ_NOTES(
            HasGlobalRole(ADMIN),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inUnitOfGroup(),
            IsMobile(requirePinLogin = false).inUnitOfGroup(),
        ),
        MARK_DEPARTURE(IsMobile(requirePinLogin = false).inUnitOfGroup()),
        MARK_EXTERNAL_DEPARTURE(IsMobile(requirePinLogin = false).inUnitOfGroup()),
        MARK_ARRIVAL(IsMobile(requirePinLogin = false).inUnitOfGroup()),
        MARK_EXTERNAL_ARRIVAL(IsMobile(requirePinLogin = false).inUnitOfGroup()),
        RECEIVE_PUSH_NOTIFICATIONS(IsMobile(requirePinLogin = false).inUnitOfGroup()),
        CREATE_CALENDAR_EVENT(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inUnitOfGroup(),
        ),
        CREATE_CHILD_DOCUMENTS(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inUnitOfGroup(),
            HasGroupRole(STAFF).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inGroup(),
        ), // used in UI
        READ_CITIZEN_DOCUMENT_RESPONSE_REPORT(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inUnitOfGroup(),
            HasGroupRole(STAFF).withUnitFeatures(PilotFeature.VASU_AND_PEDADOC).inGroup(),
        ),
        NEKKU_MANUAL_ORDER(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF).inUnitOfGroup(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class GroupNote(override vararg val defaultRules: ScopedActionRule<in GroupNoteId>) :
        ScopedAction<GroupNoteId> {
        UPDATE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inUnitOfGroupNote(),
            IsMobile(requirePinLogin = false).inUnitOfGroupNote(),
        ),
        DELETE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER).inUnitOfGroupNote(),
            IsMobile(requirePinLogin = false).inUnitOfGroupNote(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class GroupPlacement(
        override vararg val defaultRules: ScopedActionRule<in GroupPlacementId>
    ) : ScopedAction<GroupPlacementId> {
        UPDATE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inUnitOfGroupPlacement(),
        ),
        DELETE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inUnitOfGroupPlacement(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class Income(override vararg val defaultRules: ScopedActionRule<in IncomeId>) :
        ScopedAction<IncomeId> {
        UPDATE(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        DELETE(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        UPLOAD_ATTACHMENT(HasGlobalRole(ADMIN, FINANCE_ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class IncomeStatement(
        override vararg val defaultRules: ScopedActionRule<in IncomeStatementId>
    ) : ScopedAction<IncomeStatementId> {
        READ(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        UPDATE_HANDLED(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        UPLOAD_ATTACHMENT(HasGlobalRole(ADMIN, FINANCE_ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class Invoice(override vararg val defaultRules: ScopedActionRule<in InvoiceId>) :
        ScopedAction<InvoiceId> {
        READ(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        UPLOAD_ATTACHMENT(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        MARK_SENT(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        SEND(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        RESEND,
        DELETE(HasGlobalRole(ADMIN, FINANCE_ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class InvoiceCorrection(
        override vararg val defaultRules: ScopedActionRule<in InvoiceCorrectionId>
    ) : ScopedAction<InvoiceCorrectionId> {
        DELETE(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        UPDATE_NOTE(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF));

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class MessageAccount(
        override vararg val defaultRules: ScopedActionRule<in MessageAccountId>
    ) : ScopedAction<MessageAccountId> {
        ACCESS(
            IsEmployee.hasPersonalMessageAccount(),
            HasUnitRole(UNIT_SUPERVISOR).hasDaycareMessageAccount(),
            IsEmployee.hasDaycareGroupMessageAccount(),
            IsEmployee.hasMunicipalMessageAccount(),
            IsEmployee.hasServiceWorkerMessageAccount(),
            IsEmployee.hasFinanceMessageAccount(),
            IsMobile(requirePinLogin = true).hasPersonalMessageAccount(),
            IsMobile(requirePinLogin = true).hasDaycareMessageAccount(UNIT_SUPERVISOR),
            IsMobile(requirePinLogin = true).hasDaycareGroupMessageAccount(),
            IsCitizen(allowWeakLogin = true).hasMessageAccount(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class MobileDevice(override vararg val defaultRules: ScopedActionRule<in MobileDeviceId>) :
        ScopedAction<MobileDeviceId> {
        UPDATE_NAME(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inUnitOfMobileDevice(),
            IsEmployee.ownerOfMobileDevice(),
        ),
        DELETE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inUnitOfMobileDevice(),
            IsEmployee.ownerOfMobileDevice(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class OtherAssistanceMeasure(
        override vararg val defaultRules: ScopedActionRule<in OtherAssistanceMeasureId>
    ) : ScopedAction<OtherAssistanceMeasureId> {
        UPDATE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfOtherAssistanceMeasure(false),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfOtherAssistanceMeasure(true),
        ),
        DELETE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfOtherAssistanceMeasure(false),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfOtherAssistanceMeasure(true),
        ),
        READ(
            HasGlobalRole(ADMIN),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfOtherAssistanceMeasure(false),
            HasUnitRole(STAFF, UNIT_SUPERVISOR).inPlacementUnitOfChildOfOtherAssistanceMeasure(true),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class Pairing(override vararg val defaultRules: ScopedActionRule<in PairingId>) :
        ScopedAction<PairingId> {
        POST_RESPONSE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnitOfPairing(),
            IsEmployee.ownerOfPairing(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class Parentship(override vararg val defaultRules: ScopedActionRule<in ParentshipId>) :
        ScopedAction<ParentshipId> {
        DELETE(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        DELETE_CONFLICTED_PARENTSHIP(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfParentship(),
        ),
        RETRY(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfParentship(),
        ),
        UPDATE(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfParentship(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class Partnership(override vararg val defaultRules: ScopedActionRule<in PartnershipId>) :
        ScopedAction<PartnershipId> {
        DELETE(HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN)),
        RETRY(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPartnership(),
        ),
        UPDATE(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPartnership(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class Payment(override vararg val defaultRules: ScopedActionRule<in PaymentId>) :
        ScopedAction<PaymentId> {
        SEND(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        DELETE(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        CONFIRM(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        REVERT_TO_DRAFT(HasGlobalRole(ADMIN, FINANCE_ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class PedagogicalDocument(
        override vararg val defaultRules: ScopedActionRule<in PedagogicalDocumentId>
    ) : ScopedAction<PedagogicalDocumentId> {
        CREATE_ATTACHMENT(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementUnitOfChildOfPedagogicalDocument(),
        ),
        DELETE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementUnitOfChildOfPedagogicalDocument(),
        ),
        UPDATE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementUnitOfChildOfPedagogicalDocument(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class Person(override vararg val defaultRules: ScopedActionRule<in PersonId>) :
        ScopedAction<PersonId> {
        ADD_SSN(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        CREATE_FINANCE_NOTE(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        CREATE_FOSTER_PARENT_RELATIONSHIP(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        CREATE_INCOME(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        CREATE_INVOICE_CORRECTION(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        CREATE_PARENTSHIP(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPerson(),
        ),
        CREATE_PARTNERSHIP(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPerson(),
        ),
        CREATE_REPLACEMENT_DRAFT_INVOICES(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        DELETE(HasGlobalRole(ADMIN)),
        DOWNLOAD_ADDRESS_PAGE(HasGlobalRole(ADMIN, FINANCE_ADMIN, SERVICE_WORKER)),
        DISABLE_SSN_ADDING(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        ENABLE_SSN_ADDING(HasGlobalRole(ADMIN)),
        GENERATE_RETROACTIVE_FEE_DECISIONS(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        GENERATE_RETROACTIVE_VOUCHER_VALUE_DECISIONS(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        MERGE(HasGlobalRole(ADMIN)),
        READ_APPLICATIONS(
            HasGlobalRole(ADMIN, SERVICE_WORKER)
        ), // Applications summary on person page
        READ_CHILD_PLACEMENT_PERIODS(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        READ_DECISIONS(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inAnyUnit(),
        ),
        READ_FAMILY_OVERVIEW(
            HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inPlacementUnitOfChildOfPerson(),
        ),
        READ_FEE_DECISIONS(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        READ_FINANCE_NOTES(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        READ_FOSTER_CHILDREN(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, STAFF).inPlacementUnitOfChild(),
        ),
        READ_FOSTER_PARENTS(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, STAFF).inPlacementUnitOfChild(),
        ),
        READ_INCOME(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        READ_INCOME_STATEMENTS(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        READ_INCOME_NOTIFICATIONS(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        READ_INVOICES(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        READ_INVOICE_ADDRESS(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        READ_INVOICE_CORRECTIONS(HasGlobalRole(ADMIN, FINANCE_ADMIN, FINANCE_STAFF)),
        READ_OPH_OID(
            HasGlobalRole(ADMIN, DIRECTOR),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inPlacementUnitOfChildOfPerson(),
        ),
        READ_PARENTSHIPS(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, FINANCE_STAFF),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inPlacementUnitOfChildOfPerson(),
        ),
        READ_PARTNERSHIPS(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, FINANCE_STAFF),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inPlacementUnitOfChildOfPerson(),
        ),
        READ(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, FINANCE_STAFF),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inPlacementUnitOfChildOfPerson(),
        ),
        READ_DEPENDANTS(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inPlacementUnitOfChildOfPerson(),
        ),
        READ_TIMELINE(HasGlobalRole(ADMIN), HasGlobalRole(FINANCE_ADMIN)),
        READ_VOUCHER_VALUE_DECISIONS(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        UPDATE(
            HasGlobalRole(ADMIN, FINANCE_ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, STAFF).inPlacementUnitOfChildOfPerson(),
        ),
        UPDATE_EVAKA_RIGHTS(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        UPDATE_INVOICE_ADDRESS(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        UPDATE_OPH_OID(HasGlobalRole(ADMIN)),
        UPDATE_PERSONAL_DETAILS(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        UPDATE_FROM_VTJ(HasGlobalRole(ADMIN, SERVICE_WORKER));

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class Placement(override vararg val defaultRules: ScopedActionRule<in PlacementId>) :
        ScopedAction<PlacementId> {
        UPDATE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inPlacementUnitOfChildOfPlacement(),
        ),
        DELETE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inPlacementUnitOfChildOfFuturePlacement(),
        ),
        CREATE_GROUP_PLACEMENT(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inPlacementUnitOfChildOfPlacement(),
        ),
        CREATE_SERVICE_NEED(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inPlacementUnitOfChildOfPlacement(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class PreschoolAssistance(
        override vararg val defaultRules: ScopedActionRule<in PreschoolAssistanceId>
    ) : ScopedAction<PreschoolAssistanceId> {
        UPDATE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfPreschoolAssistance(false),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPreschoolAssistance(true),
        ),
        DELETE(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfPreschoolAssistance(false),
            HasUnitRole(UNIT_SUPERVISOR).inPlacementUnitOfChildOfPreschoolAssistance(true),
        ),
        READ(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER)
                .inPlacementUnitOfChildOfPreschoolAssistance(false),
            HasUnitRole(STAFF, UNIT_SUPERVISOR).inPlacementUnitOfChildOfPreschoolAssistance(true),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class PreschoolTerm(
        override vararg val defaultRules: ScopedActionRule<in PreschoolTermId>
    ) : ScopedAction<PreschoolTermId> {
        UPDATE(HasGlobalRole(ADMIN)),
        DELETE(HasGlobalRole(ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class ServiceNeed(override vararg val defaultRules: ScopedActionRule<in ServiceNeedId>) :
        ScopedAction<ServiceNeedId> {
        UPDATE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .inPlacementUnitOfChildOfServiceNeed(),
        ),
        DELETE(
            HasGlobalRole(ADMIN),
            HasUnitRole(EARLY_CHILDHOOD_EDUCATION_SECRETARY).inPlacementUnitOfChildOfServiceNeed(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class ServiceApplication(
        override vararg val defaultRules: ScopedActionRule<in ServiceApplicationId>
    ) : ScopedAction<ServiceApplicationId> {
        ACCEPT(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR)
                .withUnitFeatures(PilotFeature.SERVICE_APPLICATIONS)
                .inPlacementUnitOfChildOfServiceApplication(),
        ),
        REJECT(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR)
                .withUnitFeatures(PilotFeature.SERVICE_APPLICATIONS)
                .inPlacementUnitOfChildOfServiceApplication(),
        );

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class Unit(override vararg val defaultRules: ScopedActionRule<in DaycareId>) :
        ScopedAction<DaycareId> {
        READ(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, DIRECTOR, REPORT_VIEWER),
            HasGlobalRole(FINANCE_STAFF)
                .andUnitProviderAndCareTypeEquals(
                    setOf(ProviderType.MUNICIPAL, ProviderType.PURCHASED),
                    setOf(CareType.CENTRE, CareType.FAMILY, CareType.GROUP_FAMILY),
                ),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inUnit(),
        ),
        READ_SERVICE_WORKER_NOTE(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        SET_SERVICE_WORKER_NOTE(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        READ_GROUP_DETAILS(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, FINANCE_STAFF),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inUnit(),
        ),
        READ_ATTENDANCES(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, FINANCE_STAFF),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inUnit(),
        ), // marker for ui to toggle attendances-tab
        READ_APPLICATIONS_AND_PLACEMENT_PLANS(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR).inUnit(),
        ),
        READ_ABSENCE_APPLICATIONS(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF).inUnit(),
        ),
        READ_TRANSFER_APPLICATIONS(HasGlobalRole(ADMIN, SERVICE_WORKER)),
        READ_SERVICE_APPLICATIONS(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR)
                .withUnitFeatures(PilotFeature.SERVICE_APPLICATIONS)
                .inUnit(),
        ),
        READ_GROUPS(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, FINANCE_STAFF),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inUnit(),
        ),
        READ_CHILD_CAPACITY_FACTORS(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inUnit(),
        ),
        UPDATE(HasGlobalRole(ADMIN)),
        READ_CHILD_ATTENDANCES(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    STAFF,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inUnit(),
            IsMobile(requirePinLogin = false).inUnit(),
        ),
        READ_CHILD_RESERVATIONS(IsMobile(requirePinLogin = false).inUnit()),
        READ_UNIT_RESERVATION_STATISTICS(IsMobile(requirePinLogin = false).inUnit()),
        UPDATE_CHILD_ATTENDANCES(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, STAFF).inUnit(),
            IsMobile(requirePinLogin = false).inUnit(),
        ),
        READ_STAFF_ATTENDANCES(
            HasGlobalRole(ADMIN),
            IsMobile(requirePinLogin = false).inUnit(),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    SPECIAL_EDUCATION_TEACHER,
                    STAFF,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inUnit(),
        ),
        UPDATE_STAFF_ATTENDANCES(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inUnit(),
            IsMobile(requirePinLogin = false).inUnit(),
        ),
        READ_REALTIME_STAFF_ATTENDANCES(IsMobile(requirePinLogin = false).inUnit()),
        READ_STAFF_OCCUPANCY_COEFFICIENTS(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inUnit(),
        ),
        UPSERT_STAFF_OCCUPANCY_COEFFICIENTS(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR).inUnit(),
        ),
        READ_OCCUPANCIES(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, DIRECTOR, FINANCE_STAFF),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inUnit(),
            IsMobile(requirePinLogin = false).inUnit(),
        ),
        READ_ATTENDANCE_RESERVATIONS(
            HasGlobalRole(ADMIN),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    SPECIAL_EDUCATION_TEACHER,
                    STAFF,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inUnit(),
        ),
        CREATE_PLACEMENT(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit(),
        ),
        ACCEPT_PLACEMENT_PROPOSAL(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR).inUnit(),
        ),
        CREATE_GROUP(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit(),
        ),
        READ_MOBILE_STATS(IsMobile(requirePinLogin = false).inUnit()),
        READ_MOBILE_INFO(IsMobile(requirePinLogin = false).inUnit()),
        READ_MOBILE_DEVICES(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit(),
        ),
        CREATE_MOBILE_DEVICE_PAIRING(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit(),
        ),
        READ_ACL(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit(),
        ),
        INSERT_ACL_UNIT_SUPERVISOR(HasGlobalRole(ADMIN)),
        DELETE_ACL_UNIT_SUPERVISOR(HasGlobalRole(ADMIN)),
        INSERT_ACL_SPECIAL_EDUCATION_TEACHER(HasGlobalRole(ADMIN)),
        DELETE_ACL_SPECIAL_EDUCATION_TEACHER(HasGlobalRole(ADMIN)),
        INSERT_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY(HasGlobalRole(ADMIN)),
        DELETE_ACL_EARLY_CHILDHOOD_EDUCATION_SECRETARY(HasGlobalRole(ADMIN)),
        INSERT_ACL_STAFF(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                .withUnitProviderTypes(ProviderType.MUNICIPAL, ProviderType.MUNICIPAL_SCHOOL)
                .inUnit(),
        ),
        DELETE_ACL_STAFF(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit(),
        ),
        DELETE_ACL_SCHEDULED(HasGlobalRole(ADMIN), HasUnitRole(UNIT_SUPERVISOR).inUnit()),
        UPDATE_STAFF_GROUP_ACL(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit(),
        ),
        READ_APPLICATIONS_REPORT(
            HasGlobalRole(ADMIN, SERVICE_WORKER, DIRECTOR, REPORT_VIEWER),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit(),
        ),
        READ_PLACEMENT_GUARANTEE_REPORT(
            HasGlobalRole(ADMIN, SERVICE_WORKER, DIRECTOR),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit(),
        ),
        READ_ASSISTANCE_NEEDS_AND_ACTIONS_REPORT(
            HasGlobalRole(ADMIN, SERVICE_WORKER, DIRECTOR, REPORT_VIEWER),
            HasUnitRole(
                    UNIT_SUPERVISOR,
                    SPECIAL_EDUCATION_TEACHER,
                    EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                )
                .inUnit(),
        ),
        READ_ASSISTANCE_NEEDS_AND_ACTIONS_REPORT_BY_CHILD(
            HasGlobalRole(ADMIN, REPORT_VIEWER),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER).inUnit(),
        ),
        READ_ATTENDANCE_RESERVATION_REPORT(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit(),
        ),
        READ_CHILD_AGE_AND_LANGUAGE_REPORT(
            HasGlobalRole(ADMIN, SERVICE_WORKER, DIRECTOR, REPORT_VIEWER),
            HasUnitRole(SPECIAL_EDUCATION_TEACHER).inUnit(),
        ),
        READ_CHILD_IN_DIFFERENT_ADDRESS_REPORT(HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN)),
        READ_EXCEEDED_SERVICE_NEEDS_REPORT(HasGlobalRole(ADMIN)),
        READ_FAMILY_CONFLICT_REPORT(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit(),
        ),
        READ_FAMILY_CONTACT_REPORT(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit(),
        ),
        READ_FAMILY_DAYCARE_MEAL_REPORT(
            HasGlobalRole(ADMIN, DIRECTOR),
            HasUnitRole(UNIT_SUPERVISOR).inUnit(),
        ),
        READ_MISSING_SERVICE_NEED_REPORT(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN, DIRECTOR),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit(),
        ),
        READ_OCCUPANCY_REPORT(
            HasGlobalRole(ADMIN, SERVICE_WORKER, DIRECTOR, REPORT_VIEWER),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit(),
        ),
        READ_PARTNERS_IN_DIFFERENT_ADDRESS_REPORT(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN)
        ),
        READ_SERVICE_NEED_REPORT(
            HasGlobalRole(ADMIN, SERVICE_WORKER, DIRECTOR, REPORT_VIEWER),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit(),
        ),
        READ_NEKKU_ORDER_REPORT(HasGlobalRole(ADMIN), HasUnitRole(UNIT_SUPERVISOR, STAFF).inUnit()),
        READ_SERVICE_VOUCHER_REPORT(
            HasGlobalRole(ADMIN, DIRECTOR, REPORT_VIEWER, FINANCE_ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit(),
        ),
        READ_SERVICE_VOUCHER_VALUES_REPORT(
            HasGlobalRole(ADMIN, FINANCE_ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit(),
        ),
        UPDATE_FEATURES(HasGlobalRole(ADMIN)),
        READ_UNREAD_MESSAGES(IsMobile(requirePinLogin = false).inUnit()),
        READ_TERMINATED_PLACEMENTS(HasGlobalRole(ADMIN), HasUnitRole(UNIT_SUPERVISOR).inUnit()),
        READ_MISSING_GROUP_PLACEMENTS(
            HasGlobalRole(ADMIN, SERVICE_WORKER),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit(),
        ),
        READ_CALENDAR_EVENTS(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inUnit(),
        ),
        CREATE_CALENDAR_EVENT(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER, STAFF).inUnit(),
        ),
        CREATE_TEMPORARY_EMPLOYEE(HasGlobalRole(ADMIN), HasUnitRole(UNIT_SUPERVISOR).inUnit()),
        READ_TEMPORARY_EMPLOYEE(HasGlobalRole(ADMIN), HasUnitRole(UNIT_SUPERVISOR).inUnit()),
        UPDATE_TEMPORARY_EMPLOYEE(HasGlobalRole(ADMIN), HasUnitRole(UNIT_SUPERVISOR).inUnit()),
        DELETE_TEMPORARY_EMPLOYEE(HasGlobalRole(ADMIN), HasUnitRole(UNIT_SUPERVISOR).inUnit()),
        READ_MEAL_REPORT(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit(),
        ),
        READ_PRESCHOOL_ABSENCE_REPORT(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, STAFF).inUnit(),
        ),
        READ_CHILD_DOCUMENTS_REPORT(
            HasGlobalRole(ADMIN, DIRECTOR),
            HasUnitRole(UNIT_SUPERVISOR).inUnit(),
        ),
        READ_PRESCHOOL_APPLICATION_REPORT,
        READ_HOLIDAY_PERIOD_ATTENDANCE_REPORT(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR).withUnitFeatures(PilotFeature.RESERVATIONS).inUnit(),
        ),
        READ_HOLIDAY_QUESTIONNAIRE_REPORT(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR).inUnit(),
        ),
        READ_STARTING_PLACEMENTS_REPORT(
            HasGlobalRole(ADMIN, SERVICE_WORKER, FINANCE_ADMIN),
            HasUnitRole(UNIT_SUPERVISOR).inUnit(),
        ),
        READ_STAFF_EMPLOYEE_NUMBER(HasGlobalRole(ADMIN), HasUnitRole(UNIT_SUPERVISOR).inUnit());

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class ChildDocument(
        override vararg val defaultRules: ScopedActionRule<in ChildDocumentId>
    ) : ScopedAction<ChildDocumentId> {
        READ(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementUnitOfChildOfChildDocument(),
            HasGroupRole(STAFF)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementGroupOfChildOfChildDocument(),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementUnitOfDuplicateChildOfHojksChildDocument(),
            HasGroupRole(STAFF)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementGroupOfDuplicateChildOfHojksChildDocument(),
            IsEmployee.andIsDecisionMakerForChildDocumentDecision(),
        ),
        READ_METADATA(HasGlobalRole(ADMIN)),
        READ_ACCEPTED_DECISIONS(IsEmployee.andIsDecisionMakerForChildDocumentDecision()),
        DOWNLOAD(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementUnitOfChildOfChildDocument(),
            HasGroupRole(STAFF)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementGroupOfChildOfChildDocument(),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementUnitOfDuplicateChildOfHojksChildDocument(),
            HasGroupRole(STAFF)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementGroupOfDuplicateChildOfHojksChildDocument(),
            IsEmployee.andIsDecisionMakerForChildDocumentDecision(),
        ),
        UPDATE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementUnitOfChildOfChildDocument(editable = true),
            HasGroupRole(STAFF)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementGroupOfChildOfChildDocument(editable = true),
        ),
        PUBLISH(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementUnitOfChildOfChildDocument(publishable = true),
            HasGroupRole(STAFF)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementGroupOfChildOfChildDocument(publishable = true),
        ),
        NEXT_STATUS(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementUnitOfChildOfChildDocument(),
            HasGroupRole(STAFF)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementGroupOfChildOfChildDocument(),
        ),
        PREV_STATUS(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementUnitOfChildOfChildDocument(canGoToPrevStatus = true),
            HasGroupRole(STAFF).inPlacementGroupOfChildOfChildDocument(canGoToPrevStatus = true),
            IsEmployee.andIsDecisionMakerForChildDocumentDecision(),
        ),
        DELETE(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementUnitOfChildOfChildDocument(deletable = true),
            HasGroupRole(STAFF)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementGroupOfChildOfChildDocument(deletable = true),
        ),
        PROPOSE_DECISION(
            HasGlobalRole(ADMIN),
            HasUnitRole(UNIT_SUPERVISOR, SPECIAL_EDUCATION_TEACHER)
                .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                .inPlacementUnitOfChildOfChildDocument(),
        ),
        ACCEPT_DECISION(IsEmployee.andIsDecisionMakerForChildDocumentDecision()),
        REJECT_DECISION(IsEmployee.andIsDecisionMakerForChildDocumentDecision()),
        ANNUL_DECISION(
            HasGlobalRole(ADMIN),
            IsEmployee.andIsDecisionMakerForChildDocumentDecision(),
        ),
        UPDATE_DECISION_VALIDITY(HasGlobalRole(ADMIN)),
        ARCHIVE(HasGlobalRole(ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }

    enum class VoucherValueDecision(
        override vararg val defaultRules: ScopedActionRule<in VoucherValueDecisionId>
    ) : ScopedAction<VoucherValueDecisionId> {
        READ(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        READ_METADATA(HasGlobalRole(ADMIN)),
        UPDATE(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        IGNORE(HasGlobalRole(ADMIN, FINANCE_ADMIN)),
        UNIGNORE(HasGlobalRole(ADMIN, FINANCE_ADMIN));

        override fun toString(): String = "${javaClass.name}.$name"
    }
}
