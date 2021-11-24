// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { PropsWithChildren, useContext } from 'react'
import { Redirect } from 'react-router-dom'
import { UserContext } from '../state/user'

export default React.memo(function EnsureAuthenticated({
  children
}: PropsWithChildren<unknown>) {
  const { loggedIn } = useContext(UserContext)
  return loggedIn ? <>{children}</> : <Redirect to="" />
})
