// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

enum class Report {
    APPLICATIONS,
    ASSISTANCE_NEED_DECISIONS,
    ASSISTANCE_NEEDS_AND_ACTIONS,
    ASSISTANCE_NEEDS_AND_ACTIONS_BY_CHILD,
    ATTENDANCE_RESERVATION,
    CHILD_AGE_LANGUAGE,
    CHILD_DOCUMENT_DECISIONS,
    CHILD_DOCUMENTS,
    CHILDREN_IN_DIFFERENT_ADDRESS,
    CUSTOMER_FEES,
    DECISIONS,
    DUPLICATE_PEOPLE,
    ENDED_PLACEMENTS,
    EXCEEDED_SERVICE_NEEDS,
    FAMILY_CONFLICT,
    FAMILY_CONTACT,
    FAMILY_DAYCARE_MEAL_REPORT,
    HOLIDAY_PERIOD_ATTENDANCE,
    INVOICE,
    INCOMPLETE_INCOMES,
    MANUAL_DUPLICATION,
    MISSING_HEAD_OF_FAMILY,
    MISSING_SERVICE_NEED,
    NON_SSN_CHILDREN,
    OCCUPANCY,
    PARTNERS_IN_DIFFERENT_ADDRESS,
    PLACEMENT_COUNT,
    PLACEMENT_GUARANTEE,
    PLACEMENT_SKETCHING,
    PRESCHOOL_ABSENCES,
    PRESCHOOL_APPLICATIONS,
    PRESENCE,
    RAW,
    SERVICE_NEED,
    SERVICE_VOUCHER_VALUE,
    SEXTET,
    STARTING_PLACEMENTS,
    TITANIA_ERRORS,
    UNITS,
    VARDA_ERRORS,
    FUTURE_PRESCHOOLERS,
    MEALS,
    NEKKU_ORDERS,
    TAMPERE_REGIONAL_SURVEY,
    CITIZEN_DOCUMENT_RESPONSE,
}

@RestController
class ReportPermissions(private val accessControl: AccessControl) {
    @GetMapping("/employee/reports")
    fun getPermittedReports(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): Set<Report> =
        db.connect { dbc ->
            dbc.read { tx ->
                val permittedGlobalActions =
                    accessControl.getPermittedActions<Action.Global>(tx, user, clock)
                val permittedActionsForSomeUnit =
                    accessControl.getPermittedActionsForSomeTarget<Action.Unit>(tx, user, clock)
                val permittedActionsForSomeGroup =
                    accessControl.getPermittedActionsForSomeTarget<Action.Group>(tx, user, clock)
                setOfNotNull(
                    Report.APPLICATIONS.takeIf {
                        permittedActionsForSomeUnit.contains(Action.Unit.READ_APPLICATIONS_REPORT)
                    },
                    Report.ASSISTANCE_NEED_DECISIONS.takeIf {
                        accessControl.isPermittedForSomeTarget(
                            tx,
                            user,
                            clock,
                            Action.AssistanceNeedDecision.READ_IN_REPORT,
                        ) ||
                            accessControl.isPermittedForSomeTarget(
                                tx,
                                user,
                                clock,
                                Action.AssistanceNeedPreschoolDecision.READ_IN_REPORT,
                            )
                    },
                    Report.ASSISTANCE_NEEDS_AND_ACTIONS.takeIf {
                        permittedActionsForSomeUnit.contains(
                            Action.Unit.READ_ASSISTANCE_NEEDS_AND_ACTIONS_REPORT
                        )
                    },
                    Report.ASSISTANCE_NEEDS_AND_ACTIONS_BY_CHILD.takeIf {
                        permittedActionsForSomeUnit.contains(
                            Action.Unit.READ_ASSISTANCE_NEEDS_AND_ACTIONS_REPORT_BY_CHILD
                        )
                    },
                    Report.ATTENDANCE_RESERVATION.takeIf {
                        permittedActionsForSomeUnit.contains(
                            Action.Unit.READ_ATTENDANCE_RESERVATION_REPORT
                        )
                    },
                    Report.CHILD_AGE_LANGUAGE.takeIf {
                        permittedActionsForSomeUnit.contains(
                            Action.Unit.READ_CHILD_AGE_AND_LANGUAGE_REPORT
                        )
                    },
                    Report.CHILD_DOCUMENT_DECISIONS.takeIf {
                        permittedGlobalActions.contains(
                            Action.Global.READ_CHILD_DOCUMENT_DECISIONS_REPORT
                        )
                    },
                    Report.CHILD_DOCUMENTS.takeIf {
                        permittedActionsForSomeUnit.contains(
                            Action.Unit.READ_CHILD_DOCUMENTS_REPORT
                        )
                    },
                    Report.CHILDREN_IN_DIFFERENT_ADDRESS.takeIf {
                        permittedActionsForSomeUnit.contains(
                            Action.Unit.READ_CHILD_IN_DIFFERENT_ADDRESS_REPORT
                        )
                    },
                    Report.CUSTOMER_FEES.takeIf {
                        permittedGlobalActions.contains(Action.Global.READ_CUSTOMER_FEES_REPORT)
                    },
                    Report.DECISIONS.takeIf {
                        permittedGlobalActions.contains(Action.Global.READ_DECISIONS_REPORT)
                    },
                    Report.DUPLICATE_PEOPLE.takeIf {
                        permittedGlobalActions.contains(Action.Global.READ_DUPLICATE_PEOPLE_REPORT)
                    },
                    Report.ENDED_PLACEMENTS.takeIf {
                        permittedGlobalActions.contains(Action.Global.READ_ENDED_PLACEMENTS_REPORT)
                    },
                    Report.EXCEEDED_SERVICE_NEEDS.takeIf {
                        permittedActionsForSomeUnit.contains(
                            Action.Unit.READ_EXCEEDED_SERVICE_NEEDS_REPORT
                        )
                    },
                    Report.FAMILY_CONFLICT.takeIf {
                        permittedActionsForSomeUnit.contains(
                            Action.Unit.READ_FAMILY_CONFLICT_REPORT
                        )
                    },
                    Report.FAMILY_CONTACT.takeIf {
                        permittedActionsForSomeUnit.contains(Action.Unit.READ_FAMILY_CONTACT_REPORT)
                    },
                    Report.FAMILY_DAYCARE_MEAL_REPORT.takeIf {
                        permittedActionsForSomeUnit.contains(
                            Action.Unit.READ_FAMILY_DAYCARE_MEAL_REPORT
                        )
                    },
                    Report.INCOMPLETE_INCOMES.takeIf {
                        permittedGlobalActions.contains(
                            Action.Global.READ_INCOMPLETE_INCOMES_REPORT
                        )
                    },
                    Report.INVOICE.takeIf {
                        permittedGlobalActions.contains(Action.Global.READ_INVOICE_REPORT)
                    },
                    Report.MANUAL_DUPLICATION.takeIf {
                        permittedGlobalActions.contains(
                            Action.Global.READ_MANUAL_DUPLICATION_REPORT
                        )
                    },
                    Report.MISSING_HEAD_OF_FAMILY.takeIf {
                        permittedGlobalActions.contains(
                            Action.Global.READ_MISSING_HEAD_OF_FAMILY_REPORT
                        )
                    },
                    Report.MISSING_SERVICE_NEED.takeIf {
                        permittedActionsForSomeUnit.contains(
                            Action.Unit.READ_MISSING_SERVICE_NEED_REPORT
                        )
                    },
                    Report.NON_SSN_CHILDREN.takeIf {
                        permittedGlobalActions.contains(Action.Global.READ_NON_SSN_CHILDREN_REPORT)
                    },
                    Report.OCCUPANCY.takeIf {
                        permittedActionsForSomeUnit.contains(Action.Unit.READ_OCCUPANCY_REPORT)
                    },
                    Report.PARTNERS_IN_DIFFERENT_ADDRESS.takeIf {
                        permittedActionsForSomeUnit.contains(
                            Action.Unit.READ_PARTNERS_IN_DIFFERENT_ADDRESS_REPORT
                        )
                    },
                    Report.PLACEMENT_COUNT.takeIf {
                        permittedGlobalActions.contains(Action.Global.READ_PLACEMENT_COUNT_REPORT)
                    },
                    Report.PLACEMENT_GUARANTEE.takeIf {
                        permittedActionsForSomeUnit.contains(
                            Action.Unit.READ_PLACEMENT_GUARANTEE_REPORT
                        )
                    },
                    Report.PLACEMENT_SKETCHING.takeIf {
                        permittedGlobalActions.contains(
                            Action.Global.READ_PLACEMENT_SKETCHING_REPORT
                        )
                    },
                    Report.PRESENCE.takeIf {
                        permittedGlobalActions.contains(Action.Global.READ_PRESENCE_REPORT)
                    },
                    Report.RAW.takeIf {
                        permittedGlobalActions.contains(Action.Global.READ_RAW_REPORT)
                    },
                    Report.SERVICE_NEED.takeIf {
                        permittedActionsForSomeUnit.contains(Action.Unit.READ_SERVICE_NEED_REPORT)
                    },
                    Report.SERVICE_VOUCHER_VALUE.takeIf {
                        permittedActionsForSomeUnit.contains(
                            Action.Unit.READ_SERVICE_VOUCHER_REPORT
                        )
                    },
                    Report.SEXTET.takeIf {
                        permittedGlobalActions.contains(Action.Global.READ_SEXTET_REPORT)
                    },
                    Report.STARTING_PLACEMENTS.takeIf {
                        permittedActionsForSomeUnit.contains(
                            Action.Unit.READ_STARTING_PLACEMENTS_REPORT
                        )
                    },
                    Report.TITANIA_ERRORS.takeIf {
                        permittedGlobalActions.contains(Action.Global.READ_TITANIA_ERRORS)
                    },
                    Report.UNITS.takeIf {
                        permittedGlobalActions.contains(Action.Global.READ_UNITS_REPORT)
                    },
                    Report.VARDA_ERRORS.takeIf {
                        permittedGlobalActions.contains(Action.Global.READ_VARDA_REPORT)
                    },
                    Report.FUTURE_PRESCHOOLERS.takeIf {
                        permittedGlobalActions.contains(Action.Global.READ_FUTURE_PRESCHOOLERS)
                    },
                    Report.MEALS.takeIf {
                        permittedActionsForSomeUnit.contains(Action.Unit.READ_MEAL_REPORT)
                    },
                    Report.PRESCHOOL_ABSENCES.takeIf {
                        permittedActionsForSomeUnit.contains(
                            Action.Unit.READ_PRESCHOOL_ABSENCE_REPORT
                        )
                    },
                    Report.PRESCHOOL_APPLICATIONS.takeIf {
                        permittedActionsForSomeUnit.contains(
                            Action.Unit.READ_PRESCHOOL_APPLICATION_REPORT
                        )
                    },
                    Report.HOLIDAY_PERIOD_ATTENDANCE.takeIf {
                        permittedActionsForSomeUnit.contains(
                            Action.Unit.READ_HOLIDAY_PERIOD_ATTENDANCE_REPORT
                        )
                    },
                    Report.NEKKU_ORDERS.takeIf {
                        permittedActionsForSomeUnit.contains(Action.Unit.READ_NEKKU_ORDER_REPORT)
                    },
                    Report.TAMPERE_REGIONAL_SURVEY.takeIf {
                        permittedGlobalActions.contains(
                            Action.Global.READ_TAMPERE_REGIONAL_SURVEY_REPORT
                        )
                    },
                    Report.CITIZEN_DOCUMENT_RESPONSE.takeIf {
                        permittedActionsForSomeGroup.contains(
                            Action.Group.READ_CITIZEN_DOCUMENT_RESPONSE_REPORT
                        )
                    },
                )
            }
        }
}
