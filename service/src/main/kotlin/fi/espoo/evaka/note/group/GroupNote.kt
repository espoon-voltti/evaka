// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.group

import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupNoteId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

data class GroupNote(
    val id: GroupNoteId,
    val groupId: GroupId,
    val note: String,
    val modifiedAt: HelsinkiDateTime
)

data class GroupNoteBody(
    val note: String
)
