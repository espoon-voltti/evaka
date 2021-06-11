// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faExclamationTriangle } from '@fortawesome/free-solid-svg-icons'
import { faPlus } from 'lib-icons'
import LocalDate from 'lib-common/local-date'
import { Td, Tr } from 'lib-components/layout/Table'
import colors from 'lib-customizations/common'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { useTranslation } from '../../../../state/i18n'
import { RequireRole } from '../../../../utils/roles'

interface MissingServiceNeedRowProps {
  startDate: LocalDate
  endDate: LocalDate
  onEdit: () => void
  disabled?: boolean
}
function MissingServiceNeedRow({
  startDate,
  endDate,
  onEdit,
  disabled
}: MissingServiceNeedRowProps) {
  const { i18n } = useTranslation()
  const t = i18n.childInformation.placements.serviceNeeds
  return (
    <Tr>
      <InfoTd>
        {startDate.format()} - {endDate.format()}
      </InfoTd>
      <InfoTd>
        {t.missing}{' '}
        <FontAwesomeIcon
          icon={faExclamationTriangle}
          color={colors.accents.orange}
        />
      </InfoTd>
      <Td />
      <Td />
      <Td align="right">
        <RequireRole oneOf={['ADMIN', 'UNIT_SUPERVISOR']}>
          <InlineButton
            onClick={onEdit}
            text={t.addNewBtn}
            icon={faPlus}
            disabled={disabled}
          />
        </RequireRole>
      </Td>
    </Tr>
  )
}

const InfoTd = styled(Td)`
  vertical-align: middle;
  color: ${colors.greyscale.dark};
  font-style: italic;
`

export default MissingServiceNeedRow
