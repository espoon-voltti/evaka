// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { RouteComponentProps } from 'react-router'
import { Redirect } from 'react-router-dom'
import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'
import { PinLogin } from './PinLogin'

export default function requirePinAuth<T>(
  WrappedComponent: React.ComponentType<RouteComponentProps<T>>
) {
  return function WithRequirePinAuth(props: RouteComponentProps<T>) {
    const { user } = useContext(UserContext)

    return renderResult(user, (u) => {
      if (!u) {
        return <Redirect to="/" />
      }
      if (!u.pinLoginActive) {
        return <PinLogin />
      }
      return <WrappedComponent {...props} />
    })
  }
}
