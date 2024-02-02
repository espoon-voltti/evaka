// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useCallback, useMemo } from 'react'

import { setAntiCsrfToken } from 'employee-frontend/api/client'
import { AdRole, User } from 'lib-common/api-types/employee-auth'
import { query, useQuery } from 'lib-common/query'

import { getAuthStatus } from '../api/auth'

export interface UserState {
  loaded: boolean
  apiVersion: string | undefined
  loggedIn: boolean
  user: User | undefined
  roles: AdRole[]
  refreshAuthStatus: () => void
}

export const UserContext = createContext<UserState>({
  loaded: false,
  apiVersion: undefined,
  loggedIn: false,
  user: undefined,
  roles: [],
  refreshAuthStatus: () => undefined
})

const authStatusQuery = query({
  api: () =>
    getAuthStatus().then((status) => {
      setAntiCsrfToken(status.antiCsrfToken)
      return status
    }),
  queryKey: () => ['auth-status']
})

export const UserContextProvider = React.memo(function UserContextProvider({
  children
}: {
  children: React.JSX.Element
}) {
  const { data: authStatus, refetch } = useQuery(authStatusQuery())
  const refreshAuthStatus = useCallback(() => void refetch(), [refetch])

  const value = useMemo(
    () => ({
      loaded: !!authStatus,
      apiVersion: authStatus?.apiVersion,
      loggedIn: authStatus?.loggedIn ?? false,
      user: authStatus?.user,
      roles: authStatus?.roles ?? [],
      refreshAuthStatus
    }),
    [authStatus, refreshAuthStatus]
  )
  return <UserContext.Provider value={value}>{children}</UserContext.Provider>
})
