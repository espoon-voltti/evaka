// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { mapChildAttendanceUIState } from '../types'

import AttendanceList from './AttendanceList'
import { useAttendanceContext } from './AttendancePageWrapper'

export default React.memo(function AttendancePageWrapper() {
  const { unitId, attendanceStatuses, unitChildren, attendanceStatus } =
    useAttendanceContext()

  return (
    <AttendanceList
      activeStatus={mapChildAttendanceUIState(attendanceStatus)}
      unitChildren={unitChildren}
      attendanceStatuses={attendanceStatuses}
      unitId={unitId}
    />
  )
})
