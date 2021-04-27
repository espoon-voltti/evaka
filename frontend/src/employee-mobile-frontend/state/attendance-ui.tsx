// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState, createContext } from 'react'

import { Loading, Result } from 'lib-common/api'
import { AttendanceResponse } from '../api/attendances'
import { StaffAttendanceGroup } from 'lib-common/api-types/staffAttendances'

interface UIState {
  attendanceResponse: Result<AttendanceResponse>
  setAttendanceResponse: (result: Result<AttendanceResponse>) => void
  filterAttendanceResponse: (
    result: Result<AttendanceResponse>,
    groupIdOrAll: string | 'all'
  ) => Result<AttendanceResponse>
}

const defaultState: UIState = {
  attendanceResponse: Loading.of(),
  setAttendanceResponse: () => undefined,
  filterAttendanceResponse: () => Loading.of()
}

export const AttendanceUIContext = createContext<UIState>(defaultState)

export const AttendanceUIContextProvider = React.memo(
  function AttendanceUIContextProvider({
    children
  }: {
    children: JSX.Element
  }) {
    const [attendanceResponse, setAttendanceResponse] = useState<
      Result<AttendanceResponse>
    >(Loading.of())

    const [staffAttendanceResponse, setStaffAttendanceResponse] = useState<
      Result<StaffAttendanceGroup>
    >(Loading.of())

    const reset = useCallback(() => {
      setAttendanceResponse(Loading.of())
      setStaffAttendanceResponse(Loading.of())
    }, [])

    const value = useMemo(
      () => ({
        attendanceResponse,
        setAttendanceResponse,
        filterAttendanceResponse,
        staffAttendanceResponse,
        setStaffAttendanceResponse,
        reset
      }),
      [attendanceResponse, staffAttendanceResponse, reset]
    )

    return (
      <AttendanceUIContext.Provider value={value}>
        {children}
      </AttendanceUIContext.Provider>
    )
  }
)

function filterAttendanceResponse(
  attendanceResponse: Result<AttendanceResponse>,
  groupIdOrAll: string | 'all'
) {
  if (attendanceResponse.isSuccess) {
    if (groupIdOrAll !== 'all')
      attendanceResponse.value.children = attendanceResponse.value.children.filter(
        (child) => child.groupId === groupIdOrAll
      )
  }
  return attendanceResponse
}
