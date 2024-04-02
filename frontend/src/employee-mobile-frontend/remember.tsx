// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useMemo, useState } from 'react'

import { UUID } from 'lib-common/types'

export interface RememberState {
  groupId: UUID | undefined
  saveGroupId: (groupId: UUID | undefined) => void
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
    const [groupId, setGroupId] = useState<UUID>()

    const value = useMemo<RememberState>(
      () => ({
        groupId,
        saveGroupId: (groupId: UUID | undefined) => setGroupId(groupId)
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
