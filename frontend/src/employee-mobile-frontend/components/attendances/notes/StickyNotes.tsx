// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import styled from 'styled-components'
import { Result } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Dimmed, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { ChildAttendanceContext } from '../../../state/child-attendance'
import { useTranslation } from '../../../state/i18n'
import { InlineAsyncButton } from '../components'

export type StickyNoteBody = { note: string; expires: LocalDate }
export type StickyNote = StickyNoteBody & { id: UUID }
export type NoteUnderEdit = StickyNoteBody & { id?: UUID }
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

export const StickyNotes = React.memo(Notes)
