// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.bi

enum class BiTable(val fileName: String, val query: BiQueries.CsvQuery) {
    Absence("absence_DELTA", BiQueries.getAbsencesDelta),
    Application("application", BiQueries.getApplications),
    ApplicationForm("application_form", BiQueries.getApplicationForms),
    AssistanceAction("assistance_action", BiQueries.getAssistanceActions),
    AssistanceActionOption("assistance_action_option", BiQueries.getAssistanceActionOptions),
    AssistanceActionOptionRef(
        "assistance_action_option_ref",
        BiQueries.getAssistanceActionOptionRefs,
    ),
    AssistanceFactor("assistance_factor", BiQueries.getAssistanceFactors),
    AssistanceNeedVoucherCoefficient(
        "assistance_need_voucher_coefficient",
        BiQueries.getAssistanceNeedVoucherCoefficients,
    ),
    AttendanceReservation("attendance_reservation", BiQueries.getAttendanceReservations),
    BackupCare("backup_care", BiQueries.getBackupCares),
    CareArea("care_area", BiQueries.getAreas),
    Child("child", BiQueries.getChildren),
    ChildAttendance("child_attendance_DELTA", BiQueries.getChildAttendanceDelta),
    Daycare("daycare", BiQueries.getDaycares),
    DaycareAssistance("daycare_assistance", BiQueries.getDaycareAssistances),
    DaycareCaretaker("daycare_caretaker", BiQueries.getDaycareCaretakers),
    DaycareGroup("daycare_group", BiQueries.getDaycareGroups),
    DaycareGroupPlacement("daycare_group_placement", BiQueries.getDaycareGroupPlacements),
    Decision("decision", BiQueries.getDecisions),
    Employee("employee", BiQueries.getEmployees),
    EvakaUser("evaka_user", BiQueries.getEvakaUsers),
    FeeAlteration("fee_alteration", BiQueries.getFeeAlterations),
    FeeDecision("fee_decision", BiQueries.getFeeDecisions),
    FeeDecisionChild("fee_decision_child", BiQueries.getFeeDecisionChildren),
    FeeThresholds("fee_thresholds", BiQueries.getFeeThresholds),
    FridgeChild("fridge_child", BiQueries.getFridgeChildren),
    FridgePartner("fridge_partner", BiQueries.getFridgePartners),
    Guardian("guardian", BiQueries.getGuardians),
    GuardianBlocklist("guardian_blocklist", BiQueries.getGuardianBlockLists),
    HolidayPeriod("holiday_period", BiQueries.getHolidayPeriods),
    HolidayPeriodQuestionnaireAnswer(
        "holiday_period_questionnaire_answer",
        BiQueries.getHolidayQuestionnaireAnswers,
    ),
    Income("income", BiQueries.getIncomes),
    OtherAssistanceMeasure("other_assistance_measure", BiQueries.getOtherAssistanceMeasures),
    Person("person", BiQueries.getPersons),
    Placement("placement", BiQueries.getPlacements),
    PreschoolAssistance("preschool_assistance", BiQueries.getPreschoolAssistances),
    ServiceNeed("service_need", BiQueries.getServiceNeeds),
    ServiceNeedOption("service_need_option", BiQueries.getServiceNeedOptions),
    ServiceNeedOptionVoucherValue(
        "service_need_option_voucher_value",
        BiQueries.getServiceNeedOptionVoucherValues,
    ),
    StaffAttendance("staff_attendance", BiQueries.getStaffAttendance),
    StaffAttendanceExternal("staff_attendance_external", BiQueries.getStaffAttendanceExternals),
    StaffAttendancePlan("staff_attendance_plan", BiQueries.getStaffAttendancePlans),
    StaffAttendanceRealtime(
        "staff_attendance_realtime_DELTA",
        query = BiQueries.getStaffAttendanceRealtimeDelta,
    ),
    StaffOccupancyCoefficient(
        "staff_occupancy_coefficient",
        BiQueries.getStaffOccupancyCoefficients,
    ),
    VoucherValueDecision("voucher_value_decision", BiQueries.getVoucherValueDecisions),
}
