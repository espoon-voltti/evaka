// SPDX-FileCopyrightText: 2026 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere

import evaka.core.bi.BiTable
import kotlin.test.Test
import kotlin.test.assertEquals

class TampereBiTableExclusionTest {
    @Test
    fun `Tampere BI export excludes only the realtime and delta variants`() {
        assertEquals(
            setOf(BiTable.StaffAttendanceRealtime, BiTable.AttendanceReservationDelta),
            TampereConfig.excludedBiTables,
        )
    }

    @Test
    fun `Tampere BI export ships exactly the pinned set of BiTable entries`() {
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
                BiTable.AttendanceReservation,
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
                BiTable.StaffOccupancyCoefficient,
                BiTable.VoucherValueDecision,
            )
        val actual = BiTable.entries.toSet() - TampereConfig.excludedBiTables
        assertEquals(
            expected,
            actual,
            "Tampere BI shipped tables changed. If a BiTable was added or removed upstream, " +
                "explicitly decide whether Tampere should ship it and update both this pin and " +
                "TampereConfig.excludedBiTables.",
        )
    }
}
