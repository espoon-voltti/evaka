// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactNode, useContext, useMemo } from 'react'

import { AdRole } from 'lib-common/api-types/employee-auth'
import { Action } from 'lib-common/generated/action'

import { UserContext } from '../state/user'

export const requireRole = (roles: AdRole[], ...requiresOneOf: AdRole[]) => {
  const requiredRoles: AdRole[] = [...requiresOneOf, 'ADMIN']
  return requiredRoles.some((role) => roles.includes(role))
}

export const hasRole = (roles: AdRole[], role: AdRole) =>
  roles.some((r) => r === role)

export const RequireRole = React.memo(function RequireRole({
  oneOf,
  children
}: {
  oneOf: AdRole[]
  children: ReactNode | ReactNode[]
}) {
  const { roles } = useContext(UserContext)
  return requireRole(roles, ...oneOf) ? <>{children}</> : null
})

export const RequirePermittedGlobalAction = React.memo(
  function RequirePermittedGlobalAction({
    oneOf,
    children
  }: {
    oneOf: Action.Global[]
    children: ReactNode
  }) {
    const { user } = useContext(UserContext)
    const isPermitted = useMemo(
      () =>
        user?.permittedGlobalActions.some((a) => oneOf.includes(a)) || false,
      [oneOf, user]
    )

    return isPermitted ? <>{children}</> : null
  }
)
