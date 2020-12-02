// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from './client'
import { AdRole, User } from '~types'
import { JsonOf } from '@evaka/lib-common/src/json'

export const logoutUrl = `/api/internal/auth/saml/logout?RelayState=/employee/login`

const redirectUri =
  window.location.pathname === '/employee/login'
    ? '/employee'
    : `${window.location.pathname}${window.location.search}${window.location.hash}`

export function getLoginUrl(type: 'oidc' | 'saml' = 'saml') {
  const relayState = encodeURIComponent(redirectUri)
  return `/api/internal/auth/${type}/login?RelayState=${relayState}`
}

export interface AuthStatus {
  loggedIn: boolean
  user?: User
  roles?: AdRole[]
}

export async function getAuthStatus(): Promise<AuthStatus> {
  return client.get<JsonOf<AuthStatus>>('/auth/status').then((res) => res.data)
}
