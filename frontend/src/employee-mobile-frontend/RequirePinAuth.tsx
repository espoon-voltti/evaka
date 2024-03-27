// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { Navigate } from 'react-router-dom'

import { UUID } from 'lib-common/types'

import { PinLogin } from './auth/PinLogin'
import { UserContext } from './auth/state'

interface Props {
  unitId: UUID
  children?: React.ReactNode
}

export const RequirePinAuth = React.memo(function RequirePinAuth({
  unitId,
  children
}: Props) {
  const { user } = useContext(UserContext)

  if (!user.isSuccess || !user.value) {
    return <Navigate replace to="/" />
  }
  if (!user.value.pinLoginActive) {
    return <PinLogin unitId={unitId} />
  }
  return <>{children}</>
})
