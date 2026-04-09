// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka.security

import evaka.core.application.ApplicationType
import evaka.core.daycare.domain.ProviderType
import evaka.core.shared.auth.UserRole
import evaka.core.shared.security.Action
import evaka.core.shared.security.PilotFeature
import evaka.core.shared.security.actionrule.ActionRuleMapping
import evaka.core.shared.security.actionrule.HasGlobalRole
import evaka.core.shared.security.actionrule.HasGroupRole
import evaka.core.shared.security.actionrule.HasUnitRole
import evaka.core.shared.security.actionrule.IsEmployee
import evaka.core.shared.security.actionrule.ScopedActionRule
import evaka.core.shared.security.actionrule.UnscopedActionRule

class TrevakaActionRuleMapping : ActionRuleMapping {
    override fun rulesOf(action: Action.UnscopedAction): Sequence<UnscopedActionRule> =
        when (action) {
            Action.Global.APPLICATIONS_PAGE,
            Action.Global.FINANCE_PAGE,
            Action.Global.PERSON_SEARCH_PAGE,
            Action.Global.FETCH_INCOME_STATEMENTS_AWAITING_HANDLER,
            Action.Global.READ_SERVICE_WORKER_APPLICATION_NOTES,
            Action.Global.SEARCH_PEOPLE,
            Action.Global.SEARCH_PEOPLE_UNRESTRICTED,
            Action.Global.READ_FEE_THRESHOLDS,
            Action.Global.SEARCH_FEE_DECISIONS,
            Action.Global.SEARCH_VOUCHER_VALUE_DECISIONS,
            Action.Global.READ_FINANCE_DECISION_HANDLERS,
            Action.Global.SEARCH_INVOICES,
            Action.Global.READ_UNITS,
            Action.Global.READ_DECISION_UNITS,
            Action.Global.READ_ENDED_PLACEMENTS_REPORT,
            Action.Global.READ_INVOICE_REPORT,
            Action.Global.READ_INCOME_TYPES,
            Action.Global.READ_INVOICE_CODES,
            Action.Global.READ_EMPLOYEES -> {
                action.defaultRules.asSequence() + sequenceOf(HasGlobalRole(UserRole.DIRECTOR))
            }

            Action.Global.READ_HOLIDAY_PERIODS -> {
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasUnitRole(UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY).inAnyUnit()
                    )
            }

            Action.Global.READ_MISSING_HEAD_OF_FAMILY_REPORT -> {
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasGlobalRole(
                            UserRole.DIRECTOR,
                            UserRole.FINANCE_ADMIN,
                            UserRole.SERVICE_WORKER,
                        )
                    )
            }

            Action.Global.READ_PLACEMENT_COUNT_REPORT -> {
                action.defaultRules.asSequence() + sequenceOf(HasGlobalRole(UserRole.REPORT_VIEWER))
            }

            Action.Global.READ_PLACEMENT_SKETCHING_REPORT -> {
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR)) +
                    sequenceOf(
                        HasUnitRole(UserRole.UNIT_SUPERVISOR)
                            .withUnitProviderTypes(
                                ProviderType.MUNICIPAL,
                                ProviderType.PURCHASED,
                                ProviderType.PRIVATE,
                                ProviderType.MUNICIPAL_SCHOOL,
                                ProviderType.EXTERNAL_PURCHASED,
                            )
                            .inAnyUnit()
                    ) +
                    sequenceOf(
                        HasUnitRole(UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY).inAnyUnit()
                    )
            }

            Action.Global.PIN_CODE_PAGE -> sequenceOf(IsEmployee.any())

            Action.Global.CREATE_PERSONAL_MOBILE_DEVICE_PAIRING,
            Action.Global.PERSONAL_MOBILE_DEVICE_PAGE -> {
                action.defaultRules.asSequence() +
                    sequenceOf(HasUnitRole(UserRole.SPECIAL_EDUCATION_TEACHER).inAnyUnit())
            }

            Action.Global.SEND_PATU_REPORT,
            Action.Global.SUBMIT_PATU_REPORT -> emptySequence()

            Action.Global.SETTINGS_PAGE,
            Action.Global.UPDATE_SETTINGS -> emptySequence()

            Action.Global.READ_TAMPERE_REGIONAL_SURVEY_REPORT ->
                sequenceOf(HasGlobalRole(UserRole.ADMIN, UserRole.REPORT_VIEWER))

            else -> action.defaultRules.asSequence()
        }

    override fun <T> rulesOf(action: Action.ScopedAction<in T>): Sequence<ScopedActionRule<in T>> =
        when (action) {
            Action.Application.READ -> {
                @Suppress("UNCHECKED_CAST")
                sequenceOf(
                    HasGlobalRole(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.DIRECTOR)
                        as ScopedActionRule<in T>
                ) +
                    sequenceOf(
                        HasUnitRole(UserRole.UNIT_SUPERVISOR)
                            .inPlacementPlanUnitOfApplication(
                                onlyAllowDeletedForTypes = setOf(ApplicationType.PRESCHOOL)
                            ) as ScopedActionRule<in T>
                    )
            }

            Action.Application.READ_IF_HAS_ASSISTANCE_NEED,
            Action.Application.READ_PLACEMENT_PLAN_DRAFT,
            Action.Application.READ_DECISION_DRAFT,
            Action.Application.READ_NOTES -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>)
            }

            Action.Attachment.READ_APPLICATION_ATTACHMENT,
            Action.Attachment.READ_INCOME_STATEMENT_ATTACHMENT,
            Action.Attachment.READ_INCOME_ATTACHMENT -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>)
            }

            Action.Attachment.READ_PEDAGOGICAL_DOCUMENT_ATTACHMENT,
            Action.Attachment.DELETE_PEDAGOGICAL_DOCUMENT_ATTACHMENT -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasUnitRole(UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                            .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                            .inPlacementUnitOfChildOfPedagogicalDocumentOfAttachment()
                            as ScopedActionRule<in T>
                    )
            }

            Action.BackupCare.UPDATE,
            Action.BackupCare.DELETE -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasUnitRole(UserRole.STAFF).inPlacementUnitOfChildOfBackupCare()
                            as ScopedActionRule<in T>
                    )
            }

            Action.Child.READ,
            Action.Child.READ_ABSENCES,
            Action.Child.READ_FUTURE_ABSENCES,
            Action.Child.READ_ADDITIONAL_INFO,
            Action.Child.READ_BACKUP_CARE,
            Action.Child.READ_PLACEMENT,
            Action.Child.READ_GUARDIANS,
            Action.Child.READ_FEE_ALTERATIONS,
            Action.Child.READ_CHILD_DOCUMENT -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>)
            }

            Action.Child.READ_APPLICATION -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>) +
                    sequenceOf(
                        HasUnitRole(UserRole.UNIT_SUPERVISOR).inPlacementUnitOfChild()
                            as ScopedActionRule<in T>
                    )
            }

            Action.Child.CREATE_BACKUP_CARE -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasUnitRole(UserRole.STAFF).inPlacementUnitOfChild()
                            as ScopedActionRule<in T>
                    )
            }

            Action.Child.READ_ASSISTANCE,
            Action.Child.READ_ASSISTANCE_FACTORS -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasGlobalRole(UserRole.DIRECTOR, UserRole.FINANCE_ADMIN)
                            as ScopedActionRule<in T>
                    )
            }

            Action.AssistanceFactor.READ -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasGlobalRole(UserRole.DIRECTOR, UserRole.FINANCE_ADMIN)
                            as ScopedActionRule<in T>
                    ) +
                    sequenceOf(
                        HasUnitRole(UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                            .inPlacementUnitOfChildOfAssistanceFactor(true)
                            as ScopedActionRule<in T>
                    )
            }

            Action.Child.READ_DAYCARE_ASSISTANCES,
            Action.Child.READ_PRESCHOOL_ASSISTANCES,
            Action.Child.READ_ASSISTANCE_ACTION,
            Action.Child.READ_OTHER_ASSISTANCE_MEASURES -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>)
            }

            Action.DaycareAssistance.READ -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>) +
                    sequenceOf(
                        HasUnitRole(UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                            .inPlacementUnitOfChildOfDaycareAssistance(true)
                            as ScopedActionRule<in T>
                    )
            }

            Action.PreschoolAssistance.READ -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>) +
                    sequenceOf(
                        HasUnitRole(UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                            .inPlacementUnitOfChildOfPreschoolAssistance(true)
                            as ScopedActionRule<in T>
                    )
            }

            Action.AssistanceAction.READ -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>) +
                    sequenceOf(
                        HasUnitRole(UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                            .inPlacementUnitOfChildOfAssistanceAction(true)
                            as ScopedActionRule<in T>
                    )
            }

            Action.OtherAssistanceMeasure.READ -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>) +
                    sequenceOf(
                        HasUnitRole(UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                            .inPlacementUnitOfChildOfOtherAssistanceMeasure(true)
                            as ScopedActionRule<in T>
                    )
            }

            Action.Child.READ_PEDAGOGICAL_DOCUMENTS,
            Action.Child.CREATE_PEDAGOGICAL_DOCUMENT -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasUnitRole(UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                            .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                            .inPlacementUnitOfChild() as ScopedActionRule<in T>
                    )
            }

            Action.Child.CREATE_ABSENCE,
            Action.Child.DELETE_ABSENCE -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.FINANCE_ADMIN) as ScopedActionRule<in T>) +
                    sequenceOf(
                        HasUnitRole(UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                            .inPlacementUnitOfChild() as ScopedActionRule<in T>
                    )
            }

            Action.Child.CREATE_ATTENDANCE_RESERVATION -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasUnitRole(UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                            .inPlacementUnitOfChild() as ScopedActionRule<in T>
                    )
            }

            Action.ChildDocument.READ -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>)
            }

            Action.ChildDocument.UPDATE -> {
                @Suppress("UNCHECKED_CAST")
                sequenceOf(HasGlobalRole(UserRole.ADMIN) as ScopedActionRule<in T>) +
                    sequenceOf(
                        HasUnitRole(UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER)
                            .inPlacementUnitOfChildOfChildDocument(editable = true)
                            as ScopedActionRule<in T>
                    ) +
                    sequenceOf(
                        HasGroupRole(UserRole.STAFF)
                            .inPlacementGroupOfChildOfChildDocumentWithFutureAccess(
                                editable = true,
                                denyForDecisions = false,
                            ) as ScopedActionRule<in T>
                    )
            }

            Action.Decision.DOWNLOAD_PDF -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>)
            }

            Action.FeeDecision.READ -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>)
            }

            Action.Group.CREATE_ABSENCES,
            Action.Group.DELETE_ABSENCES -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.FINANCE_ADMIN) as ScopedActionRule<in T>) +
                    sequenceOf(
                        HasUnitRole(UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnitOfGroup()
                            as ScopedActionRule<in T>
                    )
            }

            Action.Group.READ_STAFF_ATTENDANCES,
            Action.Group.READ_CARETAKERS -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>)
            }

            Action.Group.READ_ABSENCES -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>) +
                    sequenceOf(
                        HasUnitRole(UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnitOfGroup()
                            as ScopedActionRule<in T>
                    )
            }

            Action.Invoice.READ -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>)
            }

            Action.PedagogicalDocument.DELETE,
            Action.PedagogicalDocument.UPDATE,
            Action.PedagogicalDocument.CREATE_ATTACHMENT -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasUnitRole(UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                            .withUnitFeatures(PilotFeature.VASU_AND_PEDADOC)
                            .inPlacementUnitOfChildOfPedagogicalDocument() as ScopedActionRule<in T>
                    )
            }

            Action.Person.READ_APPLICATIONS,
            Action.Person.READ_CHILD_PLACEMENT_PERIODS,
            Action.Person.READ_DECISIONS,
            Action.Person.READ_FAMILY_OVERVIEW,
            Action.Person.READ_FEE_DECISIONS,
            Action.Person.READ_INCOME,
            Action.Person.READ_INCOME_STATEMENTS,
            Action.Person.READ_INVOICES,
            Action.Person.READ_INVOICE_ADDRESS,
            Action.Person.READ_INVOICE_CORRECTIONS,
            Action.Person.READ_PARENTSHIPS,
            Action.Person.READ_PARTNERSHIPS,
            Action.Person.READ,
            Action.Person.READ_DEPENDANTS,
            Action.Person.READ_VOUCHER_VALUE_DECISIONS -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>)
            }

            Action.Person.READ_FOSTER_CHILDREN,
            Action.Person.READ_FOSTER_PARENTS -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.FINANCE_ADMIN) as ScopedActionRule<in T>)
            }

            Action.Placement.CREATE_SERVICE_NEED -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasGlobalRole(UserRole.FINANCE_ADMIN, UserRole.SERVICE_WORKER)
                            as ScopedActionRule<in T>
                    )
            }

            Action.ServiceNeed.UPDATE,
            Action.ServiceNeed.DELETE -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasGlobalRole(UserRole.FINANCE_ADMIN, UserRole.SERVICE_WORKER)
                            as ScopedActionRule<in T>
                    )
            }

            Action.Unit.READ_APPLICATIONS_AND_PLACEMENT_PLANS,
            Action.Unit.READ_ATTENDANCES,
            Action.Unit.READ_GROUP_DETAILS,
            Action.Unit.READ_GROUPS,
            Action.Unit.READ_CHILD_CAPACITY_FACTORS,
            Action.Unit.READ_APPLICATIONS_REPORT,
            Action.Unit.READ_CHILD_IN_DIFFERENT_ADDRESS_REPORT,
            Action.Unit.READ_FAMILY_CONFLICT_REPORT,
            Action.Unit.READ_PARTNERS_IN_DIFFERENT_ADDRESS_REPORT,
            Action.Unit.READ_SERVICE_VOUCHER_VALUES_REPORT,
            Action.Unit.READ_STARTING_PLACEMENTS_REPORT,
            Action.Unit.READ_MISSING_GROUP_PLACEMENTS -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>)
            }

            Action.Unit.READ_ATTENDANCE_RESERVATION_REPORT -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasGlobalRole(UserRole.REPORT_VIEWER, UserRole.DIRECTOR)
                            as ScopedActionRule<in T>
                    ) +
                    sequenceOf(HasUnitRole(UserRole.STAFF).inUnit() as ScopedActionRule<in T>)
            }

            Action.Unit.READ_CHILD_ATTENDANCES -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>)
            }

            Action.Unit.READ_STAFF_OCCUPANCY_COEFFICIENTS,
            Action.Unit.UPSERT_STAFF_OCCUPANCY_COEFFICIENTS,
            Action.Unit.UPDATE_STAFF_ATTENDANCES -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasUnitRole(UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit()
                            as ScopedActionRule<in T>
                    )
            }

            Action.Unit.READ_TEMPORARY_EMPLOYEE,
            Action.Unit.CREATE_TEMPORARY_EMPLOYEE,
            Action.Unit.UPDATE_TEMPORARY_EMPLOYEE,
            Action.Unit.DELETE_TEMPORARY_EMPLOYEE -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasUnitRole(UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit()
                            as ScopedActionRule<in T>
                    )
            }

            Action.Unit.READ_ASSISTANCE_NEEDS_AND_ACTIONS_REPORT_BY_CHILD -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasUnitRole(UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY).inUnit()
                            as ScopedActionRule<in T>
                    )
            }

            Action.Unit.READ_EXCEEDED_SERVICE_NEEDS_REPORT -> emptySequence()

            Action.Unit.READ_HOLIDAY_PERIOD_ATTENDANCE_REPORT -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasUnitRole(UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY)
                            .withUnitFeatures(PilotFeature.RESERVATIONS)
                            .inUnit() as ScopedActionRule<in T>
                    )
            }

            Action.Unit.READ_TRANSFER_APPLICATIONS -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(
                        HasUnitRole(UserRole.UNIT_SUPERVISOR).inUnit() as ScopedActionRule<in T>
                    )
            }

            Action.VoucherValueDecision.READ -> {
                @Suppress("UNCHECKED_CAST")
                action.defaultRules.asSequence() +
                    sequenceOf(HasGlobalRole(UserRole.DIRECTOR) as ScopedActionRule<in T>)
            }

            else -> action.defaultRules.asSequence()
        }
}
