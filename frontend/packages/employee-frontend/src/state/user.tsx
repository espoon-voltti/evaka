// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, createContext } from 'react'
import { AdRole, User } from '~types'

export interface UserState {
  loggedIn: boolean
  user: User | undefined
  roles: AdRole[]
}

export const UserContext = createContext<UserState>({
  loggedIn: false,
  user: undefined,
  roles: []
})

export const UserContextProvider = React.memo(function UserContextProvider({
  children,
  user,
  roles
}: {
  children: JSX.Element
  user: User | undefined
  roles: AdRole[] | undefined
}) {
  const value = useMemo(
    () => ({
      loggedIn: !!user,
      user,
      roles: (user && roles) ?? []
    }),
    [user, roles]
  )
  return <UserContext.Provider value={value}>{children}</UserContext.Provider>
})

export const ApplyAclRoles = React.memo(function ApplyAclRoles({
  children,
  roles
}: {
  children: JSX.Element
  roles: AdRole[]
}) {
  const { user } = useContext(UserContext)
  return (
    <UserContextProvider user={user} roles={roles}>
      {children}
    </UserContextProvider>
  )
})
