// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import React, { useState } from 'react'
import { useTranslation } from '../../../state/i18n'
import { EditedNote, Note } from './notes'
import { StickyNote } from './StickyNote'

interface StickyNoteTabProps {
  title: string
  placeholder: string
  notes: Note[]
  onSave: (note: EditedNote) => Promise<Result<unknown>>
  onRemove: (id: UUID) => Promise<Result<unknown>>
}

const newNote = () => ({
  id: '',
  expires: LocalDate.today().addDays(7),
  note: ''
})

export const StickyNoteTab = React.memo(function StickyNoteTab({
  title,
  placeholder,
  notes,
  onSave,
  onRemove
}: StickyNoteTabProps) {
  const { i18n } = useTranslation()
  const [addingNewNote, setAddingNewNote] = useState(false)
  return (
    <>
      <ContentArea opaque paddingHorizontal="s">
        <H2 primary noMargin>
          {title}
        </H2>

        <Gap size="xs" />

        <FixedSpaceRow justifyContent="flex-end">
          <InlineButton
            data-qa="sticky-note-new"
            disabled={addingNewNote}
            onClick={() => setAddingNewNote(true)}
            text={i18n.attendances.notes.addNew}
          />
        </FixedSpaceRow>
      </ContentArea>

      {addingNewNote && (
        <StickyNote
          editing
          note={newNote()}
          onCancel={() => setAddingNewNote(false)}
          onSave={onSave}
          onRemove={onRemove}
          placeholder={placeholder}
        />
      )}

      {notes.map((note) => (
        <StickyNote
          editing={false}
          key={note.id}
          note={note}
          placeholder={placeholder}
          onSave={onSave}
          onRemove={onRemove}
        />
      ))}
    </>
  )
})
