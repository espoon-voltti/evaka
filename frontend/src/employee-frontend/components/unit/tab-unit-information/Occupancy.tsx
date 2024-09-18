// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction, useMemo, useState } from 'react'
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
  RealtimePlannedOccupanciesForSingleDay,
  RealtimeRealizedOccupanciesForSingleDay,
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

const dayGraphModes = ['REALIZED', 'PLANNED'] as const
type DayGraphMode = (typeof dayGraphModes)[number]

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
  const [mode, setMode] = useState<DayGraphMode>('PLANNED')

  const activeGroups = useMemo(
    () =>
      groups.filter(
        (group) =>
          group.endDate === null || group.endDate.isEqualOrAfter(endDate)
      ),
    [endDate, groups]
  )

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
        <Label>{i18n.unit.occupancy.display}</Label>
        <Select
          items={[null, ...activeGroups]}
          selectedItem={
            groupId ? activeGroups.find((g) => g.id === groupId) ?? null : null
          }
          onChange={(g) => setGroupId(g ? g.id : null)}
          getItemValue={(g: DaycareGroupResponse | null) => (g ? g.id : '')}
          getItemLabel={(g) => (g ? g.name : i18n.unit.occupancy.fullUnit)}
        />
        {startDate.isEqual(endDate) && realtimeStaffAttendanceEnabled && (
          <Select
            items={dayGraphModes}
            selectedItem={mode}
            onChange={(newMode) => {
              if (newMode !== null) setMode(newMode)
            }}
            getItemLabel={(item) => i18n.unit.occupancy.realtime.modes[item]}
          />
        )}
      </FixedSpaceRow>
      <Gap />
      <Container data-qa="occupancies">
        {startDate.isEqual(endDate) ? (
          realtimeStaffAttendanceEnabled ? (
            <div>
              {mode === 'REALIZED' && (
                <RealtimeRealizedOccupanciesForSingleDay
                  unitId={unitId}
                  groupId={groupId}
                  date={startDate}
                  shiftCareUnit={shiftCareUnit}
                />
              )}
              {mode === 'PLANNED' && (
                <RealtimePlannedOccupanciesForSingleDay
                  unitId={unitId}
                  groupId={groupId}
                  date={startDate}
                />
              )}
            </div>
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
