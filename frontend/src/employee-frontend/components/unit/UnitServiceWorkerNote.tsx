// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { useBoolean } from 'lib-common/form/hooks'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import InputField from 'lib-components/atoms/form/InputField'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { faPen, faPlus, faTrash } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import {
  unitServiceWorkerNoteMutation,
  unitServiceWorkerNoteQuery
} from './queries'

export default React.memo(function UnitServiceWorkerNote({
  unitId,
  canEdit
}: {
  unitId: DaycareId
  canEdit: boolean
}) {
  const { i18n } = useTranslation()
  const response = useQueryResult(
    unitServiceWorkerNoteQuery({ daycareId: unitId })
  )
  const [editing, { on: openEditor, off: closeEditor }] = useBoolean(false)
  const [text, setText] = useState('')
  const startEditing = (note: string) => {
    setText(note)
    openEditor()
  }

  return renderResult(response, ({ note }) => (
    <FixedSpaceColumn>
      {editing ? (
        <>
          <InputField value={text} onChange={setText} data-qa="note-input" />
          <FixedSpaceRow>
            <MutateButton
              appearance="inline"
              text={i18n.common.save}
              mutation={unitServiceWorkerNoteMutation}
              onClick={() => ({
                daycareId: unitId,
                body: { note: text.trim() }
              })}
              onSuccess={closeEditor}
              data-qa="note-save-btn"
            />
            <Button
              appearance="inline"
              text={i18n.common.cancel}
              onClick={closeEditor}
            />
          </FixedSpaceRow>
        </>
      ) : note.trim() === '' ? (
        canEdit ? (
          <Button
            appearance="inline"
            text={i18n.unit.serviceWorkerNote.add}
            icon={faPlus}
            onClick={() => startEditing('')}
            data-qa="note-add-btn"
          />
        ) : null
      ) : (
        <>
          <div>
            <AlertBox
              message={note.trim()}
              noMargin
              data-qa="service-worker-note"
            />
          </div>
          {canEdit && (
            <FixedSpaceRow>
              <Button
                appearance="inline"
                text={i18n.common.edit}
                icon={faPen}
                onClick={() => startEditing(note)}
                data-qa="note-edit-btn"
              />
              <MutateButton
                appearance="inline"
                text={i18n.common.remove}
                icon={faTrash}
                mutation={unitServiceWorkerNoteMutation}
                onClick={() => ({ daycareId: unitId, body: { note: '' } })}
                onSuccess={closeEditor}
                data-qa="note-remove-btn"
              />
            </FixedSpaceRow>
          )}
        </>
      )}
    </FixedSpaceColumn>
  ))
})
