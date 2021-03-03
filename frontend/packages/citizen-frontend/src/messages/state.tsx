// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext } from 'react'

export interface HeaderState {
  unreadBulletinsCount: number | null
  setUnreadBulletinsCount: (value: number) => void
}

const defaultState = {
  unreadBulletinsCount: null,
  setUnreadBulletinsCount: () => undefined
}

export const HeaderContext = createContext<HeaderState>(defaultState)

export const HeaderContextProvider = React.memo(function HeaderContextProvider({
  children
}: {
  children: React.ReactNode
}) {
  const [unreadBulletinsCount, setUnreadBulletinsCount] = useState<
    number | null
  >(null)

  const value = useMemo(
    () => ({
      unreadBulletinsCount,
      setUnreadBulletinsCount
    }),
    [unreadBulletinsCount, setUnreadBulletinsCount]
  )

  return (
    <HeaderContext.Provider value={value}>{children}</HeaderContext.Provider>
  )
})
