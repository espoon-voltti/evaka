// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.group

import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupNoteId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate

data class GroupNote(
    val id: GroupNoteId,
    val groupId: GroupId,
    val note: String,
    val modifiedAt: HelsinkiDateTime,
    val expires: LocalDate,
)

data class GroupNoteBody(val note: String, val expires: LocalDate)
