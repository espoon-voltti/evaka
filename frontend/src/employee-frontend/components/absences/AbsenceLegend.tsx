// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { AbsenceType } from 'lib-common/generated/enums'
import { Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { absenceColors } from 'lib-customizations/common'
import { useTranslation } from '../../state/i18n'

type CalendarAbsenceType = AbsenceType | 'NO_ABSENCE'
const absenceTypesInLegend: CalendarAbsenceType[] = [
  'NO_ABSENCE',
  'OTHER_ABSENCE',
  'UNKNOWN_ABSENCE',
  'PLANNED_ABSENCE',
  'SICKLEAVE',
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

export const AbsenceLegend = React.memo(function AbsenceLegend() {
  const { i18n } = useTranslation()
  return (
    <>
      {absenceTypesInLegend.map((t) => (
        <AbsenceLegendRow key={t}>
          <AbsenceLegendSquare color={absenceColors[t]} />
          <Label>{i18n.absences.absenceTypes[t]}</Label>
        </AbsenceLegendRow>
      ))}
    </>
  )
})
