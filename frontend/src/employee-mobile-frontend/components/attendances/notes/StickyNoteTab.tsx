// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import TextArea from 'lib-components/atoms/form/TextArea'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Dimmed, H2, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import React, {
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState
} from 'react'
import { useHistory } from 'react-router-dom'
import styled from 'styled-components'
import { ChildAttendanceContext } from '../../../state/child-attendance'
import { useTranslation } from '../../../state/i18n'
import { InlineAsyncButton } from '../components'

export type StickyNoteBody = { note: string; expires: LocalDate }
type StickyNote = StickyNoteBody & { id: UUID }
type NoteUnderEdit = StickyNoteBody & { id?: UUID }

const ValidTo = styled(Dimmed)`
  font-style: italic;
`

interface NotesProps {
  notes: StickyNote[]
  onEdit: (note: StickyNote) => void
  onRemove: (id: UUID) => Promise<Result<unknown>>
}

function Notes({ notes, onEdit, onRemove }: NotesProps) {
  const { i18n } = useTranslation()
  const { reloadAttendances } = useContext(ChildAttendanceContext)
  return (
    <>
      {notes.map((note) => (
        <ContentArea
          key={note.id}
          opaque
          paddingHorizontal="s"
          data-qa="sticky-note"
        >
          <P noMargin data-qa="sticky-note-note">
            {note.note}
          </P>
          <Gap size="xs" />
          <ValidTo data-qa="sticky-note-expires">
            {i18n.common.validTo(note.expires.format())}
          </ValidTo>
          <Gap size="s" />
          <FixedSpaceRow justifyContent="flex-end">
            <InlineButton
              onClick={() => onEdit(note)}
              text={i18n.common.edit}
            />
            <InlineAsyncButton
              onClick={() => onRemove(note.id)}
              onSuccess={() => reloadAttendances()}
              text={i18n.common.remove}
            />
          </FixedSpaceRow>
        </ContentArea>
      ))}
    </>
  )
}
const NotesList = React.memo(Notes)

interface StickyNoteTabProps {
  title: string
  placeholder: string
  notes: StickyNote[]
  onSave: (body: StickyNoteBody, id?: UUID) => Promise<Result<unknown>>
  onRemove: (id: UUID) => Promise<Result<unknown>>
}

export default React.memo(function StickyNoteTab({
  title,
  placeholder,
  notes,
  onSave,
  onRemove
}: StickyNoteTabProps) {
  const { i18n } = useTranslation()
  const history = useHistory()
  const { reloadAttendances } = useContext(ChildAttendanceContext)

  const textAreaRef = useRef<HTMLTextAreaElement>(null)

  const scrollToFormAndFocus = useCallback(() => {
    if (!textAreaRef.current) return
    const top =
      window.pageYOffset + textAreaRef.current.getBoundingClientRect().top
    window.scrollTo({ top, left: 0, behavior: 'smooth' })
    textAreaRef.current.focus()
  }, [])

  const emptyNote = useMemo(
    () => ({ note: '', expires: LocalDate.today().addDays(7) }),
    []
  )
  const [note, setNote] = useState<NoteUnderEdit>(emptyNote)

  const clearNote = useCallback(() => setNote(emptyNote), [emptyNote])
  const deleteNote = useCallback(
    (id: UUID) => {
      if (id === note.id) {
        clearNote()
      }
      return onRemove(id)
    },
    [onRemove, clearNote, note.id]
  )
  const editNote = useCallback(
    (n: StickyNote) => {
      setNote(n)
      scrollToFormAndFocus()
    },
    [scrollToFormAndFocus]
  )
  const saveNote = useCallback(
    () =>
      onSave(note, note.id).then((res) =>
        res.map(() => {
          clearNote()
          reloadAttendances()
          history.goBack()
        })
      ),
    [onSave, note, clearNote, reloadAttendances, history]
  )

  const onTextChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) =>
      setNote((old) => ({ ...old, note: e.target.value })),
    []
  )

  return (
    <>
      <ContentArea opaque paddingHorizontal="s">
        <FixedSpaceColumn spacing="m">
          <H2 primary noMargin>
            {title}
          </H2>

          <TextArea
            inputRef={textAreaRef}
            value={note.note}
            onChange={onTextChange}
            placeholder={placeholder}
            data-qa="sticky-note-input"
          />
        </FixedSpaceColumn>

        <Gap />

        <FixedSpaceRow justifyContent="flex-end">
          <Button text={i18n.common.cancel} onClick={clearNote} />
          <AsyncButton
            primary
            data-qa="sticky-note-save"
            text={i18n.common.save}
            onClick={saveNote}
            onSuccess={clearNote}
            disabled={!note.note}
          />
        </FixedSpaceRow>
      </ContentArea>

      <NotesList notes={notes} onEdit={editNote} onRemove={deleteNote} />
    </>
  )
})
