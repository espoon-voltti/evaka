// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { Redirect, useSearchParams } from 'wouter'

import { UserContext } from '../state/user'

import Login from './login-page/Login'

export default React.memo(function LoginPage() {
  const [queryParams] = useSearchParams()
  const { loggedIn } = useContext(UserContext)

  const error = queryParams.get('error') || undefined
  const loginError = queryParams.get('loginError') || undefined

  if (loggedIn) {
    return <Redirect replace to="/" />
  }

  return <Login error={error || loginError} />
})
