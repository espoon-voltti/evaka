// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { EmailVerificationStatusResponse } from 'lib-common/generated/api-types/pis'

import type { User } from '../auth/state'

export function isEmailVerified(
  status: EmailVerificationStatusResponse
): boolean {
  // Must compare the same way as sendEmailVerificationCode in
  // PersonalDataControllerCitizen.kt, which sends a code only for an unverified email
  return (
    !!status.email &&
    status.email.toLowerCase() === status.verifiedEmail?.toLowerCase()
  )
}

export interface EmailVerificationProblem {
  type: 'mismatch' | 'unverified'
  email: string
}

export function getEmailVerificationProblem(
  user: Pick<User, 'weakLoginUsername'>,
  status: EmailVerificationStatusResponse
): EmailVerificationProblem | undefined {
  const email = status.email
  if (!email) return undefined
  if (
    user.weakLoginUsername &&
    user.weakLoginUsername !== email.toLowerCase()
  ) {
    return { type: 'mismatch', email }
  }
  return isEmailVerified(status) ? undefined : { type: 'unverified', email }
}
