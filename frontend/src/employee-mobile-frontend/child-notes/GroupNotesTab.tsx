// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo } from 'react'

import { GroupNote } from 'lib-common/generated/api-types/note'
import { useMutationResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import {
  StickyNoteTab,
  StickyNoteTabLabels
} from 'lib-components/employee/notes/StickyNoteTab'
import { EditedNote } from 'lib-components/employee/notes/notes'

import { Translations, useTranslation } from '../common/i18n'

import {
  createGroupNoteMutation,
  deleteGroupNoteMutation,
  updateGroupNoteMutation
} from './queries'

const getStickyNoteTabLabels = (i18n: Translations): StickyNoteTabLabels => ({
  addNew: i18n.attendances.notes.addNew,
  editor: {
    cancel: i18n.common.cancel,
    placeholder: i18n.attendances.notes.placeholders.groupNote,
    save: i18n.common.save
  },
  static: {
    edit: i18n.common.edit,
    remove: i18n.common.remove,
    validTo: i18n.common.validTo
  },
  title: i18n.attendances.notes.groupNote
})

interface Props {
  groupId: UUID
  notes: GroupNote[]
}

export const GroupNotesTab = React.memo(function GroupNotesTab({
  groupId,
  notes
}: Props) {
  const { i18n } = useTranslation()
  const { mutateAsync: createGroupNote } = useMutationResult(
    createGroupNoteMutation
  )
  const { mutateAsync: updateGroupNote } = useMutationResult(
    updateGroupNoteMutation
  )
  const { mutateAsync: deleteGroupNote } = useMutationResult(
    deleteGroupNoteMutation
  )

  const onSave = useCallback(
    ({ id, ...body }: EditedNote) =>
      id
        ? updateGroupNote({ groupId, noteId: id, body })
        : createGroupNote({ groupId, body }),
    [createGroupNote, updateGroupNote, groupId]
  )
  const onRemove = useCallback(
    (noteId: UUID) => deleteGroupNote({ groupId, noteId }),
    [deleteGroupNote, groupId]
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
      smallerHeading
    />
  )
})
