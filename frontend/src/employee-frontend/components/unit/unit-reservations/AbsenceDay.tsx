// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import styled from 'styled-components'

import type { AbsenceCategory } from 'lib-common/generated/api-types/absence'
import type { AbsenceTypeResponse } from 'lib-common/generated/api-types/reservations'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Tooltip from 'lib-components/atoms/Tooltip'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { absenceColors, absenceIcons } from 'lib-customizations/common'

import { useTranslation } from '../../../state/i18n'
import { AbsencesTooltipContent } from '../../absences/UnitCalendarDayCellTooltip'

export interface AbsenceWithCategory {
  category: AbsenceCategory
  absence: AbsenceTypeResponse
}

interface Props {
  absences: AbsenceWithCategory[]
}

export default React.memo(function AbsenceDay({ absences }: Props) {
  const { i18n } = useTranslation()

  const tooltipItems = useMemo(
    () =>
      absences.map(({ category, absence }) => ({
        category,
        absenceType: absence.absenceType,
        modifiedAt: absence.modifiedAt,
        modifiedByStaff: absence.staffCreated
      })),
    [absences]
  )

  const first = absences[0]
  if (first === undefined) return null

  const { absenceType, staffCreated } = first.absence
  return (
    <FixedSpaceRow $spacing="xs" $alignItems="center" data-qa="absence">
      <AbsenceTooltip
        tooltip={<AbsencesTooltipContent absences={tooltipItems} />}
        width="large"
        data-qa="absence-tooltip"
      >
        <RoundIcon
          content={absenceIcons[absenceType]}
          color={absenceColors[absenceType]}
          size="m"
        />
        <span>
          {i18n.absences.absenceTypesShort[absenceType]}
          {staffCreated && '*'}
        </span>
      </AbsenceTooltip>
    </FixedSpaceRow>
  )
})

const AbsenceTooltip = styled(Tooltip)`
  display: flex;
  flex-direction: column;
  justify-content: space-evenly;
  padding: 8px;
  gap: 8px;
`
