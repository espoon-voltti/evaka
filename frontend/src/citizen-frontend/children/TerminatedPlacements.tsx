// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ChildPlacement } from 'lib-common/generated/api-types/placement'
import React from 'react'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { Label } from 'lib-components/typography'
import { useTranslation } from '../localization'

interface Props {
  placements: ChildPlacement[]
}

export default React.memo(function TerminatedPlacements({ placements }: Props) {
  const t = useTranslation()
  return (
    <div>
      <HorizontalLine slim />
      <Label>{t.children.placementTermination.terminatedPlacements}</Label>
      <ul>
        {placements.map((p) => (
          <li key={p.placementId}>
            {t.placement.type[p.placementType]}, {p.placementUnitName},
            {t.children.placementTermination.lastDayOfPresence}:{' '}
            {p.placementEndDate.format()}
          </li>
        ))}
      </ul>
    </div>
  )
})
