// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useQueryClient } from '@tanstack/react-query'
import React, {
  createContext,
  ReactNode,
  useCallback,
  useContext,
  useMemo
} from 'react'

import { Loading, Result } from 'lib-common/api'
import { CitizenUserDetails } from 'lib-common/generated/api-types/pis'
import {
  CitizenAuthLevel,
  CitizenFeatures
} from 'lib-common/generated/api-types/shared'
import { query, useQueryResult } from 'lib-common/query'

import { setAntiCsrfToken } from '../api-client'

import { getAuthStatus } from './api'

export interface User extends CitizenUserDetails {
  authLevel: CitizenAuthLevel
  accessibleFeatures: CitizenFeatures
}

type AuthState = {
  apiVersion: string | undefined
  user: Result<User | undefined>
  refreshAuthStatus: () => void
}

const defaultState: AuthState = {
  apiVersion: undefined,
  user: Loading.of(),
  refreshAuthStatus: () => undefined
}

export const AuthContext = createContext<AuthState>(defaultState)

const authStatusQuery = query({
  api: () =>
    getAuthStatus().then((status) => {
      setAntiCsrfToken(status.loggedIn ? status.antiCsrfToken : undefined)
      return status
    }),
  queryKey: () => ['auth-status']
})

export const AuthContextProvider = React.memo(function AuthContextProvider({
  children
}: {
  children: ReactNode
}) {
  const authStatus = useQueryResult(authStatusQuery())
  const queryClient = useQueryClient()
  const refreshAuthStatus = useCallback(
    () =>
      queryClient.invalidateQueries({
        queryKey: authStatusQuery().queryKey
      }),
    [queryClient]
  )

  const value = useMemo(
    () => ({
      apiVersion: authStatus.map((a) => a.apiVersion).getOrElse(undefined),
      user: authStatus.map((a) =>
        a.loggedIn
          ? {
              ...a.user.details,
              // TODO: remove this extra backwards compatibility code and fetch from one property only
              authLevel: a.authLevel ?? a.user.authLevel,
              accessibleFeatures:
                // TODO: remove this extra backwards compatibility code and fetch from one property only
                ((a.user.details as any) // eslint-disable-line @typescript-eslint/no-explicit-any
                  .accessibleFeatures as CitizenFeatures) ?? // eslint-disable-line @typescript-eslint/no-unsafe-member-access
                a.user.accessibleFeatures
            }
          : undefined
      ),
      refreshAuthStatus
    }),
    [authStatus, refreshAuthStatus]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
})

export const useUser = (): User | undefined => {
  const authContext = useContext(AuthContext)
  return authContext.user.getOrElse(undefined)
}
