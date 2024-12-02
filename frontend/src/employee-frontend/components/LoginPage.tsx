// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { Navigate, useLocation } from 'react-router'

import { UserContext } from '../state/user'

import Login from './login-page/Login'

export default React.memo(function LoginPage() {
  const location = useLocation()
  const { loggedIn } = useContext(UserContext)

  const queryParams = new URLSearchParams(location.search)
  const error = queryParams.get('error') || undefined
  const loginError = queryParams.get('loginError') || undefined

  if (loggedIn) {
    return <Navigate replace to="/" />
  }

  return <Login error={error || loginError} />
})
