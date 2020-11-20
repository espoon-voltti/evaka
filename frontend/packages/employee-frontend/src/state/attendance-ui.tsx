// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext } from 'react'

import { isSuccess, Loading, Result } from '~api'
import { AttendanceResponse } from '~api/attendances'
import { UUID } from '~types'

interface UIState {
  attendanceResponse: Result<AttendanceResponse>
  setAttendanceResponse: (result: Result<AttendanceResponse>) => void
  filterAndSetAttendanceResponse: (
    result: Result<AttendanceResponse>,
    groupIdOrAll: UUID | 'all'
  ) => void
}

const defaultState: UIState = {
  attendanceResponse: Loading(),
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
    >(Loading())

    // TODO: return just attendanceresponse. Do not set setAttendanceResponse(attendanceResponse)
    function filterAndSetAttendanceResponse(
      attendanceResponse: Result<AttendanceResponse>,
      groupIdOrAll: UUID | 'all'
    ) {
      if (isSuccess(attendanceResponse)) {
        if (groupIdOrAll !== 'all')
          attendanceResponse.data.children = attendanceResponse.data.children.filter(
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
