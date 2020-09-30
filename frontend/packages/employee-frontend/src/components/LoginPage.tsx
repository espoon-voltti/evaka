// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { Redirect, RouteComponentProps } from 'react-router-dom'
import { UserContext } from '~state/user'
import Login from './login-page/Login'
import { getLoginUrl } from '~api/auth'

function LoginPage({ location }: RouteComponentProps) {
  const { loggedIn } = useContext(UserContext)

  const queryParams = new URLSearchParams(location.search)
  const error = queryParams.get('error') || undefined
  const loginError = queryParams.get('loginError') || undefined

  if (loggedIn) {
    return <Redirect to={'/'} />
  }

  return <Login loginUrl={getLoginUrl()} error={error || loginError} />
}

export default LoginPage
