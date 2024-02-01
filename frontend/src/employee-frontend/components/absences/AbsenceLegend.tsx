// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import styled from 'styled-components'

import { AbsenceType } from 'lib-common/generated/api-types/absence'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { LabelLike } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import {
  absenceColors,
  absenceIcons,
  additionalLegendItemColors,
  additionalLegendItemIcons
} from 'lib-customizations/common'
import { absenceTypes } from 'lib-customizations/employee'

import { useTranslation } from '../../state/i18n'

type LegendItemType =
  | AbsenceType
  | 'TEMPORARY_RELOCATION'
  | 'NO_ABSENCE'
  | AdditionalLegendItem
type AdditionalLegendItem = 'CONTRACT_DAYS'

const allLegendItems: LegendItemType[] = [
  'NO_ABSENCE',
  'OTHER_ABSENCE',
  'UNKNOWN_ABSENCE',
  'PLANNED_ABSENCE',
  'SICKLEAVE',
  'TEMPORARY_RELOCATION',
  'PARENTLEAVE',
  'FORCE_MAJEURE',
  'FREE_ABSENCE',
  'UNAUTHORIZED_ABSENCE',
  'CONTRACT_DAYS'
]

const allAbsenceTypes: LegendItemType[] = [
  ...absenceTypes,
  'TEMPORARY_RELOCATION'
]
const allAdditionalItems: LegendItemType[] = ['CONTRACT_DAYS']

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
  showAdditionalLegendItems?: boolean
}

export const AbsenceLegend = React.memo(function AbsenceLegend({
  showNoAbsence = false,
  icons = false,
  showAdditionalLegendItems = false
}: Props) {
  const { i18n } = useTranslation()

  const visibleAbsenceTypes = useMemo(
    () =>
      allLegendItems.filter((a) => {
        if (a === 'NO_ABSENCE') return showNoAbsence
        if (allAdditionalItems.includes(a)) return showAdditionalLegendItems
        else return allAbsenceTypes.includes(a)
      }),
    [showNoAbsence, showAdditionalLegendItems]
  )

  const allLegendColors = { ...absenceColors, ...additionalLegendItemColors }
  const allLegendInfos = {
    ...i18n.absences.absenceTypeInfo,
    ...i18n.absences.additionalLegendItemInfos
  }
  const allLegendIcons = { ...absenceIcons, ...additionalLegendItemIcons }
  const allLegendLabels = {
    ...i18n.absences.absenceTypes,
    ...i18n.absences.additionalLegendItems
  }

  return (
    <>
      {visibleAbsenceTypes.map((t) => (
        <ExpandingInfo key={t} info={allLegendInfos[t]}>
          <AbsenceLegendRow>
            {icons ? (
              <RoundIcon
                size="m"
                color={allLegendColors[t]}
                content={allLegendIcons[t]}
              />
            ) : (
              <AbsenceLegendSquare color={allLegendColors[t]} />
            )}

            <LabelLike>{allLegendLabels[t]}</LabelLike>
          </AbsenceLegendRow>
        </ExpandingInfo>
      ))}
    </>
  )
})
