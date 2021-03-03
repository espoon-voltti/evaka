// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext } from 'react'

import { Loading, Result } from '@evaka/lib-common/api'
import { AttendanceResponse } from '../api/attendances'

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

    const value = useMemo(
      () => ({
        attendanceResponse,
        setAttendanceResponse,
        filterAttendanceResponse
      }),
      [attendanceResponse, setAttendanceResponse, filterAttendanceResponse]
    )

    return (
      <AttendanceUIContext.Provider value={value}>
        {children}
      </AttendanceUIContext.Provider>
    )
  }
)
