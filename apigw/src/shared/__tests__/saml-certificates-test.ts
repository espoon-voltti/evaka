// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, test } from '@jest/globals'
import { differenceInMonths } from 'date-fns'
import nodeForge from 'node-forge'

import certificates, { TrustedCertificates } from '../certificates'

describe('SAML certificates', () => {
  test('at least one certificate must exist', () => {
    // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
    expect(Object.keys(certificates).length).toBeGreaterThan(0)
  })

  // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
  test.each(Object.keys(certificates) as TrustedCertificates[])(
    '%s must decode successfully',
    (certificateName) => {
      const computeHash = false
      const strict = true
      const certificate = nodeForge.pki.certificateFromPem(
        // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access,@typescript-eslint/no-unsafe-argument
        certificates[certificateName],
        computeHash,
        strict
      )
      const monthsToExpiry = differenceInMonths(
        certificate.validity.notAfter,
        new Date()
      )
      if (monthsToExpiry < 1) {
        console.warn(
          `⚠ Certificate ${certificateName} expired or about to expire! ⚠`
        )
      }
    }
  )
})
