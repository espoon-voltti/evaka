// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'

import { Success } from 'lib-common/api'
import { AssistanceFactorResponse } from 'lib-common/generated/api-types/assistance'
import { useMutationResult } from 'lib-common/query'
import { formatDecimal } from 'lib-common/utils/number'
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
  assistanceFactor: { data, permittedActions },
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
      <Td data-qa="capacity-factor">{formatDecimal(data.capacityFactor)}</Td>
      <Td data-qa="valid-during">{data.validDuring.format()}</Td>
      <Td>
        <StatusLabel
          status={getStatusLabelByDateRange({
            startDate: data.validDuring.start,
            endDate: data.validDuring.end
          })}
        />
      </Td>
      <Td>
        {uiMode === `remove-assistance-factor-${data.id}` && (
          <DeleteConfirmation
            title={t.assistanceFactor.removeConfirmation}
            range={data.validDuring.asDateRange()}
            onSubmit={() =>
              deleteAssistanceFactor({
                id: data.id,
                childId: data.childId
              }).then(() => Success.of())
            }
          />
        )}
        <Toolbar
          dataQaEdit="edit"
          dataQaDelete="delete"
          onEdit={onEdit}
          editable={permittedActions.includes('UPDATE')}
          onDelete={() => toggleUiMode(`remove-assistance-factor-${data.id}`)}
          deletable={permittedActions.includes('DELETE')}
        />
      </Td>
    </Tr>
  )
})
