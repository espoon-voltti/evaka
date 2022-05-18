// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AuthStatus,
  MobileUser,
  User
} from 'lib-common/api-types/employee-auth'
import { JsonOf } from 'lib-common/json'

import { client } from './client'

export const logoutUrl = `/api/internal/auth/saml/logout?RelayState=/employee/login`

const redirectUri =
  window.location.pathname === '/employee/login'
    ? '/employee'
    : `${window.location.pathname}${window.location.search}${window.location.hash}`

export function getLoginUrl(type: 'evaka' | 'saml' = 'saml') {
  const relayState = encodeURIComponent(redirectUri)
  return `/api/internal/auth/${type}/login?RelayState=${relayState}`
}

export async function getAuthStatus(): Promise<AuthStatus<User>> {
  return client
    .get<JsonOf<AuthStatus<User | MobileUser>>>('/auth/status')
    .then(({ data: { user, ...status } }) =>
      user?.userType === 'EMPLOYEE'
        ? {
            user: {
              ...user,
              permittedGlobalActions: new Set(user.permittedGlobalActions)
            },
            ...status
          }
        : {
            ...status,
            loggedIn: false
          }
    )
}
