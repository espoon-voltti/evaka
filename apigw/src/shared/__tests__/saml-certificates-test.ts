// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { differenceInMonths } from 'date-fns'
import * as invalidlyTypedForge from 'node-forge'

import certificates, { TrustedCertificates } from '../certificates'

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const forge = (invalidlyTypedForge as any)
  .default as typeof import('node-forge')

describe('SAML certificates', () => {
  test('at least one certificate must exist', () => {
    expect(Object.keys(certificates).length).toBeGreaterThan(0)
  })

  test.each(Object.keys(certificates) as TrustedCertificates[])(
    '%s must decode successfully',
    (certificateName) => {
      const computeHash = false
      const strict = true
      const certificate = forge.pki.certificateFromPem(
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
