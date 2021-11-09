// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { combine, Success } from 'lib-common/api'
import { GroupInfo } from 'lib-common/generated/api-types/attendance'
import { StaffAttendanceUpdate } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { ContentArea } from 'lib-components/layout/Container'
import React, { useCallback, useContext, useMemo } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { getRealizedOccupancyToday } from '../../api/occupancy'
import { getUnitStaffAttendances, postStaffAttendance } from '../../api/staff'
import { UnitContext } from '../../state/unit'
import { staffAttendanceForGroupOrUnit } from '../../utils/staffAttendances'
import { renderResult } from '../async-rendering'
import BottomNavBar from '../common/BottomNavbar'
import { TopBarWithGroupSelector } from '../common/TopBarWithGroupSelector'
import StaffAttendanceEditor from './StaffAttendanceEditor'

export default React.memo(function StaffPage() {
  const history = useHistory()
  const { unitId, groupId: groupIdOrAll } = useParams<{
    unitId: UUID
    groupId: UUID | 'all'
  }>()

  const { unitInfoResponse } = useContext(UnitContext)

  const selectedGroup = useMemo(
    () =>
      groupIdOrAll === 'all'
        ? undefined
        : unitInfoResponse
            .map((res) => res.groups.find((g) => g.id === groupIdOrAll))
            .getOrElse(undefined),
    [groupIdOrAll, unitInfoResponse]
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
      history.push(`/units/${unitId}/groups/${groupId}/staff`)
    },
    [history, unitId]
  )

  const updateAttendance = useCallback(
    async (attendance: StaffAttendanceUpdate) => {
      await postStaffAttendance(attendance)
      reloadStaff()
      reloadRealizedOccupancyToday()
      return Success.of()
    },
    [reloadRealizedOccupancyToday, reloadStaff]
  )

  const today = useMemo(() => LocalDate.today(), [])

  return renderResult(
    combine(unitInfoResponse, unitOrGroupStaffAttendance, occupancy),
    ([unitInfo, foobar, occupancy]) => (
      <>
        <TopBarWithGroupSelector
          title={unitInfo.name}
          selectedGroup={selectedGroup}
          onChangeGroup={changeGroup}
        />
        <ContentArea opaque fullHeight>
          <StaffAttendanceEditor
            // Force re-render if the selected group changes
            key={selectedGroup?.id}
            groupId={selectedGroup?.id}
            date={today}
            count={foobar.count}
            countOther={foobar.countOther}
            updated={foobar.updated}
            realizedOccupancy={occupancy}
            onConfirm={updateAttendance}
          />
        </ContentArea>
        <BottomNavBar selected="staff" />
      </>
    )
  )
})
