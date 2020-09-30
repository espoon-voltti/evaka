// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactNode, useContext } from 'react'
import { UserContext } from '~state/user'
import { AdRole } from '~types'

export const requireRole = (roles: AdRole[], ...requiresOneOf: AdRole[]) => {
  const requiredRoles: AdRole[] = [...requiresOneOf, 'ADMIN']
  return requiredRoles.some((role) => roles.includes(role))
}

export const hasRole = (roles: AdRole[], role: AdRole) => {
  return roles.some((r) => r === role)
}

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

export const ALL_ROLES_BUT_STAFF: AdRole[] = [
  'ADMIN',
  'SERVICE_WORKER',
  'FINANCE_ADMIN',
  'DIRECTOR',
  'UNIT_SUPERVISOR'
]
