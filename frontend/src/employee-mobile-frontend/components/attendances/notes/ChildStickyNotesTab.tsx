// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ChildStickyNote } from 'lib-common/generated/api-types/note'
import { UUID } from 'lib-common/types'
import React, { useCallback } from 'react'
import {
  deleteChildStickyNote,
  postChildStickyNote,
  putChildStickyNote
} from '../../../api/notes'
import { useTranslation } from '../../../state/i18n'
import { EditedNote } from './notes'
import { StickyNoteTab } from './StickyNoteTab'

interface Props {
  childId: UUID
  notes: ChildStickyNote[]
}

export const ChildStickyNotesTab = React.memo(function ChildStickyNotesTab({
  childId,
  notes
}: Props) {
  const { i18n } = useTranslation()
  const onSaveChildStickyNote = useCallback(
    ({ id, ...body }: EditedNote) =>
      id ? putChildStickyNote(id, body) : postChildStickyNote(childId, body),
    [childId]
  )
  return (
    <StickyNoteTab
      notes={notes}
      onSave={onSaveChildStickyNote}
      onRemove={deleteChildStickyNote}
      title={i18n.attendances.notes.childStickyNotes}
      placeholder={i18n.attendances.notes.placeholders.childStickyNote}
    />
  )
})
