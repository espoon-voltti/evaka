// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  createContext,
  ReactNode,
  useContext,
  useEffect,
  useMemo
} from 'react'
import { idleTracker } from 'lib-common/utils/idleTracker'
import { client } from '../api-client'
import { useApiState } from 'lib-common/utils/useRestApi'
import { getAuthStatus } from './api'
import { Loading, Result } from 'lib-common/api'

export type Person = {
  id: string
  firstName: string
  lastName: string
  socialSecurityNumber: string
}

export type AccessibleFeatures = {
  messages: boolean
  reservations: boolean
  pedagogicalDocumentation: boolean
}

export type User = Person & {
  children: Person[]
  userType: 'ENDUSER' | 'CITIZEN_WEAK'
  accessibleFeatures: AccessibleFeatures
}

type AuthState = {
  user: Result<User | undefined>
  refreshAuthStatus: () => void
}

const defaultState: AuthState = {
  user: Loading.of(),
  refreshAuthStatus: () => undefined
}

export const AuthContext = createContext<AuthState>(defaultState)

export const AuthContextProvider = React.memo(function AuthContextProvider({
  children
}: {
  children: ReactNode
}) {
  const [authStatus, refreshAuthStatus] = useApiState(getAuthStatus, [])

  useEffect(
    () => idleTracker(client, refreshAuthStatus, { thresholdInMinutes: 20 }),
    [refreshAuthStatus]
  )

  const value = useMemo(
    () => ({
      user: authStatus.map((a) => (a.loggedIn ? a.user : undefined)),
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
