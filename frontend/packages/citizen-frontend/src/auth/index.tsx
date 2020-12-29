// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactNode } from 'react'
import Authenticated from './Authenticated'
import { AuthContextProvider } from './state'

export { useUser } from './state'

export const Authentication = React.memo(function Authentication({
  children
}: {
  children: ReactNode
}) {
  return (
    <AuthContextProvider>
      <Authenticated>{children}</Authenticated>
    </AuthContextProvider>
  )
})
