// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'

import { Success } from 'lib-common/api'
import { DaycareAssistanceResponse } from 'lib-common/generated/api-types/assistance'
import { useMutationResult } from 'lib-common/query'
import { Td, Tr } from 'lib-components/layout/Table'

import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { getStatusLabelByDateRange } from '../../../utils/date'
import StatusLabel from '../../common/StatusLabel'
import Toolbar from '../../common/Toolbar'
import { deleteDaycareAssistanceMutation } from '../queries'

import { DeleteConfirmation } from './DeleteConfirmation'

interface Props {
  daycareAssistance: DaycareAssistanceResponse
  onEdit: () => void
}

export const DaycareAssistanceRow = React.memo(function DaycareAssistanceRow({
  daycareAssistance: { data, permittedActions },
  onEdit
}: Props) {
  const { i18n } = useTranslation()
  const t = i18n.childInformation.assistance
  const { uiMode, toggleUiMode } = useContext(UIContext)
  const { mutateAsync: deleteDaycareAssistance } = useMutationResult(
    deleteDaycareAssistanceMutation
  )
  return (
    <Tr data-qa="daycare-assistance-row">
      <Td data-qa="level">{t.types.daycareAssistanceLevel[data.level]}</Td>
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
        {uiMode === `remove-daycare-assistance-${data.id}` && (
          <DeleteConfirmation
            title={t.daycareAssistance.removeConfirmation}
            range={data.validDuring.asDateRange()}
            onSubmit={() =>
              deleteDaycareAssistance({
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
          onDelete={() => toggleUiMode(`remove-daycare-assistance-${data.id}`)}
          deletable={permittedActions.includes('DELETE')}
        />
      </Td>
    </Tr>
  )
})
