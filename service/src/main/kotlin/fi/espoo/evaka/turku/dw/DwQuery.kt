// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.dw

enum class DwQuery(val queryName: String, val query: DwQueries.CsvQuery) {
    Absence("absences", DwQueries.getAbsences),
    ApplicationInfos("application_info", DwQueries.getApplicationInfos),
    AssistanceActions("assistance_actions", DwQueries.getAssistanceActions),
    ChildAggregate("child_aggregate_3v_", DwQueries.getChildAggregate),
    ChildReservations("child_reservations_", DwQueries.getChildReservations),
    ChildAttendances("child_attendances_", DwQueries.getChildAttendances),
    DailyInfo("daily_info_3v_", DwQueries.getDailyInfos),
    DailyUnitAndGroupAttendance(
        "daily_units_and_groups_attendance_3v_",
        DwQueries.getDailyUnitsAndGroupsAttendances,
    ),
    DailyUnitOccupancyConfirmed(
        "daily_units_occupancy_confirmed_",
        DwQueries.getDailyUnitsOccupanciesConfirmed,
    ),
    DailyUnitOccupancyRealized(
        "daily_units_occupancy_realized_",
        DwQueries.getDailyUnitsOccupanciesRealized,
    ),
    FeeDecision("fee_decisions_3v_", DwQueries.getFeeDecisions),
    UnitAndGroup("units_and_groups_3v_", DwQueries.getUnitsAndGroups),
    VoucherValueDecision("voucher_value_decisions_3v_", DwQueries.getVoucherValueDecisions),
}
