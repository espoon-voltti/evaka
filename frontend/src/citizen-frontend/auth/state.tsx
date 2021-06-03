// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  createContext,
  ReactNode,
  useContext,
  useMemo,
  useState
} from 'react'

export type Person = {
  id: string
  firstName: string
  lastName: string
  socialSecurityNumber: string
}

export type User = Person & {
  children: Person[]
  userType: 'ENDUSER' | 'CITIZEN_WEAK'
}

type AuthState = {
  loading: boolean
  setLoading: (loading: boolean) => void
  user: User | undefined
  setUser: (user: User | undefined) => void
}

const defaultState: AuthState = {
  loading: true,
  setLoading: () => undefined,
  user: undefined,
  setUser: () => undefined
}

export const AuthContext = createContext<AuthState>(defaultState)

export const AuthContextProvider = React.memo(
  function AuthContextProvider(props: { children: ReactNode }) {
    const [user, setUser] = useState<User | undefined>(defaultState.user)
    const [loading, setLoading] = useState<boolean>(defaultState.loading)

    const value = useMemo(
      () => ({
        user,
        setUser,
        loading,
        setLoading
      }),
      [user, setUser, loading, setLoading]
    )

    return (
      <AuthContext.Provider value={value}>
        {props.children}
      </AuthContext.Provider>
    )
  }
)

export const useUser = (): User | undefined => {
  const authContext = useContext(AuthContext)
  return authContext.user
}
