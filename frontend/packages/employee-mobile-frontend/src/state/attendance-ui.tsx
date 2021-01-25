// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext } from 'react'

import { Loading, Result } from '@evaka/lib-common/src/api'
import { AttendanceResponse } from '~api/attendances'

interface UIState {
  attendanceResponse: Result<AttendanceResponse>
  setAttendanceResponse: (result: Result<AttendanceResponse>) => void
  filterAndSetAttendanceResponse: (
    result: Result<AttendanceResponse>,
    groupIdOrAll: string | 'all'
  ) => void
}

const defaultState: UIState = {
  attendanceResponse: Loading.of(),
  setAttendanceResponse: () => undefined,
  filterAndSetAttendanceResponse: () => undefined
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

    // TODO: return just attendanceresponse. Do not set setAttendanceResponse(attendanceResponse)
    function filterAndSetAttendanceResponse(
      attendanceResponse: Result<AttendanceResponse>,
      groupIdOrAll: string | 'all'
    ) {
      if (attendanceResponse.isSuccess) {
        if (groupIdOrAll !== 'all')
          attendanceResponse.value.children = attendanceResponse.value.children.filter(
            (child) => child.groupId === groupIdOrAll
          )
      }
      setAttendanceResponse(attendanceResponse)
    }

    const value = useMemo(
      () => ({
        attendanceResponse,
        setAttendanceResponse,
        filterAndSetAttendanceResponse
      }),
      [
        attendanceResponse,
        setAttendanceResponse,
        filterAndSetAttendanceResponse
      ]
    )

    return (
      <AttendanceUIContext.Provider value={value}>
        {children}
      </AttendanceUIContext.Provider>
    )
  }
)
