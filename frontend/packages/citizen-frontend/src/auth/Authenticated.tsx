// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactNode, useContext, useEffect } from 'react'
import { client } from '../api-client'
import { AuthContext, User } from './state'

export default React.memo(function Authenticated(props: {
  children: ReactNode
}) {
  const authContext = useContext(AuthContext)

  useEffect(() => {
    void getAuthStatus()
      .then((response) => {
        const status = response.data
        if (status.loggedIn && status.user) {
          authContext.setUser(status.user)
        } else {
          redirectToEnduser()
        }
      })
      .catch(redirectToEnduser)
  }, [])

  return authContext.user !== undefined ? <>{props.children}</> : null
})

const redirectToEnduser = () => {
  window.location.href =
    window.location.host === 'localhost:9094' ? 'http://localhost:9091' : '/'
}

const getAuthStatus = () => client.get<AuthStatus>('/auth/status')

type AuthStatus = { loggedIn: false } | { loggedIn: true; user: User }
