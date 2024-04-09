// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { Navigate } from 'react-router-dom'

import { UserContext } from './auth/state'

interface Props {
  children?: React.ReactNode
}

export default React.memo(function RequireAuth({ children }: Props) {
  const { loggedIn } = useContext(UserContext)

  return loggedIn ? <>{children}</> : <Navigate replace to="/" />
})
