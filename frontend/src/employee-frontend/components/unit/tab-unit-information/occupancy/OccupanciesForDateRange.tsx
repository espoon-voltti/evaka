// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import { Caretakers } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { useTranslation } from '../../../../state/i18n'
import { renderResult } from '../../../async-rendering'
import { DataList } from '../../../common/DataList'
import { unitOccupanciesQuery } from '../../queries'

import OccupancyCard from './OccupancyCard'
import OccupancyGraph from './OccupancyGraph'

const WrapBox = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
`

const GraphWrapper = styled.div`
  min-width: 300px;
  max-width: 500px;
  flex-grow: 1;
`

const CardsWrapper = styled.div`
  width: 50%;
  min-width: 400px;
  padding-right: ${defaultMargins.X3L};
  margin-bottom: ${defaultMargins.X3L};
`

interface SelectionsState {
  confirmed: boolean
  planned: boolean
  realized: boolean
}

const Caretakers = React.memo(function Caretakers({
  caretakers
}: {
  caretakers: Caretakers
}) {
  const { i18n } = useTranslation()

  const formatNumber = (num: number) =>
    parseFloat(num.toFixed(2)).toLocaleString()

  const min = formatNumber(caretakers.minimum)
  const max = formatNumber(caretakers.maximum)

  return min === max ? (
    <span>
      {min} {i18n.unit.info.caretakers.unitOfValue}
    </span>
  ) : (
    <span>{`${min} - ${max} ${i18n.unit.info.caretakers.unitOfValue}`}</span>
  )
})

export default React.memo(function OccupanciesForDateRange({
  unitId,
  groupId,
  from,
  to
}: {
  unitId: UUID
  groupId: UUID | null
  from: LocalDate
  to: LocalDate
}) {
  const { i18n } = useTranslation()

  const [selections, setSelections] = useState<SelectionsState>({
    confirmed: true,
    planned: true,
    realized: true
  })

  const occupancies = useQueryResult(
    unitOccupanciesQuery({ unitId, from, to, groupId })
  )

  return renderResult(occupancies, (occupancies, isReloading) => (
    <div data-qa="unit-attendances" data-isloading={isReloading}>
      <DataList>
        <div>
          <label>{i18n.unit.info.caretakers.titleLabel}</label>
          <span data-qa="unit-total-caretaker-count">
            <Caretakers caretakers={occupancies.caretakers} />
          </span>
        </div>
      </DataList>
      <Gap size="s" />
      <WrapBox>
        <CardsWrapper>
          <FixedSpaceColumn>
            <OccupancyCard
              type="confirmed"
              data={occupancies.confirmed}
              active={selections.confirmed}
              onClick={() =>
                setSelections({
                  ...selections,
                  confirmed: !selections.confirmed
                })
              }
            />
            <OccupancyCard
              type="planned"
              data={occupancies.planned}
              active={selections.planned}
              onClick={() =>
                setSelections({
                  ...selections,
                  planned: !selections.planned
                })
              }
            />
            <OccupancyCard
              type="realized"
              data={occupancies.realized}
              active={selections.realized}
              onClick={() =>
                setSelections({
                  ...selections,
                  realized: !selections.realized
                })
              }
            />
          </FixedSpaceColumn>
        </CardsWrapper>
        <GraphWrapper>
          <OccupancyGraph
            occupancies={occupancies.confirmed}
            plannedOccupancies={occupancies.planned}
            realizedOccupancies={occupancies.realized}
            confirmed={selections.confirmed}
            planned={selections.planned}
            realized={selections.realized}
            startDate={from.toSystemTzDate()}
            endDate={to.toSystemTzDate()}
          />
        </GraphWrapper>
      </WrapBox>
    </div>
  ))
})
