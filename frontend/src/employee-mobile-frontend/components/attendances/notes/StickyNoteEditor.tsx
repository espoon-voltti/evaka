// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import TextArea from 'lib-components/atoms/form/TextArea'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'
import React, { useCallback, useContext, useState } from 'react'
import { ChildAttendanceContext } from '../../../state/child-attendance'
import { useTranslation } from '../../../state/i18n'
import { InlineAsyncButton } from '../components'
import { EditedNote, Note } from './notes'

interface Props {
  note: Note
  placeholder: string
  onSave: (note: EditedNote) => Promise<Result<unknown>>
  onCancelEdit: () => void
}

export const StickyNoteEditor = React.memo(function StickyNoteEditor({
  note,
  onCancelEdit,
  onSave,
  placeholder
}: Props) {
  const { i18n } = useTranslation()
  const { reloadAttendances } = useContext(ChildAttendanceContext)

  const [text, setText] = useState(note.note)
  const onTextChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => setText(e.target.value),
    []
  )

  const reloadOnSuccess = useCallback(
    (res: Result<unknown>) => res.map(() => reloadAttendances()),
    [reloadAttendances]
  )
  const saveNote = useCallback(
    () =>
      onSave({
        id: note.id,
        note: text,
        expires: LocalDate.today().addDays(7)
      }).then(reloadOnSuccess),
    [onSave, note.id, text, reloadOnSuccess]
  )
  const cancel = useCallback(() => {
    setText(note.note)
    onCancelEdit()
  }, [note.note, onCancelEdit])

  return (
    <ContentArea opaque paddingHorizontal="s" data-qa="sticky-note">
      <TextArea
        autoFocus
        value={text}
        onChange={onTextChange}
        placeholder={placeholder}
        data-qa="sticky-note-input"
      />
      <Gap size="xs" />
      <FixedSpaceRow justifyContent="flex-end">
        <InlineButton onClick={cancel} text={i18n.common.cancel} />
        <InlineAsyncButton
          data-qa="sticky-note-save"
          text={i18n.common.save}
          onClick={saveNote}
          onSuccess={cancel}
          disabled={!text}
        />
      </FixedSpaceRow>
    </ContentArea>
  )
})
