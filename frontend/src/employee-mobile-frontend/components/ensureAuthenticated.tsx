// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ComponentType, useContext } from 'react'
import { Redirect } from 'react-router-dom'
import { UserContext } from '../state/user'

export default function ensureAuthenticated<P>(Component: ComponentType<P>) {
  return function Authenticated(props: P) {
    const { loggedIn } = useContext(UserContext)

    if (!loggedIn) {
      return <Redirect to="" />
    }

    return <Component {...props} />
  }
}
