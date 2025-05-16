// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useMemo, useState } from 'react'

import type { GroupId } from 'lib-common/generated/api-types/shared'

export interface RememberState {
  groupId: GroupId | undefined
  saveGroupId: (groupId: GroupId | undefined) => void
}

export const RememberContext = createContext<RememberState>({
  groupId: undefined,
  saveGroupId: () => undefined
})

export const RememberContextProvider = React.memo(
  function RememberContextProvider({
    children
  }: {
    children: React.ReactNode
  }) {
    const [groupId, setGroupId] = useState<GroupId>()

    const value = useMemo<RememberState>(
      () => ({
        groupId,
        saveGroupId: (groupId: GroupId | undefined) => setGroupId(groupId)
      }),
      [groupId, setGroupId]
    )
    return (
      <RememberContext.Provider value={value}>
        <>{children}</>
      </RememberContext.Provider>
    )
  }
)
