// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { Redirect } from 'wouter'

import type { DaycareId } from 'lib-common/generated/api-types/shared'

import { PinLogin } from './auth/PinLogin'
import { UserContext } from './auth/state'

interface Props {
  unitId: DaycareId
  children?: React.ReactNode
}

export const RequirePinAuth = React.memo(function RequirePinAuth({
  unitId,
  children
}: Props) {
  const { user } = useContext(UserContext)

  if (!user.isSuccess || !user.value) {
    return <Redirect replace to="/" />
  }
  if (!user.value.pinLoginActive) {
    return <PinLogin unitId={unitId} />
  }
  return <>{children}</>
})
