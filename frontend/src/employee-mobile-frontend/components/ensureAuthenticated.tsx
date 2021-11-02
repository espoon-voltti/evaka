// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FullScreenDimmedSpinner } from 'lib-components/molecules/FullScreenDimmedSpinner'
import React, { ComponentType, useContext } from 'react'
import { Redirect } from 'react-router-dom'
import { UserContext } from '../state/user'

export default function ensureAuthenticated<P>(Component: ComponentType<P>) {
  return function Authenticated(props: P) {
    const { loggedIn, user } = useContext(UserContext)

    if (!loggedIn) {
      return <Redirect to="" />
    }

    return (
      <>
        {user.isLoading && <FullScreenDimmedSpinner />}
        <Component {...props} />
      </>
    )
  }
}
