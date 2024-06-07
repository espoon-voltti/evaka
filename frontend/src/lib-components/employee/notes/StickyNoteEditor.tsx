// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'

import { Result } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import { Button } from 'lib-components/atoms/buttons/Button'
import TextArea from 'lib-components/atoms/form/TextArea'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'

import { InlineAsyncButton } from './InlineAsyncButton'
import { EditedNote, Note } from './notes'

export interface EditorLabels {
  cancel: string
  save: string
  placeholder: string
}

interface Props {
  note: Note
  onSave: (note: EditedNote) => Promise<Result<unknown>>
  onCancelEdit: () => void
  labels: EditorLabels
}

export const StickyNoteEditor = React.memo(function StickyNoteEditor({
  note,
  onCancelEdit,
  onSave,
  labels
}: Props) {
  const [text, setText] = useState(note.note)

  const saveNote = useCallback(
    () =>
      onSave({
        id: note.id,
        note: text,
        expires: LocalDate.todayInSystemTz().addDays(7)
      }),
    [note.id, onSave, text]
  )

  return (
    <ContentArea opaque paddingHorizontal="s" data-qa="sticky-note">
      <TextArea
        autoFocus
        value={text}
        onChange={setText}
        placeholder={labels.placeholder}
        data-qa="sticky-note-input"
      />
      <Gap size="xs" />
      <FixedSpaceRow justifyContent="flex-end">
        <Button
          appearance="inline"
          onClick={onCancelEdit}
          text={labels.cancel}
        />
        <InlineAsyncButton
          data-qa="sticky-note-save"
          text={labels.save}
          onClick={saveNote}
          onSuccess={onCancelEdit}
          disabled={!text}
        />
      </FixedSpaceRow>
    </ContentArea>
  )
})
