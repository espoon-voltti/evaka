// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.note.child.sticky

import evaka.core.shared.ChildId
import evaka.core.shared.ChildStickyNoteId
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate

data class ChildStickyNote(
    val id: ChildStickyNoteId,
    val childId: ChildId,
    val note: String,
    val modifiedAt: HelsinkiDateTime,
    val expires: LocalDate,
)

data class ChildStickyNoteBody(val note: String, val expires: LocalDate)
