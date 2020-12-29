// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactNode, useContext, useEffect, useState } from 'react'
import { config } from '../configs'
import { client } from '../api-client'
import { AuthContext } from './state'

export default React.memo(function Authenticated(props: {
  children: ReactNode
}) {
  const authContext = useContext(AuthContext)
  const [loggedIn, setLoggedIn] = useState(false)

  useEffect(() => {
    void getAuthStatus()
      .then((response) => {
        const status = response.data
        if (status.loggedIn && status.user) {
          setLoggedIn(true)
          authContext.setUser(status.user)
        } else {
          redirectToEnduser()
        }
      })
      .catch(redirectToEnduser)
  }, [])

  return loggedIn ? <>{props.children}</> : null
})

const redirectToEnduser = () => {
  window.location.href = config.enduserBaseUrl
}

const getAuthStatus = () => client.get<AuthStatus>('/auth/status')

type AuthStatus =
  | { loggedIn: false }
  | { loggedIn: true; user: { firstName: string; lastName: string } }
