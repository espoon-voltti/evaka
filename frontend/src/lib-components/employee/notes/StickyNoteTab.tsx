// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'

import type { Result } from 'lib-common/api'
import type { Id } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'
import { Button } from 'lib-components/atoms/buttons/Button'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H1, H2, H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import type { StaticLabels } from './StaticStickyNote'
import { StaticStickyNote } from './StaticStickyNote'
import type { EditorLabels } from './StickyNoteEditor'
import { StickyNoteEditor } from './StickyNoteEditor'
import type { EditedNote, Note } from './notes'

const newNote = (): EditedNote<never> => ({
  expires: LocalDate.todayInSystemTz().addDays(7),
  note: ''
})

export interface StickyNoteTabLabels {
  title: string
  addNew: string
  editor: EditorLabels
  static: StaticLabels
}

interface StickyNoteTabProps<IdType extends Id<string>> {
  labels: StickyNoteTabLabels
  notes: Note<IdType>[]
  onSave: (note: EditedNote<IdType>) => Promise<Result<unknown>>
  onRemove: (id: IdType) => Promise<Result<unknown>>
  smallerHeading?: boolean
  subHeading?: string
}

export function StickyNoteTab<IdType extends Id<string>>({
  labels,
  notes,
  onSave,
  onRemove,
  smallerHeading = false,
  subHeading
}: StickyNoteTabProps<IdType>) {
  // eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents
  const [editing, setEditing] = useState<UUID | 'new' | null>(
    notes.length === 0 ? 'new' : null
  )
  const onCancelEdit = useCallback(() => setEditing(null), [])
  const setNoteToEdit = useCallback((id: UUID) => () => setEditing(id), [])

  const headingProps = {
    noMargin: true,
    primary: true,
    children: labels.title
  }
  return (
    <>
      <ContentArea opaque paddingHorizontal="s">
        {smallerHeading ? <H2 {...headingProps} /> : <H1 {...headingProps} />}

        {!!subHeading && (
          <>
            <Gap size="xs" />
            <H3 primary noMargin>
              {subHeading}
            </H3>
          </>
        )}

        <Gap size="s" />

        <FixedSpaceRow justifyContent="flex-end">
          <Button
            appearance="inline"
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
}
