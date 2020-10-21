// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import 'chartjs-adapter-date-fns'
import { UnitOccupancies } from '~api/unit'
import OccupancyCard from '~components/unit/tab-unit-information/occupancy/OccupancyCard'
import OccupancyGraph from '~components/unit/tab-unit-information/occupancy/OccupancyGraph'
import OccupancySingleDay from '~components/unit/tab-unit-information/occupancy/OccupancySingleDay'
import { UnitFilters } from 'utils/UnitFilters'
import { FixedSpaceColumn } from '~components/shared/layout/flex-helpers'
import { DefaultMargins } from '~components/shared/layout/white-space'

const Container = styled.div`
  display: flex;
  flex-direction: column;
`

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
  padding-right: ${DefaultMargins.X3L};
  margin-bottom: ${DefaultMargins.X3L};
`

interface SelectionsState {
  confirmed: boolean
  planned: boolean
  realized: boolean
}

type Props = {
  filters: UnitFilters
  occupancies: UnitOccupancies
}

export default React.memo(function OccupancyContainer({
  filters,
  occupancies
}: Props) {
  const { startDate, endDate } = filters

  const [selections, setSelections] = useState<SelectionsState>({
    confirmed: true,
    planned: true,
    realized: true
  })

  return (
    <Container data-qa="occupancies">
      {startDate.isEqual(endDate) ? (
        <OccupancySingleDay
          occupancies={occupancies.confirmed}
          plannedOccupancies={occupancies.planned}
          realizedOccupancies={occupancies.confirmed}
        />
      ) : (
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
              startDate={startDate.toSystemTzDate()}
              endDate={endDate.toSystemTzDate()}
            />
          </GraphWrapper>
        </WrapBox>
      )}
    </Container>
  )
})
