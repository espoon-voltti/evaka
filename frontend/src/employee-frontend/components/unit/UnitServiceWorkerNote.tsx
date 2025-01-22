// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { useBoolean } from 'lib-common/form/hooks'
import { DaycareId } from 'lib-common/generated/api-types/shared'
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
  unitId
}: {
  unitId: DaycareId
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
          <InputField value={text} onChange={setText} />
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
            />
            <Button
              appearance="inline"
              text={i18n.common.cancel}
              onClick={closeEditor}
            />
          </FixedSpaceRow>
        </>
      ) : note.trim() === '' ? (
        <Button
          appearance="inline"
          text={i18n.unit.serviceWorkerNote.add}
          icon={faPlus}
          onClick={() => startEditing('')}
        />
      ) : (
        <>
          <div>
            <AlertBox message={note.trim()} noMargin />
          </div>
          <FixedSpaceRow>
            <Button
              appearance="inline"
              text={i18n.common.edit}
              icon={faPen}
              onClick={() => startEditing(note)}
            />
            <MutateButton
              appearance="inline"
              text={i18n.common.remove}
              icon={faTrash}
              mutation={unitServiceWorkerNoteMutation}
              onClick={() => ({ daycareId: unitId, body: { note: '' } })}
              onSuccess={closeEditor}
            />
          </FixedSpaceRow>
        </>
      )}
    </FixedSpaceColumn>
  ))
})
