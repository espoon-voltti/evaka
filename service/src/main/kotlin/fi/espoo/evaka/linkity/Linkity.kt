// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.linkity

import fi.espoo.evaka.shared.domain.HelsinkiDateTime

enum class ShiftType {
    PRESENT,
    TRAINING,
}

enum class StampingType {
    PRESENT,
    TRAINING,
    OTHER_WORK,
    OVERTIME,
    JUSTIFIED_CHANGE,
}

data class Shift(
    val sarastiaId: String,
    val workShiftId: String,
    val startDateTime: HelsinkiDateTime,
    val endDateTime: HelsinkiDateTime,
    val type: ShiftType,
    val notes: String? = null,
)

data class Stamping(
    val stampingId: String,
    val sarastiaId: String,
    val startTime: HelsinkiDateTime,
    val endTime: HelsinkiDateTime,
    val type: StampingType,
)

data class StampingBatch(
    val rangeStartTime: HelsinkiDateTime,
    val rangeEndTime: HelsinkiDateTime,
    val stampings: List<Stamping>,
)
