// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { AuthContext } from '../auth/state'
import { RouteComponentProps } from 'react-router'
import { Redirect } from 'react-router-dom'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import { getWeakLoginUri, getLoginUri } from 'citizen-frontend/header/const'

function refreshRedirect(uri: string) {
  window.location.replace(uri)
  return null
}

export default function requireAuth<T>(
  WrappedComponent: React.ComponentType<RouteComponentProps<T>>,
  requireStrong = true
) {
  return function WithRequireAuth(props: RouteComponentProps<T>) {
    const { user, loading } = useContext(AuthContext)

    return user ? (
      requireStrong && user.userType === 'CITIZEN_WEAK' ? (
        refreshRedirect(getLoginUri(props.location.pathname))
      ) : (
        <WrappedComponent {...props} />
      )
    ) : loading ? (
      <SpinnerSegment />
    ) : requireStrong ? (
      <Redirect to={'/'} />
    ) : (
      refreshRedirect(getWeakLoginUri(props.location.pathname))
    )
  }
}
