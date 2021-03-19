// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { AuthContext } from '../auth/state'
import { RouteComponentProps } from 'react-router'
import { Redirect } from 'react-router-dom'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'

export default function requireAuth<T>(
  WrappedComponent: React.ComponentType<RouteComponentProps<T>>
) {
  return function WithRequireAuth(props: RouteComponentProps<T>) {
    const { user, loading } = useContext(AuthContext)

    return user ? (
      <WrappedComponent {...props} />
    ) : loading ? (
      <SpinnerSegment />
    ) : (
      <Redirect to={'/'} />
    )
  }
}
