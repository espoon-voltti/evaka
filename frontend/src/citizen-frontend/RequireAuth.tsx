// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { Redirect, useLocation } from 'react-router-dom'

import { AuthContext } from './auth/state'
import { getStrongLoginUri, getWeakLoginUri } from './header/const'

interface Props {
  strength?: 'STRONG' | 'WEAK' | undefined
  children?: React.ReactNode
}

export default React.memo(function RequireAuth({
  strength = 'STRONG',
  children
}: Props) {
  const location = useLocation()
  const { user } = useContext(AuthContext)

  const isStrong = user
    .map((usr) => usr?.userType === 'ENDUSER')
    .getOrElse(false)
  const isWeak = user
    .map((usr) => usr?.userType === 'CITIZEN_WEAK')
    .getOrElse(false)
  const isLoggedIn = isStrong || isWeak

  return isLoggedIn ? (
    strength === 'STRONG' && !isStrong ? (
      refreshRedirect(getStrongLoginUri(location.pathname))
    ) : (
      <>{children}</>
    )
  ) : strength === 'STRONG' ? (
    <Redirect to="/" />
  ) : (
    refreshRedirect(getWeakLoginUri(location.pathname))
  )
})

function refreshRedirect(uri: string) {
  window.location.replace(uri)
  return null
}
