// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext } from 'react'

export interface MessagesState {
  unreadBulletinsCount: number | null
  setUnreadBulletinsCount: (request: number) => void
}

const defaultState = {
  unreadBulletinsCount: null,
  setUnreadBulletinsCount: () => undefined
}

export const MessagesContext = createContext<MessagesState>(defaultState)

export const MessagesContextProvider = React.memo(
  function MessagesContextProvider({
    children
  }: {
    children: React.ReactNode
  }) {
    const [unreadBulletinsCount, setUnreadBulletinsCount] = useState<
      number | null
    >(null)

    const value = useMemo(
      () => ({
        unreadBulletinsCount: unreadBulletinsCount,
        setUnreadBulletinsCount: setUnreadBulletinsCount
      }),
      [unreadBulletinsCount, setUnreadBulletinsCount]
    )

    return (
      <MessagesContext.Provider value={value}>
        {children}
      </MessagesContext.Provider>
    )
  }
)
