// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { TerminatablePlacementGroup } from 'lib-common/generated/api-types/placement'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { Label } from 'lib-components/typography'

import { useTranslation } from '../../../localization'

import { terminatedPlacementInfo } from './utils'

interface Props {
  placements: TerminatablePlacementGroup[]
}

export default React.memo(function TerminatedPlacements({ placements }: Props) {
  const t = useTranslation()
  return (
    <div>
      <HorizontalLine slim />
      <Label>{t.children.placementTermination.terminatedPlacements}</Label>
      <ul>
        {placements.map((placementGroup) => {
          const { type, unitId, unitName, lastDay } =
            terminatedPlacementInfo(placementGroup)
          const typeText =
            type.type === 'placement'
              ? t.placement.type[type.placementType]
              : t.children.placementTermination.invoicedDaycare
          return (
            <li key={`${unitId}-${typeText}`} data-qa="terminated-placement">
              {typeText}, {unitName},{' '}
              {t.children.placementTermination.lastDayOfPresence.toLowerCase()}:{' '}
              {lastDay.format()}
            </li>
          )
        })}
      </ul>
    </div>
  )
})
