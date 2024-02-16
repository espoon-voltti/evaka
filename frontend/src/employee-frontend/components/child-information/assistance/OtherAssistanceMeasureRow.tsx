// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'

import { Success } from 'lib-common/api'
import { OtherAssistanceMeasureResponse } from 'lib-common/generated/api-types/assistance'
import { useMutationResult } from 'lib-common/query'
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
    otherAssistanceMeasure: { data, permittedActions },
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
        <Td data-qa="type">{t.types.otherAssistanceMeasureType[data.type]}</Td>
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
          {uiMode === `remove-other-assistance-measure-${data.id}` && (
            <DeleteConfirmation
              title={t.otherAssistanceMeasure.removeConfirmation}
              range={data.validDuring.asDateRange()}
              onSubmit={() =>
                deleteOtherAssistanceMeasure({
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
              toggleUiMode(`remove-other-assistance-measure-${data.id}`)
            }
            deletable={permittedActions.includes('DELETE')}
          />
        </Td>
      </Tr>
    )
  }
)
