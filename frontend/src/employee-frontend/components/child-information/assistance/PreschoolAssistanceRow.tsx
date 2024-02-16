// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'

import { Success } from 'lib-common/api'
import { PreschoolAssistanceResponse } from 'lib-common/generated/api-types/assistance'
import { useMutationResult } from 'lib-common/query'
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
    preschoolAssistance: { data, permittedActions },
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
        <Td data-qa="level">{t.types.preschoolAssistanceLevel[data.level]}</Td>
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
          {uiMode === `remove-preschool-assistance-${data.id}` && (
            <DeleteConfirmation
              title={t.preschoolAssistance.removeConfirmation}
              range={data.validDuring.asDateRange()}
              onSubmit={() =>
                deletePreschoolAssistance({
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
            onDelete={() =>
              toggleUiMode(`remove-preschool-assistance-${data.id}`)
            }
            deletable={permittedActions.includes('DELETE')}
          />
        </Td>
      </Tr>
    )
  }
)
