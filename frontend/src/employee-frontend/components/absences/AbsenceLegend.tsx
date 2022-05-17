// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import styled from 'styled-components'

import { AbsenceType } from 'lib-common/generated/api-types/daycare'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { LabelLike } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { absenceColors, absenceIcons } from 'lib-customizations/common'
import { absenceTypes } from 'lib-customizations/employee'

import { useTranslation } from '../../state/i18n'

type CalendarAbsenceType = AbsenceType | 'TEMPORARY_RELOCATION' | 'NO_ABSENCE'

const absenceTypesInLegend: CalendarAbsenceType[] = [
  'NO_ABSENCE',
  'OTHER_ABSENCE',
  'UNKNOWN_ABSENCE',
  'PLANNED_ABSENCE',
  'SICKLEAVE',
  'TEMPORARY_RELOCATION',
  'PARENTLEAVE',
  'FORCE_MAJEURE',
  'FREE_ABSENCE',
  'UNAUTHORIZED_ABSENCE'
]

const AbsenceLegendRow = styled.div`
  display: flex;
  align-items: center;
  gap: ${defaultMargins.s};
`

const AbsenceLegendSquare = styled.div<{ color: string }>`
  height: 20px;
  width: 20px;
  background-color: ${(p) => p.color};
  border-radius: 2px;
`

interface Props {
  showNoAbsence?: boolean
  icons?: boolean
}

export const AbsenceLegend = React.memo(function AbsenceLegend({
  showNoAbsence = false,
  icons = false
}: Props) {
  const { i18n } = useTranslation()

  const visibleAbsenceTypes = useMemo(
    () =>
      absenceTypesInLegend.filter((a) =>
        a === 'NO_ABSENCE'
          ? showNoAbsence
          : a === 'TEMPORARY_RELOCATION' || absenceTypes.includes(a)
      ),
    [showNoAbsence]
  )

  return (
    <>
      {visibleAbsenceTypes.map((t) => (
        <ExpandingInfo
          key={t}
          info={i18n.absences.absenceTypeInfo[t]}
          ariaLabel={i18n.common.openExpandingInfo}
        >
          <AbsenceLegendRow>
            {icons ? (
              <RoundIcon
                size="m"
                color={absenceColors[t]}
                content={absenceIcons[t]}
              />
            ) : (
              <AbsenceLegendSquare color={absenceColors[t]} />
            )}

            <LabelLike>{i18n.absences.absenceTypes[t]}</LabelLike>
          </AbsenceLegendRow>
        </ExpandingInfo>
      ))}
    </>
  )
})
