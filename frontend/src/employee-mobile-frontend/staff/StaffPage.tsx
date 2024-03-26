// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'

import { combine, Success } from 'lib-common/api'
import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import { StaffAttendanceUpdate } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import { useApiState } from 'lib-common/utils/useRestApi'
import { ContentArea } from 'lib-components/layout/Container'

import { renderResult } from '../async-rendering'
import { PageWithNavigation } from '../common/PageWithNavigation'
import { useSelectedGroup } from '../common/selected-group'
import { unitInfoQuery } from '../units/queries'

import StaffAttendanceEditor from './StaffAttendanceEditor'
import {
  getRealizedOccupancyToday,
  getUnitStaffAttendances,
  postStaffAttendance
} from './api'
import { staffAttendanceForGroupOrUnit } from './utils'

export default React.memo(function StaffPage() {
  const navigate = useNavigate()
  const { unitId } = useRouteParams(['unitId'])
  const { selectedGroupId } = useSelectedGroup()

  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))

  const selectedGroup = useMemo(
    () =>
      selectedGroupId.type === 'all'
        ? undefined
        : unitInfoResponse
            .map((res) => res.groups.find((g) => g.id === selectedGroupId.id))
            .getOrElse(undefined),
    [selectedGroupId, unitInfoResponse]
  )

  const [staffResponse, reloadStaff] = useApiState(
    () => getUnitStaffAttendances(unitId),
    [unitId]
  )

  const unitOrGroupStaffAttendance = useMemo(
    () =>
      staffResponse.map((staffAttendances) =>
        staffAttendanceForGroupOrUnit(staffAttendances, selectedGroup?.id)
      ),
    [selectedGroup?.id, staffResponse]
  )

  const [occupancy, reloadRealizedOccupancyToday] = useApiState(
    () => getRealizedOccupancyToday(unitId, selectedGroup?.id),
    [unitId, selectedGroup]
  )

  const changeGroup = useCallback(
    (group: GroupInfo | undefined) => {
      const groupId = group === undefined ? 'all' : group.id
      navigate(`/units/${unitId}/groups/${groupId}/staff`)
    },
    [navigate, unitId]
  )

  const updateAttendance = useCallback(
    async (attendance: StaffAttendanceUpdate) => {
      await postStaffAttendance(attendance)
      void reloadStaff()
      void reloadRealizedOccupancyToday()
      return Success.of()
    },
    [reloadRealizedOccupancyToday, reloadStaff]
  )

  const today = useMemo(() => LocalDate.todayInSystemTz(), [])

  return (
    <PageWithNavigation
      selected="staff"
      selectedGroup={selectedGroup}
      onChangeGroup={changeGroup}
    >
      <ContentArea opaque style={{ height: '100%' }}>
        {renderResult(
          combine(unitOrGroupStaffAttendance, occupancy),
          ([staffAttendance, occupancy]) => (
            <StaffAttendanceEditor
              // Force re-render if the selected group changes
              key={selectedGroup?.id}
              groupId={selectedGroup?.id}
              date={today}
              count={staffAttendance.count}
              countOther={staffAttendance.countOther}
              updated={staffAttendance.updated}
              realizedOccupancy={occupancy}
              onConfirm={updateAttendance}
            />
          )
        )}
      </ContentArea>
    </PageWithNavigation>
  )
})
