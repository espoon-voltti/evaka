// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { TerminatablePlacementGroup } from 'lib-common/generated/api-types/placement'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { Label } from 'lib-components/typography'
import React from 'react'
import { useTranslation } from '../localization'

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
        {placements.map((p) => (
          <li key={`${p.unitId}-${p.type}`}>
            {t.placement.type[p.type]}, {p.unitName}
            {', '}
            {t.children.placementTermination.lastDayOfPresence.toLowerCase()}:{' '}
            {p.endDate.format()}
          </li>
        ))}
      </ul>
    </div>
  )
})
