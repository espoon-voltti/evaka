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

type Person = {
  id: string
  firstName: string
  lastName: string
}

export type User = Person & {
  children: Person[]
}

type AuthState = {
  user?: User
  setUser: (user: User) => void
}

const defaultState: AuthState = {
  user: undefined,
  setUser: () => undefined
}

export const AuthContext = createContext<AuthState>(defaultState)

export const AuthContextProvider = React.memo(
  function AuthContextProvider(props: { children: ReactNode }) {
    const [user, setUser] = useState<User | undefined>(defaultState.user)

    const value = useMemo(
      () => ({
        user,
        setUser
      }),
      [user, setUser]
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
