// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState
} from 'react'

import { AdRole, User } from 'lib-common/api-types/employee-auth'
import { query, useQuery } from 'lib-common/query'
import { LoginStatusChangeEvent } from 'lib-common/utils/login-status'

import { getAuthStatus } from '../api/auth'

export interface UserState {
  loaded: boolean
  apiVersion: string | undefined
  loggedIn: boolean
  user: User | undefined
  roles: AdRole[]
  refreshAuthStatus: () => void
  logoutDetected: boolean
  dismissLogoutDetection: () => void
}

export const UserContext = createContext<UserState>({
  loaded: false,
  apiVersion: undefined,
  loggedIn: false,
  user: undefined,
  roles: [],
  refreshAuthStatus: () => undefined,
  logoutDetected: false,
  dismissLogoutDetection: () => undefined
})

const authStatusQuery = query({
  api: () => getAuthStatus(),
  queryKey: () => ['auth-status']
})

export const UserContextProvider = React.memo(function UserContextProvider({
  children
}: {
  children: React.JSX.Element
}) {
  const [logoutDetected, setLogoutDetected] = useState(false)
  const { data: authStatus, refetch } = useQuery(authStatusQuery())
  const refreshAuthStatus = useCallback(() => void refetch(), [refetch])

  useEffect(() => {
    const eventListener = ((loginStatusEvent: LoginStatusChangeEvent) => {
      loginStatusEvent.preventDefault()
      setLogoutDetected(!loginStatusEvent.loginStatus)
    }) as EventListener
    window.addEventListener(LoginStatusChangeEvent.name, eventListener)
    return () => {
      window.removeEventListener(LoginStatusChangeEvent.name, eventListener)
    }
  }, [setLogoutDetected])

  const value = useMemo(
    () => ({
      loaded: !!authStatus,
      apiVersion: authStatus?.apiVersion,
      loggedIn: authStatus?.loggedIn ?? false,
      user: authStatus?.user,
      roles: authStatus?.roles ?? [],
      refreshAuthStatus,
      logoutDetected,
      dismissLogoutDetection: () => setLogoutDetected(false)
    }),
    [authStatus, refreshAuthStatus, logoutDetected]
  )
  return <UserContext.Provider value={value}>{children}</UserContext.Provider>
})
