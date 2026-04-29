// SPDX-FileCopyrightText: 2026 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu

import evaka.core.bi.BiTable
import kotlin.test.Test
import kotlin.test.assertEquals

class OuluBiTableExclusionTest {
    @Test
    fun `Oulu BI export excludes only the full attendance reservation snapshot`() {
        assertEquals(setOf(BiTable.AttendanceReservation), OuluConfig.excludedBiTables)
    }

    @Test
    fun `Oulu BI export ships exactly the pinned set of BiTable entries`() {
        val expected =
            setOf(
                BiTable.Absence,
                BiTable.Application,
                BiTable.ApplicationForm,
                BiTable.AssistanceAction,
                BiTable.AssistanceActionOption,
                BiTable.AssistanceActionOptionRef,
                BiTable.AssistanceFactor,
                BiTable.AssistanceNeedVoucherCoefficient,
                BiTable.AttendanceReservationDelta,
                BiTable.BackupCare,
                BiTable.CareArea,
                BiTable.Child,
                BiTable.ChildAttendance,
                BiTable.Daycare,
                BiTable.DaycareAssistance,
                BiTable.DaycareCaretaker,
                BiTable.DaycareGroup,
                BiTable.DaycareGroupPlacement,
                BiTable.Decision,
                BiTable.Employee,
                BiTable.EvakaUser,
                BiTable.FeeAlteration,
                BiTable.FeeDecision,
                BiTable.FeeDecisionChild,
                BiTable.FeeThresholds,
                BiTable.FridgeChild,
                BiTable.FridgePartner,
                BiTable.Guardian,
                BiTable.GuardianBlocklist,
                BiTable.HolidayPeriod,
                BiTable.HolidayPeriodQuestionnaireAnswer,
                BiTable.Income,
                BiTable.OtherAssistanceMeasure,
                BiTable.Person,
                BiTable.Placement,
                BiTable.PreschoolAssistance,
                BiTable.ServiceNeed,
                BiTable.ServiceNeedOption,
                BiTable.ServiceNeedOptionVoucherValue,
                BiTable.StaffAttendance,
                BiTable.StaffAttendanceExternal,
                BiTable.StaffAttendancePlan,
                BiTable.StaffAttendanceRealtime,
                BiTable.StaffOccupancyCoefficient,
                BiTable.VoucherValueDecision,
            )
        val actual = BiTable.entries.toSet() - OuluConfig.excludedBiTables
        assertEquals(
            expected,
            actual,
            "Oulu BI shipped tables changed. If a BiTable was added or removed upstream, " +
                "explicitly decide whether Oulu should ship it and update both this pin and " +
                "OuluConfig.excludedBiTables.",
        )
    }
}
