// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { getApplicationNotes } from '../../api/applications'
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

  const [notesResponse, loadNotes] = useApiState(
    () => getApplicationNotes(applicationId),
    [applicationId]
  )
  const [editing, setEditing] = useState<UUID | null>(null)
  const [creating, setCreating] = useState<boolean>(false)

  return renderResult(notesResponse, (notes) => (
    <>
      <FixedSpaceColumn>
        {notes.map(({ note, permittedActions }) =>
          editing === note.id ? (
            <ApplicationNoteBox
              key={note.id}
              note={note}
              onSave={() => {
                setEditing(null)
                loadNotes()
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
                permittedActions.includes('UPDATE')
              }
              deletable={
                !creating &&
                editing === null &&
                permittedActions.includes('DELETE')
              }
              onStartEdit={() => setEditing(note.id)}
              onDelete={() => loadNotes()}
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
              loadNotes()
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
