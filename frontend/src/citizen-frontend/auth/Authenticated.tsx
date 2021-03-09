// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactNode, useContext, useEffect } from 'react'
import { client } from '../api-client'
import { AuthContext, User } from './state'

export default function Authenticated(props: { children: ReactNode }) {
  const { setUser, setLoading } = useContext(AuthContext)

  useEffect(() => {
    setLoading(true)
    void getAuthStatus()
      .then((response) => {
        const status = response.data
        if (status.loggedIn && status.user) {
          setUser(status.user)
        } else {
          setUser(undefined)
        }
      })
      .catch(() => setUser(undefined))
      .finally(() => setLoading(false))
  }, [])

  return <>{props.children}</>
}

const getAuthStatus = () => client.get<AuthStatus>('/auth/status')

type AuthStatus = { loggedIn: false } | { loggedIn: true; user: User }
