// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Success } from 'lib-common/api'

export const PinLoginRequired: unique symbol = Symbol('pin-login-required')
export type PinLoginRequired = typeof PinLoginRequired

export const mapPinLoginRequiredError = (
  e: unknown
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
): Failure<any> | Success<PinLoginRequired> => {
  const failure = Failure.fromError(e)
  return failure.errorCode === 'PIN_LOGIN_REQUIRED'
    ? Success.of(PinLoginRequired)
    : failure
}
