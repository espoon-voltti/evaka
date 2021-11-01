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
import React, { useCallback, useState } from 'react'
import { EditedNote, Note } from './notes'
import { StaticLabels, StaticStickyNote } from './StaticStickyNote'
import { EditorLabels, StickyNoteEditor } from './StickyNoteEditor'

export interface StickyNoteTabLabels {
  title: string
  addNew: string
  editor: EditorLabels
  static: StaticLabels
}

interface StickyNoteTabProps {
  labels: StickyNoteTabLabels
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
  labels,
  notes,
  onSave,
  onRemove
}: StickyNoteTabProps) {
  const [editing, setEditing] = useState<UUID | 'new' | null>(
    notes.length === 0 ? 'new' : null
  )
  const onCancelEdit = useCallback(() => setEditing(null), [])
  const setNoteToEdit = useCallback((id: UUID) => () => setEditing(id), [])

  return (
    <>
      <ContentArea opaque paddingHorizontal="s">
        <H2 primary noMargin>
          {labels.title}
        </H2>

        <Gap size="xs" />

        <FixedSpaceRow justifyContent="flex-end">
          <InlineButton
            data-qa="sticky-note-new"
            disabled={editing !== null}
            onClick={() => setEditing('new')}
            text={labels.addNew}
          />
        </FixedSpaceRow>
      </ContentArea>

      {editing === 'new' && (
        <StickyNoteEditor
          note={newNote()}
          onCancelEdit={onCancelEdit}
          onSave={onSave}
          labels={labels.editor}
        />
      )}

      {notes.map((note) =>
        editing === note.id ? (
          <StickyNoteEditor
            key={note.id}
            note={note}
            onCancelEdit={onCancelEdit}
            labels={labels.editor}
            onSave={onSave}
          />
        ) : (
          <StaticStickyNote
            key={note.id}
            note={note}
            onRemove={onRemove}
            editable={!editing}
            onEdit={setNoteToEdit(note.id)}
            labels={labels.static}
          />
        )
      )}
    </>
  )
})
