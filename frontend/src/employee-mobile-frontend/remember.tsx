// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useMemo, useState } from 'react'

import type { AttendanceStatus } from 'lib-common/generated/api-types/attendance'
import type { GroupId } from 'lib-common/generated/api-types/shared'

import type { SortType } from './child-attendance/ChildList'

type ChildListSort = Record<AttendanceStatus, SortType>

export interface RememberState {
  groupId: GroupId | undefined
  saveGroupId: (groupId: GroupId | undefined) => void
  childListSort: ChildListSort
  updateChildListSort: (
    attendanceStatus: AttendanceStatus,
    sortType: SortType
  ) => void
}

const defaultChildListSort: ChildListSort = {
  COMING: 'CHILD_FIRST_NAME',
  PRESENT: 'CHILD_FIRST_NAME',
  DEPARTED: 'CHILD_FIRST_NAME',
  ABSENT: 'CHILD_FIRST_NAME'
}

export const RememberContext = createContext<RememberState>({
  groupId: undefined,
  saveGroupId: () => undefined,
  childListSort: defaultChildListSort,
  updateChildListSort: () => undefined
})

export const RememberContextProvider = React.memo(
  function RememberContextProvider({
    children
  }: {
    children: React.ReactNode
  }) {
    const [groupId, setGroupId] = useState<GroupId>()
    const [childListSort, setChildListSort] = useState(defaultChildListSort)

    const value = useMemo<RememberState>(
      () => ({
        groupId,
        saveGroupId: (groupId: GroupId | undefined) => setGroupId(groupId),
        childListSort,
        updateChildListSort: (attendanceStatus, sortType) =>
          setChildListSort({
            ...childListSort,
            [attendanceStatus]: sortType
          })
      }),
      [childListSort, groupId]
    )
    return (
      <RememberContext.Provider value={value}>
        <>{children}</>
      </RememberContext.Provider>
    )
  }
)
