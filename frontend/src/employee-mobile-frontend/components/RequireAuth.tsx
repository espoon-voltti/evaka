// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { Navigate } from 'react-router-dom'

import { UserContext } from '../state/user'

import { PinLogin } from './auth/PinLogin'

interface Props {
  strength?: 'PAIRING' | 'PIN'
  children?: React.ReactNode
}

export default React.memo(function RequireAuth({
  strength = 'PAIRING',
  children
}: Props) {
  const { loggedIn, user } = useContext(UserContext)

  if (strength === 'PAIRING') {
    return loggedIn ? <>{children}</> : <Navigate replace to="/" />
  } else {
    if (!user.isSuccess || !user.value) {
      return <Navigate replace to="/" />
    }
    if (!user.value.pinLoginActive) {
      return <PinLogin />
    }
    return <>{children}</>
  }
})
