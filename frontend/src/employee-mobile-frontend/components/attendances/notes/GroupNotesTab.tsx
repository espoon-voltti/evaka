// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import { GroupNote } from 'lib-common/generated/api-types/note'
import { UUID } from 'lib-common/types'
import { EditedNote } from 'lib-components/employee/notes/notes'
import React, { useCallback, useContext, useMemo } from 'react'
import {
  StickyNoteTab,
  StickyNoteTabLabels
} from 'lib-components/employee/notes/StickyNoteTab'
import {
  deleteGroupNote,
  postGroupNote,
  putGroupNote
} from '../../../api/notes'
import { ChildAttendanceContext } from '../../../state/child-attendance'
import { useTranslation } from '../../../state/i18n'
import { getStickyNoteTabLabels } from './labels'

interface Props {
  groupId: UUID
  notes: GroupNote[]
}

export const GroupNotesTab = React.memo(function GroupNotesTab({
  groupId,
  notes
}: Props) {
  const { i18n } = useTranslation()

  const { reloadAttendances } = useContext(ChildAttendanceContext)
  const reloadOnSuccess = useCallback(
    (res: Result<unknown>) => res.map(() => reloadAttendances()),
    [reloadAttendances]
  )
  const onSave = useCallback(
    ({ id, ...body }: EditedNote) =>
      (id ? putGroupNote(id, body) : postGroupNote(groupId, body)).then(
        reloadOnSuccess
      ),
    [groupId, reloadOnSuccess]
  )
  const onRemove = useCallback(
    (id) => deleteGroupNote(id).then(reloadOnSuccess),
    [reloadOnSuccess]
  )
  const labels = useMemo<StickyNoteTabLabels>(
    () => getStickyNoteTabLabels(i18n),
    [i18n]
  )

  return (
    <StickyNoteTab
      notes={notes}
      onSave={onSave}
      onRemove={onRemove}
      labels={labels}
    />
  )
})
