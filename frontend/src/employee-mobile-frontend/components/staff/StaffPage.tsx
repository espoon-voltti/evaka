// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { ContentArea } from 'lib-components/layout/Container'
import { StaffAttendanceUpdate } from 'lib-common/generated/api-types/daycare'
import TopBar from '../common/TopBar'
import BottomNavBar from '../common/BottomNavbar'
import { postStaffAttendance } from '../../api/staff'
import { getRealizedOccupancyToday } from '../../api/occupancy'
import { combine, Loading, Result, Success } from 'lib-common/api'
import StaffAttendanceEditor from './StaffAttendanceEditor'
import LocalDate from 'lib-common/local-date'
import { staffAttendanceForGroupOrUnit } from '../../utils/staffAttendances'
import { renderResult } from '../async-rendering'
import { UUID } from 'lib-common/types'
import { UnitContext } from '../../state/unit'
import { StaffContext } from '../../state/staff'
import { GroupInfo } from 'lib-common/generated/api-types/attendance'

export default function StaffPage() {
  const history = useHistory()
  const { unitId, groupId: groupIdOrAll } = useParams<{
    unitId: UUID
    groupId: UUID | 'all'
  }>()

  const { unitInfoResponse } = useContext(UnitContext)

  const selectedGroup =
    groupIdOrAll === 'all'
      ? undefined
      : unitInfoResponse
          .map((res) => res.groups.find((g) => g.id === groupIdOrAll))
          .getOrElse(undefined)

  const { staffResponse, reloadStaff } = useContext(StaffContext)

  useEffect(reloadStaff, [reloadStaff])

  const [occupancyResponse, setOccupancyResponse] = useState<Result<number>>(
    Loading.of()
  )

  const loadRealizedOccupancyToday = useRestApi(
    getRealizedOccupancyToday,
    setOccupancyResponse
  )

  useEffect(() => {
    loadRealizedOccupancyToday(unitId, selectedGroup?.id)
  }, [unitId, selectedGroup, loadRealizedOccupancyToday])

  function changeGroup(group: GroupInfo | undefined) {
    const groupId = group === undefined ? 'all' : group.id
    history.push(`/units/${unitId}/groups/${groupId}/staff`)
  }

  const updateAttendance = async (attendance: StaffAttendanceUpdate) => {
    await postStaffAttendance(attendance)
    reloadStaff()
    await getRealizedOccupancyToday(unitId, selectedGroup?.id).then(
      setOccupancyResponse
    )
    return Success.of()
  }

  const today = LocalDate.today()

  return renderResult(
    combine(
      unitInfoResponse,
      staffResponse.map((staffAttendances) =>
        staffAttendanceForGroupOrUnit(staffAttendances, selectedGroup?.id)
      ),
      occupancyResponse
    ),
    ([unitInfo, staffAttendance, occupancy]) => (
      <>
        <TopBar
          unitName={unitInfo.name}
          selectedGroup={selectedGroup}
          onChangeGroup={changeGroup}
        />
        <ContentArea opaque fullHeight>
          <StaffAttendanceEditor
            groupId={selectedGroup?.id}
            date={today}
            count={staffAttendance.count}
            countOther={staffAttendance.countOther}
            updated={staffAttendance.updated}
            realizedOccupancy={occupancy}
            onConfirm={updateAttendance}
          />
        </ContentArea>
        <BottomNavBar selected="staff" />
      </>
    )
  )
}
