// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'

import { Success } from 'lib-common/api'
import { AssistanceFactorResponse } from 'lib-common/generated/api-types/assistance'
import { useMutationResult } from 'lib-common/query'
import { formatDecimal } from 'lib-common/utils/number'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Td, Tr } from 'lib-components/layout/Table'

import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { getStatusLabelByDateRange } from '../../../utils/date'
import StatusLabel from '../../common/StatusLabel'
import Toolbar from '../../common/Toolbar'
import { deleteAssistanceFactorMutation } from '../queries'

import { DeleteConfirmation } from './DeleteConfirmation'

interface Props {
  assistanceFactor: AssistanceFactorResponse
  onEdit: () => void
}

export const AssistanceFactorRow = React.memo(function AssistanceFactorRow({
  assistanceFactor: {
    data: { capacityFactor, childId, id, modified, modifiedBy, validDuring },
    permittedActions
  },
  onEdit
}: Props) {
  const { i18n } = useTranslation()
  const t = i18n.childInformation.assistance
  const { uiMode, toggleUiMode } = useContext(UIContext)
  const { mutateAsync: deleteAssistanceFactor } = useMutationResult(
    deleteAssistanceFactorMutation
  )

  return (
    <Tr data-qa="assistance-factor-row">
      <Td data-qa="capacity-factor">{formatDecimal(capacityFactor)}</Td>
      <Td data-qa="valid-during">{validDuring.format()}</Td>
      <Td>
        <StatusLabel
          status={getStatusLabelByDateRange({
            startDate: validDuring.start,
            endDate: validDuring.end
          })}
        />
      </Td>
      <Td data-qa="modified-by">
        {modified ? (
          <Tooltip
            tooltip={
              modifiedBy ? t.fields.lastModifiedBy(modifiedBy.name) : null
            }
            position="left"
          >
            {modified.format()}
          </Tooltip>
        ) : (
          t.unknown
        )}
      </Td>
      <Td>
        {uiMode === `remove-assistance-factor-${id}` && (
          <DeleteConfirmation
            title={t.assistanceFactor.removeConfirmation}
            range={validDuring.asDateRange()}
            onSubmit={() =>
              deleteAssistanceFactor({ id, childId }).then(() => Success.of())
            }
          />
        )}
        <Toolbar
          dataQaEdit="edit"
          dataQaDelete="delete"
          onEdit={onEdit}
          editable={permittedActions.includes('UPDATE')}
          onDelete={() => toggleUiMode(`remove-assistance-factor-${id}`)}
          deletable={permittedActions.includes('DELETE')}
        />
      </Td>
    </Tr>
  )
})
