// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Loading, Result } from 'lib-common/api'
import { idleTracker } from 'lib-common/utils/idleTracker'
import { useApiState } from 'lib-common/utils/useRestApi'
import React, { createContext, useEffect, useMemo } from 'react'
import { getAuthStatus } from '../api/auth'
import { client } from '../api/client'
import { User } from '../types'

export interface UserState {
  loggedIn: boolean
  user: Result<User | null>
  refreshAuthStatus: () => void
}

export const UserContext = createContext<UserState>({
  loggedIn: false,
  user: Loading.of(),
  refreshAuthStatus: () => null
})

export const UserContextProvider = React.memo(function UserContextProvider({
  children
}: {
  children: React.ReactNode
}) {
  const [authStatus, refreshAuthStatus] = useApiState(getAuthStatus, [])

  useEffect(
    () => idleTracker(client, refreshAuthStatus, { thresholdInMinutes: 20 }),
    [refreshAuthStatus]
  )

  const value = useMemo(
    () => ({
      loggedIn: authStatus.map((a) => a.loggedIn).getOrElse(false),
      user: authStatus.map((a) => a.user ?? null),
      refreshAuthStatus
    }),
    [authStatus, refreshAuthStatus]
  )
  return <UserContext.Provider value={value}>{children}</UserContext.Provider>
})
