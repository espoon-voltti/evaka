// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'

import { Success } from 'lib-common/api'
import type { PreschoolAssistanceResponse } from 'lib-common/generated/api-types/assistance'
import { useMutationResult } from 'lib-common/query'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Td, Tr } from 'lib-components/layout/Table'

import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { getStatusLabelByDateRange } from '../../../utils/date'
import StatusLabel from '../../common/StatusLabel'
import Toolbar from '../../common/Toolbar'
import { deletePreschoolAssistanceMutation } from '../queries'

import { DeleteConfirmation } from './DeleteConfirmation'

interface Props {
  preschoolAssistance: PreschoolAssistanceResponse
  onEdit: () => void
}

export const PreschoolAssistanceRow = React.memo(
  function PreschoolAssistanceRow({
    preschoolAssistance: {
      data: { id, childId, validDuring, level, modified, modifiedBy },
      permittedActions
    },
    onEdit
  }: Props) {
    const { i18n } = useTranslation()
    const t = i18n.childInformation.assistance
    const { uiMode, toggleUiMode } = useContext(UIContext)
    const { mutateAsync: deletePreschoolAssistance } = useMutationResult(
      deletePreschoolAssistanceMutation
    )
    return (
      <Tr data-qa="preschool-assistance-row">
        <Td data-qa="valid-during">{validDuring.format()}</Td>
        <Td data-qa="level">{t.types.preschoolAssistanceLevel[level]}</Td>
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
          {uiMode === `remove-preschool-assistance-${id}` && (
            <DeleteConfirmation
              title={t.preschoolAssistance.removeConfirmation}
              range={validDuring.asDateRange()}
              onSubmit={() =>
                deletePreschoolAssistance({
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
            onDelete={() => toggleUiMode(`remove-preschool-assistance-${id}`)}
            deletable={permittedActions.includes('DELETE')}
          />
        </Td>
      </Tr>
    )
  }
)
