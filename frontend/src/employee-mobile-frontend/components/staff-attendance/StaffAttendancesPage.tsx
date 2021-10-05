// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect } from 'react'
import { renderResult } from '../async-rendering'
import BottomNavBar from '../common/BottomNavbar'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import StaffListItem from './StaffListItem'
import { StaffAttendanceContext } from '../../state/staff-attendance'
import TopBar from '../common/TopBar'
import { UnitContext } from '../../state/unit'
import { useHistory, useParams } from 'react-router-dom'
import { GroupInfo } from 'lib-common/generated/api-types/attendance'

export default function StaffAttendancesPage() {
  const history = useHistory()
  const { unitId, groupId } = useParams<{
    unitId: string
    groupId: string
  }>()

  const { unitInfoResponse } = useContext(UnitContext)
  const { staffAttendanceResponse, reloadStaffAttendance } = useContext(
    StaffAttendanceContext
  )
  useEffect(reloadStaffAttendance, [reloadStaffAttendance])

  const changeGroup = (group: GroupInfo | undefined) =>
    history.push(
      `/units/${unitId}/groups/${group?.id ?? 'all'}/staff-attendance`
    )

  return renderResult(unitInfoResponse, (unit) => (
    <>
      <TopBar
        unitName={unit.name}
        selectedGroup={
          groupId === 'all'
            ? undefined
            : unit.groups.find((g) => g.id === groupId)
        }
        onChangeGroup={changeGroup}
      />
      {renderResult(
        staffAttendanceResponse.map((res) =>
          groupId === 'all'
            ? res.staff
            : res.staff.filter((staffMember) =>
                staffMember.groupIds.includes(groupId)
              )
        ),
        (staffList) => (
          <FixedSpaceColumn spacing="zero">
            {staffList.map((staffMember) => (
              <StaffListItem
                staffMember={staffMember}
                key={staffMember.employeeId}
              />
            ))}
          </FixedSpaceColumn>
        )
      )}
      <BottomNavBar selected="staff" />
    </>
  ))
}
