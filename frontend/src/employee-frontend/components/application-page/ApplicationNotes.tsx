// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { applicationNotesQuery } from '../../api/applications-queries'
import ApplicationNoteBox from '../../components/application-page/ApplicationNoteBox'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

const Sticky = styled.div`
  position: sticky;
  top: ${defaultMargins.s};
`

type Props = {
  applicationId: UUID
  allowCreate: boolean
}

export default React.memo(function ApplicationNotes({
  applicationId,
  allowCreate
}: Props) {
  const { i18n } = useTranslation()

  const notesResponse = useQueryResult(applicationNotesQuery(applicationId))
  const [editing, setEditing] = useState<UUID | null>(null)
  const [creating, setCreating] = useState<boolean>(false)

  return renderResult(notesResponse, (notes) => (
    <>
      <FixedSpaceColumn data-qa="application-notes-list">
        {notes.map(({ note, permittedActions }) =>
          editing === note.id ? (
            <ApplicationNoteBox
              key={note.id}
              note={note}
              onSave={() => {
                setEditing(null)
              }}
              onCancel={() => setEditing(null)}
            />
          ) : (
            <ApplicationNoteBox
              key={note.id}
              note={note}
              editable={
                !creating &&
                editing === null &&
                permittedActions.includes('UPDATE') &&
                note.messageThreadId === null
              }
              deletable={
                !creating &&
                editing === null &&
                permittedActions.includes('DELETE') &&
                note.messageThreadId === null
              }
              onStartEdit={() => setEditing(note.id)}
            />
          )
        )}
      </FixedSpaceColumn>

      {notes.length > 0 && <Gap size="s" />}

      <Sticky>
        {creating ? (
          <ApplicationNoteBox
            applicationId={applicationId}
            onSave={() => {
              setCreating(false)
            }}
            onCancel={() => setCreating(false)}
          />
        ) : editing ? null : allowCreate ? (
          <AddButton
            onClick={() => setCreating(true)}
            text={i18n.application.notes.add}
            darker
            data-qa="add-note"
          />
        ) : null}
      </Sticky>
    </>
  ))
})
