// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { differenceInMonths } from 'date-fns'
import certificates, { TrustedCertificates } from '../certificates'
import { pki } from 'node-forge'

describe('SAML certificates', () => {
  test('at least one certificate must exist', () => {
    expect(Object.keys(certificates).length).toBeGreaterThan(0)
  })
  for (const certificateName of Object.keys(
    certificates
  ) as Array<TrustedCertificates>) {
    test(`${certificateName} must decode successfully`, () => {
      const computeHash = false
      const strict = true
      const certificate = pki.certificateFromPem(
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
    })
  }
})
