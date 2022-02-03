// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { AbsenceType } from 'lib-common/generated/api-types/daycare'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { LabelLike } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { absenceColors, absenceIcons } from 'lib-customizations/common'
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
  'FORCE_MAJEURE'
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
  absenceTypes?: CalendarAbsenceType[]
  icons?: boolean
}

export const AbsenceLegend = React.memo(function AbsenceLegend({
  absenceTypes = absenceTypesInLegend,
  icons = false
}: Props) {
  const { i18n } = useTranslation()
  return (
    <>
      {absenceTypes.map((t) => (
        <AbsenceLegendRow key={t}>
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
      ))}
    </>
  )
})
