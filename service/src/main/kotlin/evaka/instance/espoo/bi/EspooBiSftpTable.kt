// SPDX-FileCopyrightText: 2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo.bi

import evaka.core.bi.BiQueries
import evaka.core.bi.BiTable

enum class EspooBiSftpTable(val fileName: String, val query: BiQueries.CsvQuery) {
    AbsenceSnapshot("absence_SNAPSHOT", BiQueries.getAbsencesSnapshot),
    AssistanceFactor(BiTable.AssistanceFactor),
    BackupCare(BiTable.BackupCare),
    CareArea(BiTable.CareArea),
    Daycare(BiTable.Daycare),
    DaycareCaretaker(BiTable.DaycareCaretaker),
    DaycareGroup(BiTable.DaycareGroup),
    DaycareGroupPlacement(BiTable.DaycareGroupPlacement),
    Person(BiTable.Person),
    Placement(BiTable.Placement),
    ServiceNeed(BiTable.ServiceNeed),
    ServiceNeedOption(BiTable.ServiceNeedOption),
    StaffAttendanceExternal(BiTable.StaffAttendanceExternal),
    StaffAttendanceRealtimeSnapshot(
        "staff_attendance_realtime_SNAPSHOT",
        BiQueries.getStaffAttendanceRealtimeSnapshot,
    );

    constructor(table: BiTable) : this(table.fileName, table.query)
}
