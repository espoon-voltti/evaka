// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.child.sticky

import fi.espoo.evaka.shared.GroupNoteId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.util.UUID

data class ChildStickyNote(
    val id: GroupNoteId,
    val childId: UUID,
    val note: String,
    val modifiedAt: HelsinkiDateTime,
    val expires: LocalDate
)

data class ChildStickyNoteBody(
    val note: String,
    val expires: LocalDate
)
