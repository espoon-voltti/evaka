// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext } from 'react'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { getUnreadMessagesCount } from './api'

export interface HeaderState {
  unreadMessagesCount: number | undefined
  refreshUnreadMessagesCount: () => void
}

const defaultState = {
  unreadMessagesCount: undefined,
  refreshUnreadMessagesCount: () => undefined
}

export const HeaderContext = createContext<HeaderState>(defaultState)

export const HeaderContextProvider = React.memo(function HeaderContextProvider({
  children
}: {
  children: React.ReactNode
}) {
  const [unreadMessagesCount, setUnreadMessagesCount] = useState<number>()

  const refreshUnreadMessagesCount = useRestApi(
    getUnreadMessagesCount,
    (result) => result.isSuccess && setUnreadMessagesCount(result.value)
  )

  const value = useMemo(
    () => ({
      unreadMessagesCount,
      refreshUnreadMessagesCount
    }),
    [unreadMessagesCount, refreshUnreadMessagesCount]
  )

  return (
    <HeaderContext.Provider value={value}>{children}</HeaderContext.Provider>
  )
})
