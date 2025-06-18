// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'

import { Success } from 'lib-common/api'
import type { OtherAssistanceMeasureResponse } from 'lib-common/generated/api-types/assistance'
import { useMutationResult } from 'lib-common/query'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Td, Tr } from 'lib-components/layout/Table'

import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { getStatusLabelByDateRange } from '../../../utils/date'
import StatusLabel from '../../common/StatusLabel'
import Toolbar from '../../common/Toolbar'
import { deleteOtherAssistanceMeasureMutation } from '../queries'

import { DeleteConfirmation } from './DeleteConfirmation'

interface Props {
  otherAssistanceMeasure: OtherAssistanceMeasureResponse
  onEdit: () => void
}

export const OtherAssistanceMeasureRow = React.memo(
  function OtherAssistanceMeasureRow({
    otherAssistanceMeasure: {
      data: { id, childId, validDuring, type, modifiedBy, modified },
      permittedActions
    },
    onEdit
  }: Props) {
    const { i18n } = useTranslation()
    const t = i18n.childInformation.assistance
    const { uiMode, toggleUiMode } = useContext(UIContext)
    const { mutateAsync: deleteOtherAssistanceMeasure } = useMutationResult(
      deleteOtherAssistanceMeasureMutation
    )
    return (
      <Tr data-qa="other-assistance-measure-row">
        <Td data-qa="valid-during">{validDuring.format()}</Td>
        <Td data-qa="type">{t.types.otherAssistanceMeasureType[type]}</Td>
        <Td data-qa="modified">
          <Tooltip
            tooltip={t.fields.lastModifiedBy(modifiedBy.name)}
            position="left"
          >
            {modified.format()}
          </Tooltip>
        </Td>
        <Td>
          <StatusLabel
            status={getStatusLabelByDateRange({
              startDate: validDuring.start,
              endDate: validDuring.end
            })}
          />
        </Td>
        <Td>
          {uiMode === `remove-other-assistance-measure-${id}` && (
            <DeleteConfirmation
              title={t.otherAssistanceMeasure.removeConfirmation}
              range={validDuring.asDateRange()}
              onSubmit={() =>
                deleteOtherAssistanceMeasure({
                  id: id,
                  childId: childId
                }).then(() => Success.of())
              }
            />
          )}
          <Toolbar
            dataQaEdit="edit"
            dataQaDelete="delete"
            onEdit={onEdit}
            editable={permittedActions.includes('UPDATE')}
            onDelete={() =>
              toggleUiMode(`remove-other-assistance-measure-${id}`)
            }
            deletable={permittedActions.includes('DELETE')}
          />
        </Td>
      </Tr>
    )
  }
)
