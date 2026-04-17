// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.note.group

import evaka.core.shared.GroupId
import evaka.core.shared.GroupNoteId
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate

data class GroupNote(
    val id: GroupNoteId,
    val groupId: GroupId,
    val note: String,
    val modifiedAt: HelsinkiDateTime,
    val expires: LocalDate,
    val createdAt: HelsinkiDateTime,
)

data class GroupNoteBody(val note: String, val expires: LocalDate)
