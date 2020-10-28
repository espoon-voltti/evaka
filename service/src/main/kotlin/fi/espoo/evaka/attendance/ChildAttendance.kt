// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import java.time.OffsetDateTime
import java.util.UUID

data class ChildAttendance(
    val id: UUID,
    val childId: UUID,
    val arrived: OffsetDateTime,
    val departed: OffsetDateTime?
)
