// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { GroupNote } from 'lib-common/generated/api-types/note'
import { UUID } from 'lib-common/types'
import React, { useCallback } from 'react'
import {
  deleteGroupNote,
  postGroupNote,
  putGroupNote
} from '../../../api/notes'
import { useTranslation } from '../../../state/i18n'
import { EditedNote } from './notes'
import { StickyNoteTab } from './StickyNoteTab'

interface Props {
  groupId: UUID
  notes: GroupNote[]
}

export const GroupNotesTab = React.memo(function GroupNotesTab({
  groupId,
  notes
}: Props) {
  const { i18n } = useTranslation()
  const onSave = useCallback(
    ({ id, ...body }: EditedNote) =>
      id ? putGroupNote(id, body) : postGroupNote(groupId, body),
    [groupId]
  )
  return (
    <StickyNoteTab
      notes={notes}
      onSave={onSave}
      onRemove={deleteGroupNote}
      title={i18n.attendances.notes.groupNote}
      placeholder={i18n.attendances.notes.placeholders.groupNote}
    />
  )
})
