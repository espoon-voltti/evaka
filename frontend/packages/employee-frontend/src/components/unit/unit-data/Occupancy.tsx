// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import 'chartjs-adapter-date-fns'
import { UnitOccupancies } from '~api/unit'
import OccupancyCard from '~components/unit/unit-data/occupancy/OccupancyCard'
import OccupancyGraph from '~components/unit/unit-data/occupancy/OccupancyGraph'
import OccupancySingleDay from '~components/unit/unit-data/occupancy/OccupancySingleDay'
import { UnitFilters } from 'utils/UnitFilters'

const Container = styled.div`
  display: flex;
  flex-direction: column;
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
        <>
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
        </>
      )}
    </Container>
  )
})
