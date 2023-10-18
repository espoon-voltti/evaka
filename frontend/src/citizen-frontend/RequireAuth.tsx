// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { createSearchParams, Navigate, useLocation } from 'react-router-dom'

import { CitizenAuthLevel } from 'lib-common/generated/api-types/shared'

import { AuthContext } from './auth/state'
import { getStrongLoginUri } from './navigation/const'

interface Props {
  strength?: CitizenAuthLevel | undefined
  children?: React.ReactNode
}

export default React.memo(function RequireAuth({
  strength = 'STRONG',
  children
}: Props) {
  const location = useLocation()
  const { user } = useContext(AuthContext)

  const isStrong = user
    .map((usr) => usr?.authLevel === 'STRONG')
    .getOrElse(false)
  const isWeak = user.map((usr) => usr?.authLevel === 'WEAK').getOrElse(false)
  const isLoggedIn = isStrong || isWeak

  const returnUrl = `${location.pathname}${location.search}${location.hash}`

  return isLoggedIn ? (
    strength === 'STRONG' && !isStrong ? (
      refreshRedirect(getStrongLoginUri(returnUrl))
    ) : (
      <>{children}</>
    )
  ) : (
    <Navigate
      to={{
        pathname: '/login',
        search: createSearchParams({
          next: returnUrl
        }).toString()
      }}
    />
  )
})

function refreshRedirect(uri: string) {
  window.location.replace(uri)
  return null
}
