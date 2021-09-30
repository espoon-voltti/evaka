// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import BottomNavBar from '../common/BottomNavbar'
import { useParams } from 'react-router-dom'
import { UUID } from 'lib-common/types'
import { getUnitStaffAttendances } from '../../api/staffAttendances2'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { StaffAttendanceResponse } from 'lib-common/generated/api-types/attendance'
import { Loading, Result } from 'lib-common/api'
import { renderResult } from 'lib-components/async-rendering'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import StaffListItem from './StaffListItem'

export default function StaffPage2() {
  const { unitId } = useParams<{ unitId: UUID }>()

  const [attendanceResponse, setAttendanceResponse] = useState<
    Result<StaffAttendanceResponse>
  >(Loading.of())
  const loadAttendances = useRestApi(
    getUnitStaffAttendances,
    setAttendanceResponse
  )
  useEffect(() => loadAttendances(unitId), [unitId, loadAttendances])

  return (
    <>
      {renderResult(
        attendanceResponse.map((res) => res.staff),
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
  )
}
