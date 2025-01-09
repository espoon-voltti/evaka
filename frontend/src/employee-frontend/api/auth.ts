// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as Sentry from '@sentry/browser'

import {
  AuthStatus,
  MobileUser,
  User
} from 'lib-common/api-types/employee-auth'
import { JsonOf } from 'lib-common/json'

import { client } from './client'

export const logoutUrl = `/api/employee/auth/logout?RelayState=/employee/login`

const redirectUri = (() => {
  if (window.location.pathname === '/employee/login') {
    return '/employee'
  }

  const params = new URLSearchParams(window.location.search)
  params.delete('loginError')

  const searchParams = params.toString()

  return `${window.location.pathname}${
    searchParams.length > 0 ? '?' : ''
  }${searchParams}${window.location.hash}`
})()

export function getLoginUrl(type: 'ad' | 'keycloak' | 'sfi' = 'ad') {
  const relayState = encodeURIComponent(redirectUri)
  return `/api/employee/auth/${type}/login?RelayState=${relayState}`
}

export async function getAuthStatus(): Promise<AuthStatus<User>> {
  return client
    .get<JsonOf<AuthStatus<User | MobileUser>>>('/employee/auth/status')
    .then(({ data: { user, ...status } }) => {
      if (user?.userType === 'EMPLOYEE') {
        return {
          user,
          ...status
        }
      } else {
        if (user) {
          Sentry.captureMessage(
            `Invalid user type ${user.userType} in employee frontend`,
            'error'
          )
        }
        return { ...status, loggedIn: false }
      }
    })
}
