// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, createContext } from 'react'
import { User } from '../types'

export interface UserState {
  loggedIn: boolean
  user: User | undefined
  refreshAuthStatus: () => Promise<void>
}

export const UserContext = createContext<UserState>({
  loggedIn: false,
  user: undefined,
  refreshAuthStatus: () => Promise.resolve()
})

export const UserContextProvider = React.memo(function UserContextProvider({
  children,
  user,
  refreshAuthStatus
}: {
  children: JSX.Element
  user: User | undefined
  refreshAuthStatus: () => Promise<void>
}) {
  const value = useMemo(
    () => ({
      loggedIn: user !== undefined,
      user,
      refreshAuthStatus
    }),
    [user, refreshAuthStatus]
  )
  return <UserContext.Provider value={value}>{children}</UserContext.Provider>
})
