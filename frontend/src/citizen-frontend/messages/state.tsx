// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext } from 'react'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { getUnreadBulletinsCount } from './api'

export interface HeaderState {
  unreadBulletinsCount: number | null
  refreshUnreadBulletinsCount: () => void
}

const defaultState = {
  unreadBulletinsCount: null,
  refreshUnreadBulletinsCount: () => undefined
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

  const refreshUnreadBulletinsCount = useRestApi(
    getUnreadBulletinsCount,
    (result) => result.isSuccess && setUnreadBulletinsCount(result.value)
  )

  const value = useMemo(
    () => ({
      unreadBulletinsCount,
      refreshUnreadBulletinsCount
    }),
    [unreadBulletinsCount, refreshUnreadBulletinsCount]
  )

  return (
    <HeaderContext.Provider value={value}>{children}</HeaderContext.Provider>
  )
})
