// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  Dispatch,
  SetStateAction,
  useCallback,
  useMemo,
  useState
} from 'react'
import styled from 'styled-components'

import { DaycareGroupResponse } from 'lib-common/generated/api-types/daycare'
import { UUID } from 'lib-common/types'
import Select from 'lib-components/atoms/dropdowns/Select'
import TreeDropdown, {
  TreeNode
} from 'lib-components/atoms/dropdowns/TreeDropdown'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H3, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { Translations, useTranslation } from '../../../state/i18n'
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
  const [groupIds, setGroupIds] = useState<UUID[] | null>(null)
  const [mode, setMode] = useState<DayGraphMode>('PLANNED')

  const activeGroups = useMemo(
    () =>
      groups.filter(
        (group) =>
          group.endDate === null || group.endDate.isEqualOrAfter(endDate)
      ),
    [groups, endDate]
  )
  const tree = useMemo(
    () => groupsToTree(i18n, activeGroups, groupIds),
    [i18n, activeGroups, groupIds]
  )
  const handleTreeChange = useCallback((newTree: TreeNode[]) => {
    setGroupIds(treeToGroupIds(newTree))
  }, [])

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
        {startDate.isEqual(endDate) && realtimeStaffAttendanceEnabled ? (
          <>
            <Label>{i18n.unit.occupancy.display}</Label>
            <GroupSelectWrapper>
              <TreeDropdown
                tree={tree}
                onChange={handleTreeChange}
                placeholder={i18n.common.select}
              />
            </GroupSelectWrapper>
            <Select
              items={dayGraphModes}
              selectedItem={mode}
              onChange={(newMode) => {
                if (newMode !== null) setMode(newMode)
              }}
              getItemLabel={(item) => i18n.unit.occupancy.realtime.modes[item]}
              data-qa="graph-mode-select"
            />
          </>
        ) : (
          <Select
            items={[null, ...activeGroups]}
            selectedItem={
              groupIds !== null && groupIds.length > 0
                ? activeGroups.find((g) => g.id === groupIds[0]) ?? null
                : null
            }
            onChange={(g) => setGroupIds(g ? [g.id] : null)}
            getItemValue={(g: DaycareGroupResponse | null) => (g ? g.id : '')}
            getItemLabel={(g) => (g ? g.name : i18n.unit.occupancy.fullUnit)}
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
                  groupIds={groupIds}
                  date={startDate}
                  shiftCareUnit={shiftCareUnit}
                />
              )}
              {mode === 'PLANNED' && (
                <RealtimePlannedOccupanciesForSingleDay
                  unitId={unitId}
                  groupIds={groupIds}
                  date={startDate}
                />
              )}
            </div>
          ) : (
            <SimpleOccupanciesForSingleDay
              unitId={unitId}
              groupId={groupIds !== null ? groupIds[0] ?? null : null}
              date={startDate}
            />
          )
        ) : (
          <OccupanciesForDateRange
            unitId={unitId}
            groupId={groupIds !== null ? groupIds[0] ?? null : null}
            from={startDate}
            to={endDate}
          />
        )}
      </Container>
    </CollapsibleContentArea>
  )
})

const GroupSelectWrapper = styled.div`
  min-width: 300px;
`

function groupsToTree(
  i18n: Translations,
  groups: DaycareGroupResponse[],
  selectedGroupIds: UUID[] | null
): TreeNode[] {
  return [
    {
      text: i18n.unit.occupancy.fullUnit,
      key: 'unit',
      checked: selectedGroupIds === null || selectedGroupIds.length > 0,
      children: groups.map((group) => ({
        text: group.name,
        key: group.id,
        checked:
          selectedGroupIds === null || selectedGroupIds.includes(group.id),
        children: []
      }))
    }
  ]
}

function treeToGroupIds(tree: TreeNode[]): UUID[] | null {
  const root = tree[0]
  const checkedGroupIds = root.children
    .filter((child) => child.checked)
    .map((child) => child.key)
  if (checkedGroupIds.length === root.children.length) {
    return null
  }
  return checkedGroupIds
}
