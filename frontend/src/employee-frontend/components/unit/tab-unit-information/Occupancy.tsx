// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction, useState } from 'react'
import styled from 'styled-components'

import {
  Caretakers,
  DaycareGroupResponse
} from 'lib-common/generated/api-types/daycare'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Select from 'lib-components/atoms/dropdowns/Select'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H3, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import OccupancyCard from '../../../components/unit/tab-unit-information/occupancy/OccupancyCard'
import OccupancyGraph from '../../../components/unit/tab-unit-information/occupancy/OccupancyGraph'
import OccupancySingleDay from '../../../components/unit/tab-unit-information/occupancy/OccupancySingleDay'
import { useTranslation } from '../../../state/i18n'
import { UnitFilters } from '../../../utils/UnitFilters'
import { renderResult } from '../../async-rendering'
import { DataList } from '../../common/DataList'
import UnitDataFilters from '../UnitDataFilters'
import { unitOccupanciesQuery } from '../queries'

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
  padding-right: ${defaultMargins.X3L};
  margin-bottom: ${defaultMargins.X3L};
`

interface SelectionsState {
  confirmed: boolean
  planned: boolean
  realized: boolean
}

type Props = {
  unitId: UUID
  filters: UnitFilters
  setFilters: Dispatch<SetStateAction<UnitFilters>>
  realtimeStaffAttendanceEnabled: boolean
  shiftCareUnit: boolean
  groups: DaycareGroupResponse[]
}

export default React.memo(function OccupancyContainer({
  unitId,
  filters,
  setFilters,
  realtimeStaffAttendanceEnabled,
  shiftCareUnit,
  groups
}: Props) {
  const { startDate, endDate } = filters
  const { i18n } = useTranslation()
  const [open, setOpen] = useState(true)
  const [groupId, setGroupId] = useState<UUID | null>(null)
  const occupancies = useQueryResult(
    unitOccupanciesQuery({ unitId, from: startDate, to: endDate, groupId })
  )
  const [selections, setSelections] = useState<SelectionsState>({
    confirmed: true,
    planned: true,
    realized: true
  })

  return renderResult(occupancies, (occupancies, isReloading) => (
    <CollapsibleContentArea
      title={<H3 noMargin>{i18n.unit.occupancies}</H3>}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      data-qa="unit-attendances"
      data-isloading={isReloading}
    >
      <FixedSpaceRow alignItems="center">
        <Label>{i18n.unit.filters.title}</Label>
        <UnitDataFilters canEdit filters={filters} setFilters={setFilters} />
      </FixedSpaceRow>
      <Gap size="s" />
      <DataList>
        <div>
          <label>{i18n.unit.info.caretakers.titleLabel}</label>
          <span data-qa="unit-total-caretaker-count">
            <Caretakers caretakers={occupancies.caretakers} />
          </span>
        </div>
      </DataList>
      <Gap size="s" />
      <FixedSpaceRow alignItems="center">
        <Label>{i18n.unit.occupancy.group}</Label>
        <Select
          items={[null, ...groups]}
          selectedItem={
            groupId ? groups.find((g) => g.id === groupId) ?? null : null
          }
          onChange={(g) => setGroupId(g ? g.id : null)}
          getItemValue={(g: DaycareGroupResponse | null) => (g ? g.id : '')}
          getItemLabel={(g) => (g ? g.name : i18n.unit.occupancy.fullUnit)}
        />
      </FixedSpaceRow>
      <Gap />
      <Container data-qa="occupancies">
        {startDate.isEqual(endDate) ? (
          <OccupancySingleDay
            queryDate={startDate}
            occupancies={occupancies.confirmed}
            plannedOccupancies={occupancies.planned}
            realizedOccupancies={occupancies.realized}
            realtimeOccupancies={
              realtimeStaffAttendanceEnabled ? occupancies.realtime : null
            }
            shiftCareUnit={shiftCareUnit}
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
    </CollapsibleContentArea>
  ))
})

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
