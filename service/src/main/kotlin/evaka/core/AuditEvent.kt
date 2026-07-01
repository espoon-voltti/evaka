// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core

enum class AuditEvent(val securityEvent: Boolean = false, val securityLevel: String = "low") {
    DecisionAccept,
    ChildServiceApplicationsRead,
    UnitServiceApplicationsRead,
    ChildServiceApplicationAccept,
    ChildServiceApplicationReject,
    AttendanceReservationCitizenCreate,
    AttendanceReservationEmployeeCreate,
    ChildDatePresenceUpsert;

    val eventCode = name
}
