// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { useTranslation } from '../../state/i18n'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'
import { UUID } from '../../types'
import AddButton from '@evaka/lib-components/src/atoms/buttons/AddButton'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { ApplicationNote } from '../../types/application'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import { getApplicationNotes } from '../../api/applications'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'
import ErrorSegment from '@evaka/lib-components/src/atoms/state/ErrorSegment'
import ApplicationNoteBox from '../../components/application-page/ApplicationNoteBox'
import { UserContext } from '../../state/user'
import { requireRole } from '../../utils/roles'
import styled from 'styled-components'
import { defaultMargins, Gap } from '@evaka/lib-components/src/white-space'

const Sticky = styled.div`
  position: sticky;
  top: ${defaultMargins.s};
`

type Props = {
  applicationId: UUID
}

export default React.memo(function ApplicationNotes({ applicationId }: Props) {
  const { i18n } = useTranslation()
  const { roles, user } = useContext(UserContext)

  const [notes, setNotes] = useState<Result<ApplicationNote[]>>(Loading.of())
  const [editing, setEditing] = useState<UUID | null>(null)
  const [creating, setCreating] = useState<boolean>(false)

  const loadNotes = useRestApi(
    () => getApplicationNotes(applicationId),
    setNotes
  )
  useEffect(loadNotes, [loadNotes, applicationId])

  const editAllowed = (note: ApplicationNote): boolean => {
    return (
      requireRole(roles, 'ADMIN', 'SERVICE_WORKER') ||
      !!(
        requireRole(roles, 'UNIT_SUPERVISOR') &&
        user &&
        user.id &&
        user.id === note.createdBy
      )
    )
  }

  return (
    <>
      {notes.isLoading && <SpinnerSegment />}
      {notes.isFailure && <ErrorSegment />}
      {notes.isSuccess && (
        <>
          <FixedSpaceColumn>
            {notes.value.map((note) =>
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
                  editable={!creating && editing === null && editAllowed(note)}
                  onStartEdit={() => setEditing(note.id)}
                  onDelete={() => loadNotes()}
                />
              )
            )}
          </FixedSpaceColumn>

          {notes.value.length > 0 && <Gap size="s" />}

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
            ) : editing ? null : (
              <AddButton
                onClick={() => setCreating(true)}
                text={i18n.application.notes.add}
                dataQa="add-note"
              />
            )}
          </Sticky>
        </>
      )}
    </>
  )
})
