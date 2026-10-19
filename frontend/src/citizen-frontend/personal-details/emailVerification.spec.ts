// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it } from 'vitest'

import type { EmailVerificationStatusResponse } from 'lib-common/generated/api-types/pis'

import type { EmailVerificationProblem } from './emailVerification'
import {
  getEmailVerificationProblem,
  isEmailVerified
} from './emailVerification'

const status = (
  email: string | null,
  verifiedEmail: string | null
): EmailVerificationStatusResponse => ({
  email,
  verifiedEmail,
  latestVerification: null
})

describe('getEmailVerificationProblem', () => {
  it.each<{
    scenario: string
    weakLoginUsername: string | null
    emailVerification: EmailVerificationStatusResponse
    expected: EmailVerificationProblem['type'] | undefined
  }>([
    {
      scenario: 'no email',
      weakLoginUsername: null,
      emailVerification: status(null, null),
      expected: undefined
    },
    {
      scenario: 'weak login, email equals verified email',
      weakLoginUsername: 'citizen@example.com',
      emailVerification: status('citizen@example.com', 'citizen@example.com'),
      expected: undefined
    },
    {
      scenario: 'weak login, email differs from verified email only by case',
      weakLoginUsername: 'citizen@example.com',
      emailVerification: status('Citizen@Example.com', 'citizen@example.com'),
      expected: undefined
    },
    {
      scenario:
        'weak login, no verified email, username matches lowercased email',
      weakLoginUsername: 'citizen@example.com',
      emailVerification: status('Citizen@Example.com', null),
      expected: 'unverified'
    },
    {
      scenario: 'weak login, username is a different address',
      weakLoginUsername: 'old@example.com',
      emailVerification: status('new@example.com', 'old@example.com'),
      expected: 'mismatch'
    },
    {
      scenario: 'no weak login, unverified',
      weakLoginUsername: null,
      emailVerification: status('new@example.com', 'old@example.com'),
      expected: 'unverified'
    },
    {
      scenario: 'no weak login, verified',
      weakLoginUsername: null,
      emailVerification: status('citizen@example.com', 'citizen@example.com'),
      expected: undefined
    },
    {
      scenario: 'no weak login, email differs from verified email only by case',
      weakLoginUsername: null,
      emailVerification: status('Citizen@Example.com', 'citizen@example.com'),
      expected: undefined
    }
  ])('$scenario', ({ weakLoginUsername, emailVerification, expected }) => {
    expect(
      getEmailVerificationProblem({ weakLoginUsername }, emailVerification)
        ?.type
    ).toBe(expected)
  })

  it('agrees with isEmailVerified whenever an email is set', () => {
    const addresses = [
      'citizen@example.com',
      'Citizen@Example.com',
      'other@example.com'
    ]
    const combinations = [null, 'citizen@example.com', 'other@example.com']
      .flatMap((weakLoginUsername) =>
        addresses.flatMap((email) =>
          [null, ...addresses].map((verifiedEmail) => ({
            weakLoginUsername,
            emailVerification: status(email, verifiedEmail)
          }))
        )
      )
      .map(({ weakLoginUsername, emailVerification }) => ({
        weakLoginUsername,
        emailVerification,
        problem: getEmailVerificationProblem(
          { weakLoginUsername },
          emailVerification
        )
      }))

    // A username mismatch has its own notice in the contact details, and the
    // email can be verified at the same time, so it is not compared here
    const disagreements = combinations.filter(
      ({ emailVerification, problem }) =>
        problem?.type !== 'mismatch' &&
        (problem === undefined) !== isEmailVerified(emailVerification)
    )
    expect(disagreements).toEqual([])
  })
})
