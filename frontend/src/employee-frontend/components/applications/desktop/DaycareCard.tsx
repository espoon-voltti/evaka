// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type { PreferredUnit } from 'lib-common/generated/api-types/application'
import { useQueryResult } from 'lib-common/query'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { LabelLike } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

import { renderResult } from '../../async-rendering'
import { getPlacementDesktopDaycareQuery } from '../queries'

export default React.memo(function DaycareCard({
  daycare
}: {
  daycare: PreferredUnit
}) {
  const daycareDetails = useQueryResult(
    getPlacementDesktopDaycareQuery({ daycareId: daycare.id })
  )
  return (
    <Card>
      <FixedSpaceColumn spacing="s">
        <LabelLike>{daycare.name}</LabelLike>
        {renderResult(daycareDetails, (details) => (
          <span>{details.foo}</span>
        ))}
      </FixedSpaceColumn>
    </Card>
  )
})

const Card = styled.div`
  width: 580px;
  border: 1px solid ${(p) => p.theme.colors.grayscale.g35};
  border-radius: 4px;
  padding: ${defaultMargins.s};
  background-color: ${(p) => p.theme.colors.grayscale.g0};
`
