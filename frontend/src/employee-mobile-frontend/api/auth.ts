// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as Sentry from '@sentry/browser'

import { Failure, Result, Success } from 'lib-common/api'
import {
  AuthStatus,
  MobileUser,
  User
} from 'lib-common/api-types/employee-auth'
import { PinLoginResponse } from 'lib-common/generated/api-types/pairing'
import { JsonOf } from 'lib-common/json'

import { client } from './client'

export async function getAuthStatus(): Promise<Result<AuthStatus<MobileUser>>> {
  return client
    .get<JsonOf<AuthStatus<User | MobileUser>>>('/auth/status')
    .then(({ data: { user, ...status } }) => {
      if (user?.userType === 'MOBILE') {
        return Success.of({ user, ...status })
      } else {
        if (user) {
          Sentry.captureMessage(
            `Invalid user type ${user.userType} in mobile frontend`,
            Sentry.Severity.Error
          )
        }
        return Success.of({ ...status, loggedIn: false })
      }
    })
    .catch((e) => Failure.fromError(e))
}

export function pinLogin(
  employeeId: string,
  pin: string
): Promise<Result<PinLoginResponse>> {
  return client
    .post<JsonOf<PinLoginResponse>>(`/auth/pin-login`, {
      employeeId,
      pin
    })
    .then((res) => res.data)
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export function pinLogout(): Promise<Result<void>> {
  return client
    .post(`/auth/pin-logout`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
