// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type { ApplicationSummary } from 'lib-common/generated/api-types/application'
import OrderedList from 'lib-components/atoms/OrderedList'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H4 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'

export default React.memo(function ApplicationCard({
  application
}: {
  application: ApplicationSummary
}) {
  return (
    <Card>
      <FixedSpaceColumn spacing="s">
        <H4 noMargin>
          {application.firstName} {application.lastName}
        </H4>
        <OrderedList spacing="xxs">
          {application.preferredUnits.map((unit) => (
            <li key={unit.id}>{unit.name}</li>
          ))}
        </OrderedList>
      </FixedSpaceColumn>
    </Card>
  )
})

const Card = styled.div`
  width: 400px;
  border: 1px solid ${(p) => p.theme.colors.grayscale.g35};
  border-radius: 4px;
  padding: ${defaultMargins.s};
  background-color: ${(p) => p.theme.colors.grayscale.g0};
`
