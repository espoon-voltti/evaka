// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, createContext } from 'react'

import { AdRole, User } from 'lib-common/api-types/employee-auth'

export interface UserState {
  loggedIn: boolean
  user: User | undefined
  roles: AdRole[]
  refreshAuthStatus: () => void
}

export const UserContext = createContext<UserState>({
  loggedIn: false,
  user: undefined,
  roles: [],
  refreshAuthStatus: () => undefined
})

export const UserContextProvider = React.memo(function UserContextProvider({
  children,
  user,
  roles,
  refreshAuthStatus
}: {
  children: JSX.Element
  user: User | undefined
  roles: AdRole[] | undefined
  refreshAuthStatus: () => void
}) {
  const value = useMemo(
    () => ({
      loggedIn: !!user,
      user,
      roles: (user && roles) ?? [],
      refreshAuthStatus
    }),
    [user, roles, refreshAuthStatus]
  )
  return <UserContext.Provider value={value}>{children}</UserContext.Provider>
})
