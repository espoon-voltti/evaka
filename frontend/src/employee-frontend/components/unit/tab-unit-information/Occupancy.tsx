// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction, useState } from 'react'
import styled from 'styled-components'

import { DaycareGroupResponse } from 'lib-common/generated/api-types/daycare'
import { UUID } from 'lib-common/types'
import Select from 'lib-components/atoms/dropdowns/Select'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H3, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../../state/i18n'
import { UnitFilters } from '../../../utils/UnitFilters'
import UnitDataFilters from '../UnitDataFilters'

import OccupanciesForDateRange from './occupancy/OccupanciesForDateRange'
import {
  RealtimeOccupanciesForSingleDay,
  SimpleOccupanciesForSingleDay
} from './occupancy/OccupanciesForSingleDay'

const Container = styled.div`
  display: flex;
  flex-direction: column;
`

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

  return (
    <CollapsibleContentArea
      title={<H3 noMargin>{i18n.unit.occupancies}</H3>}
      open={open}
      toggleOpen={() => setOpen(!open)}
      opaque
      data-qa="unit-attendances"
    >
      <FixedSpaceRow alignItems="center">
        <Label>{i18n.unit.filters.title}</Label>
        <UnitDataFilters canEdit filters={filters} setFilters={setFilters} />
      </FixedSpaceRow>
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
          realtimeStaffAttendanceEnabled ? (
            <RealtimeOccupanciesForSingleDay
              unitId={unitId}
              groupId={groupId}
              date={startDate}
              shiftCareUnit={shiftCareUnit}
            />
          ) : (
            <SimpleOccupanciesForSingleDay
              unitId={unitId}
              groupId={groupId}
              date={startDate}
            />
          )
        ) : (
          <OccupanciesForDateRange
            unitId={unitId}
            groupId={groupId}
            from={startDate}
            to={endDate}
          />
        )}
      </Container>
    </CollapsibleContentArea>
  )
})
