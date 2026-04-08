// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu.dw

enum class DwQuery(val queryName: String, val query: CsvQuery) {
    ABSENCE("absences", DwQueries.getAbsences),
    APPLICATION_INFO("application_info", DwQueries.getApplicationInfos),
    ASSISTANCE_ACTION("assistance_actions_", DwQueries.getAssistanceActions),
    ASSISTANCE_NEED_DECISION("assistance_need_decisions_", DwQueries.getAssistanceNeedDecisions),
    CHILD_RESERVATIONS("child_reservations_", DwQueries.getChildReservations),
    DAILY_INFO("daily_info_", DwQueries.getDailyInfos),
    DAILY_ATTENDANCE("daily_units_and_groups_", DwQueries.getDailyUnitsAndGroupsAttendances),
    DAILY_OCCUPANCY_CONFIRMED(
        "daily_units_occupancy_confirmed_",
        DwQueries.getDailyUnitsOccupanciesConfirmed,
    ),
    DAILY_OCCUPANCY_REALIZED(
        "daily_units_occupancy_realized_",
        DwQueries.getDailyUnitsOccupanciesRealized,
    ),
    DAYCARE_ASSISTANCE("daycare_assistances_", DwQueries.getDaycareAssistances),
    FEE_DECISION("fee_decisions_", DwQueries.getFeeDecisions),
    OTHER_ASSISTANCE_MEASURE("other_assistance_measures_", DwQueries.getOtherAssistanceMeasures),
    PLACEMENT("placements_", DwQueries.getPlacements),
    PRESCHOOL_ASSISTANCE("preschool_assistances_", DwQueries.getPreschoolAssistances),
    UNIT_GROUP("units_and_groups_", DwQueries.getUnitsAndGroups),
    VOUCHER_VALUE_DECISION("voucher_value_decisions_", DwQueries.getVoucherValueDecisions),
}

enum class FabricQuery(val queryName: String, val query: CsvQuery) {
    ABSENCE("absences", FabricQueries.getAbsences()),
    APPLICATION_INFO("application_info", FabricQueries.getApplicationInfos()),
    ASSISTANCE_ACTION("assistance_actions_", FabricQueries.getAssistanceActions()),
    CHILD_RESERVATIONS("child_reservations_", FabricQueries.getChildReservations()),
    DAILY_INFO("daily_info_", FabricQueries.getDailyInfos),
    DAILY_ATTENDANCE("daily_units_and_groups_", FabricQueries.getDailyUnitsAndGroupsAttendances),
    DAILY_OCCUPANCY_CONFIRMED(
        "daily_units_occupancy_confirmed_",
        FabricQueries.getDailyUnitsOccupanciesConfirmed,
    ),
    DAILY_OCCUPANCY_REALIZED(
        "daily_units_occupancy_realized_",
        FabricQueries.getDailyUnitsOccupanciesRealized,
    ),
    DAYCARE_ASSISTANCE("daycare_assistances_", FabricQueries.getDaycareAssistance()),
    FEE_DECISION("fee_decisions_", FabricQueries.getFeeDecisions()),
    OTHER_ASSISTANCE_MEASURE(
        "other_assistance_measures_",
        FabricQueries.getOtherAssistanceMeasures(),
    ),
    PLACEMENT("placements_", FabricQueries.getPlacements()),
    PRESCHOOL_ASSISTANCE("preschool_assistances_", FabricQueries.getPreschoolAssistance()),
    UNIT_GROUP("units_and_groups_", FabricQueries.getUnitsAndGroups()),
    VOUCHER_VALUE_DECISION("voucher_value_decisions_", FabricQueries.getVoucherValueDecisions()),
}

enum class FabricHistoryQuery(val queryName: String, val query: CsvQuery) {
    ABSENCE("absences", FabricQueries.getAbsences(true)),
    APPLICATION_INFO("application_info", FabricQueries.getApplicationInfos(true)),
    ASSISTANCE_ACTION("assistance_actions_", FabricQueries.getAssistanceActions(true)),
    CHILD_RESERVATIONS("child_reservations_", FabricQueries.getChildReservations(true)),
    DAYCARE_ASSISTANCE("daycare_assistances_", FabricQueries.getDaycareAssistance(true)),
    FEE_DECISION("fee_decisions_", FabricQueries.getFeeDecisions(true)),
    OTHER_ASSISTANCE_MEASURE(
        "other_assistance_measures_",
        FabricQueries.getOtherAssistanceMeasures(true),
    ),
    PLACEMENT("placements_", FabricQueries.getPlacements(true)),
    PRESCHOOL_ASSISTANCE("preschool_assistances_", FabricQueries.getPreschoolAssistance(true)),
    UNIT_GROUP("units_and_groups_", FabricQueries.getUnitsAndGroups(true)),
    VOUCHER_VALUE_DECISION("voucher_value_decisions_", FabricQueries.getVoucherValueDecisions(true)),
}
