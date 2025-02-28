// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.linkity

import fi.espoo.evaka.shared.domain.HelsinkiDateTime

enum class ShiftType {
    PRESENT,
    TRAINING,
}

data class Shift(
    val sarastiaId: String,
    val workShiftId: String,
    val startDateTime: HelsinkiDateTime,
    val endDateTime: HelsinkiDateTime,
    val type: ShiftType,
    val notes: String? = null,
)
