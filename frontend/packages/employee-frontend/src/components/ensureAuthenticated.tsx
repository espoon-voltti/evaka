// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ComponentType, useContext } from 'react'
import { Redirect, RouteComponentProps } from 'react-router-dom'
import { UserContext } from '~state/user'

export default function ensureAuthenticated<P>(
  Component: ComponentType<RouteComponentProps<P>>
) {
  return function Authenticated(props: RouteComponentProps<P>) {
    const { loggedIn } = useContext(UserContext)

    if (!loggedIn) {
      return <Redirect to="/login" />
    }

    return <Component {...props} />
  }
}
