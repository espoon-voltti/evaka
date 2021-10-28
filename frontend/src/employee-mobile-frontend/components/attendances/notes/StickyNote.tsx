// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import TextArea from 'lib-components/atoms/form/TextArea'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Dimmed, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import React, {
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState
} from 'react'
import styled from 'styled-components'
import LocalDate from 'lib-common/local-date'
import { ChildAttendanceContext } from '../../../state/child-attendance'
import { useTranslation } from '../../../state/i18n'
import { InlineAsyncButton } from '../components'
import { Note, EditedNote } from './notes'

const ValidTo = styled(Dimmed)`
  font-style: italic;
`

interface Props {
  note: Note
  placeholder: string
  editing: boolean
  onSave: (note: EditedNote) => Promise<Result<unknown>>
  onRemove: (id: UUID) => Promise<Result<unknown>>
  onCancel?: () => void
}

export const StickyNote = React.memo(function StickyNote({
  note,
  editing: initialEditing,
  onCancel,
  onSave,
  onRemove,
  placeholder
}: Props) {
  const { i18n } = useTranslation()
  const { reloadAttendances } = useContext(ChildAttendanceContext)
  const textAreaRef = useRef<HTMLTextAreaElement>(null)
  const [editing, setEditing] = useState(initialEditing)

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
  const removeNote = useCallback(
    () => onRemove(note.id).then(reloadOnSuccess),
    [note.id, onRemove, reloadOnSuccess]
  )
  const cancel = useCallback(() => {
    setEditing(false)
    setText(note.note)
    onCancel?.()
  }, [note.note, onCancel])

  useEffect(() => {
    if (editing) {
      textAreaRef?.current?.focus()
    }
  }, [editing])

  return (
    <ContentArea opaque paddingHorizontal="s" data-qa="sticky-note">
      {editing ? (
        <TextArea
          inputRef={textAreaRef}
          value={text}
          onChange={onTextChange}
          placeholder={placeholder}
          data-qa="sticky-note-input"
        />
      ) : (
        <>
          <P noMargin data-qa="sticky-note-note" preserveWhiteSpace>
            {note.note}
          </P>
          <Gap size="xs" />
          <ValidTo data-qa="sticky-note-expires">
            {i18n.common.validTo(note.expires.format())}
          </ValidTo>
        </>
      )}
      <Gap size="xs" />
      <FixedSpaceRow justifyContent="flex-end">
        {editing ? (
          <>
            <InlineButton onClick={cancel} text={i18n.common.cancel} />
            <InlineAsyncButton
              data-qa="sticky-note-save"
              text={i18n.common.save}
              onClick={saveNote}
              onSuccess={cancel}
              disabled={!text}
            />
          </>
        ) : (
          <>
            <InlineButton
              data-qa="sticky-note-edit"
              onClick={() => setEditing(true)}
              text={i18n.common.edit}
            />
            <InlineAsyncButton
              data-qa="sticky-note-remove"
              onClick={removeNote}
              onSuccess={reloadAttendances}
              text={i18n.common.remove}
            />
          </>
        )}
      </FixedSpaceRow>
    </ContentArea>
  )
})
