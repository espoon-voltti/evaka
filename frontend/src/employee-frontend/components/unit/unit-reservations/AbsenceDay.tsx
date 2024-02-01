// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { AbsenceType } from 'lib-common/generated/api-types/absence'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { absenceColors, absenceIcons } from 'lib-customizations/common'

import { useTranslation } from '../../../state/i18n'

interface Props {
  type: AbsenceType
  staffCreated: boolean
}

export default React.memo(function AbsenceDay({ type, staffCreated }: Props) {
  const { i18n } = useTranslation()
  return (
    <FixedSpaceRow spacing="xs" alignItems="center" data-qa="absence">
      <AbsenceTooltip
        tooltip={
          staffCreated && i18n.unit.attendanceReservations.createdByEmployee
        }
      >
        <RoundIcon
          content={absenceIcons[type]}
          color={absenceColors[type]}
          size="m"
        />
        <span>
          {i18n.absences.absenceTypesShort[type]}
          {staffCreated && '*'}
        </span>
      </AbsenceTooltip>
    </FixedSpaceRow>
  )
})

const AbsenceTooltip = styled(Tooltip)`
  display: flex;
  justify-content: space-evenly;
  padding: 8px;
  gap: 8px;
`
