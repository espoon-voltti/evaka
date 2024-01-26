// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useQueryClient } from '@tanstack/react-query'
import React, { createContext, useCallback, useMemo } from 'react'

import { Loading, Result } from 'lib-common/api'
import { MobileUser } from 'lib-common/api-types/employee-auth'
import { query, useQueryResult } from 'lib-common/query'

import { renderResult } from '../async-rendering'
import { setAntiCsrfToken } from '../client'

import { getAuthStatus } from './api'

export interface UserState {
  apiVersion: string | undefined
  loggedIn: boolean
  user: Result<MobileUser | null>
  refreshAuthStatus: () => void
}

export const UserContext = createContext<UserState>({
  apiVersion: undefined,
  loggedIn: false,
  user: Loading.of(),
  refreshAuthStatus: () => null
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
  children: React.ReactNode
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
      loggedIn: authStatus.map((a) => a.loggedIn).getOrElse(false),
      user: authStatus.map((a) => a.user ?? null),
      refreshAuthStatus
    }),
    [authStatus, refreshAuthStatus]
  )
  return (
    <UserContext.Provider value={value}>
      {renderResult(authStatus, () => (
        <>{children}</>
      ))}
    </UserContext.Provider>
  )
})
