// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { RouteComponentProps } from 'react-router'
import { Redirect } from 'react-router-dom'
import { getWeakLoginUri, getLoginUri } from '../header/const'
import { AuthContext } from './state'

export function refreshRedirect(uri: string) {
  window.location.replace(uri)
  return null
}

export default function requireAuth<T>(
  WrappedComponent: React.ComponentType<RouteComponentProps<T>>,
  requireStrong = true
) {
  function WithRequireAuth(props: RouteComponentProps<T>) {
    const { user } = useContext(AuthContext)

    return user.isSuccess && user.value ? (
      requireStrong && user.value.userType === 'CITIZEN_WEAK' ? (
        refreshRedirect(getLoginUri(props.location.pathname))
      ) : (
        <WrappedComponent {...props} />
      )
    ) : requireStrong ? (
      <Redirect to={'/'} />
    ) : (
      refreshRedirect(getWeakLoginUri(props.location.pathname))
    )
  }

  return React.memo(WithRequireAuth) as typeof WithRequireAuth
}
