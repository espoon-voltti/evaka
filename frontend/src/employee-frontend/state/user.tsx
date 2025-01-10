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
import { Queries, useQuery } from 'lib-common/query'
import { LoginStatusChangeEvent } from 'lib-common/utils/login-status'

import { getAuthStatus } from '../api/auth'

export interface UserState {
  loaded: boolean
  apiVersion: string | undefined
  loggedIn: boolean
  user: User | undefined
  roles: AdRole[]
  refreshAuthStatus: () => void
  unauthorizedApiCallDetected: boolean
  dismissUnauthorizedApiCallDetection: () => void
}

export const UserContext = createContext<UserState>({
  loaded: false,
  apiVersion: undefined,
  loggedIn: false,
  user: undefined,
  roles: [],
  refreshAuthStatus: () => undefined,
  unauthorizedApiCallDetected: false,
  dismissUnauthorizedApiCallDetection: () => undefined
})

const q = new Queries()
const authStatusQuery = q.query(getAuthStatus)

export const UserContextProvider = React.memo(function UserContextProvider({
  children
}: {
  children: React.JSX.Element
}) {
  const [unauthorizedApiCallDetected, setUnauthorizedApiCallDetected] =
    useState(false)
  const { data: authStatus, refetch } = useQuery(authStatusQuery())

  const refreshAuthStatus = useCallback(async () => {
    const result = await refetch()
    // Reset logout detection if user is logged in again
    if (result.data?.loggedIn) {
      setUnauthorizedApiCallDetected(false)
    }
    return result
  }, [refetch])

  useEffect(() => {
    const eventListener = (loginStatusEvent: LoginStatusChangeEvent) => {
      loginStatusEvent.preventDefault()
      setUnauthorizedApiCallDetected(!loginStatusEvent.detail)
    }
    window.addEventListener(LoginStatusChangeEvent.name, eventListener)
    return () => {
      window.removeEventListener(LoginStatusChangeEvent.name, eventListener)
    }
  }, [setUnauthorizedApiCallDetected])

  const value = useMemo(
    () => ({
      loaded: !!authStatus,
      apiVersion: authStatus?.apiVersion,
      loggedIn: authStatus?.loggedIn ?? false,
      user: authStatus?.user,
      roles: authStatus?.roles ?? [],
      refreshAuthStatus,
      unauthorizedApiCallDetected,
      dismissUnauthorizedApiCallDetection: () =>
        setUnauthorizedApiCallDetected(false)
    }),
    [authStatus, refreshAuthStatus, unauthorizedApiCallDetected]
  )
  return <UserContext.Provider value={value}>{children}</UserContext.Provider>
})
