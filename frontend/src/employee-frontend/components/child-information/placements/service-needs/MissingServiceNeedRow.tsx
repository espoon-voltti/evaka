// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faExclamationTriangle } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import LocalDate from 'lib-common/local-date'
import { Button } from 'lib-components/atoms/buttons/Button'
import { Td, Tr } from 'lib-components/layout/Table'
import colors from 'lib-customizations/common'
import { faPlus } from 'lib-icons'

import { useTranslation } from '../../../../state/i18n'

interface MissingServiceNeedRowProps {
  createAllowed: boolean
  startDate: LocalDate
  endDate: LocalDate
  onEdit: () => void
  disabled?: boolean
}
function MissingServiceNeedRow({
  createAllowed,
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
          color={colors.status.warning}
        />
      </InfoTd>
      <Td />
      <Td />
      <Td />
      <Td align="right">
        {createAllowed && (
          <Button
            appearance="inline"
            onClick={onEdit}
            text={t.addNewBtn}
            icon={faPlus}
            disabled={disabled}
            data-qa="add-new-missing-service-need"
          />
        )}
      </Td>
    </Tr>
  )
}

const InfoTd = styled(Td)`
  vertical-align: middle;
  color: ${colors.grayscale.g70};
  font-style: italic;
`

export default MissingServiceNeedRow
