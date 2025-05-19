// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo } from 'react'

import type { ChildStickyNote } from 'lib-common/generated/api-types/note'
import type {
  ChildId,
  ChildStickyNoteId,
  DaycareId
} from 'lib-common/generated/api-types/shared'
import { useMutationResult } from 'lib-common/query'
import type { StickyNoteTabLabels } from 'lib-components/employee/notes/StickyNoteTab'
import { StickyNoteTab } from 'lib-components/employee/notes/StickyNoteTab'
import type { EditedNote } from 'lib-components/employee/notes/notes'

import type { Translations } from '../common/i18n'
import { useTranslation } from '../common/i18n'

import {
  createChildStickyNoteMutation,
  deleteChildStickyNoteMutation,
  updateChildStickyNoteMutation
} from './queries'

const getStickyNoteTabLabels = (i18n: Translations): StickyNoteTabLabels => ({
  addNew: i18n.attendances.notes.addNew,
  editor: {
    cancel: i18n.common.cancel,
    placeholder: i18n.attendances.notes.placeholders.childStickyNote,
    save: i18n.common.save
  },
  static: {
    edit: i18n.common.edit,
    remove: i18n.common.remove,
    validTo: i18n.common.validTo
  },
  title: i18n.attendances.notes.childStickyNotes
})

interface Props {
  unitId: DaycareId
  childId: ChildId
  notes: ChildStickyNote[]
}

export const ChildStickyNotesTab = React.memo(function ChildStickyNotesTab({
  unitId,
  childId,
  notes
}: Props) {
  const { i18n } = useTranslation()

  const { mutateAsync: createChildStickyNote } = useMutationResult(
    createChildStickyNoteMutation
  )
  const { mutateAsync: updateChildStickyNote } = useMutationResult(
    updateChildStickyNoteMutation
  )
  const { mutateAsync: deleteChildStickyNote } = useMutationResult(
    deleteChildStickyNoteMutation
  )
  const onSave = useCallback(
    ({ id, ...body }: EditedNote<ChildStickyNoteId>) =>
      id
        ? updateChildStickyNote({ unitId, noteId: id, body })
        : createChildStickyNote({ unitId, childId, body }),
    [updateChildStickyNote, createChildStickyNote, unitId, childId]
  )
  const onRemove = useCallback(
    (noteId: ChildStickyNoteId) => deleteChildStickyNote({ unitId, noteId }),
    [deleteChildStickyNote, unitId]
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
