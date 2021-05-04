// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Td, Tr } from 'lib-components/layout/Table'
import Tooltip from 'lib-components/atoms/Tooltip'
import { NewServiceNeed } from '../../../../types/child'
import { useTranslation } from '../../../../state/i18n'
import { formatDate } from '../../../../utils/date'
import { DATE_FORMAT_DATE_TIME } from '../../../../constants'
import Toolbar from '../../../common/Toolbar'

interface NewServiceNeedReadRowProps {
  serviceNeed: NewServiceNeed
  onEdit: () => void
  onDelete: () => void
  disabled?: boolean
}

function NewServiceNeedReadRow({
  serviceNeed,
  onEdit,
  onDelete,
  disabled
}: NewServiceNeedReadRowProps) {
  const { i18n } = useTranslation()
  return (
    <Tr>
      <Td>
        {serviceNeed.startDate.format()} - {serviceNeed.endDate.format()}
      </Td>
      <Td>{serviceNeed.option.name}</Td>
      <Td>{serviceNeed.shiftCare ? i18n.common.yes : i18n.common.no}</Td>
      <Td>
        <Tooltip
          tooltip={
            <span>
              {serviceNeed.confirmed.lastName} {serviceNeed.confirmed.firstName}
            </span>
          }
        >
          {formatDate(serviceNeed.confirmed.at, DATE_FORMAT_DATE_TIME)}
        </Tooltip>
      </Td>
      <Td>
        <Toolbar
          dateRange={serviceNeed}
          onEdit={onEdit}
          editableFor={['ADMIN', 'UNIT_SUPERVISOR']}
          onDelete={onDelete}
          deletableFor={['ADMIN', 'UNIT_SUPERVISOR']}
          disableAll={disabled}
        />
      </Td>
    </Tr>
  )
}

export default NewServiceNeedReadRow
