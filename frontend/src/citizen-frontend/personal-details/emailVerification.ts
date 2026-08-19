// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { EmailVerificationStatusResponse } from 'lib-common/generated/api-types/pis'

import type { User } from '../auth/state'

export function isEmailVerified(
  status: EmailVerificationStatusResponse
): boolean {
  return !!status.email && status.email === status.verifiedEmail
}

export interface EmailVerificationProblem {
  type: 'mismatch' | 'unverified'
  email: string
}

export function getEmailVerificationProblem(
  user: User,
  status: EmailVerificationStatusResponse
): EmailVerificationProblem | undefined {
  const email = status.email
  if (!email) return undefined
  if (user.weakLoginUsername) {
    return user.weakLoginUsername !== email.toLowerCase()
      ? { type: 'mismatch', email }
      : undefined
  }
  return email !== status.verifiedEmail
    ? { type: 'unverified', email }
    : undefined
}
