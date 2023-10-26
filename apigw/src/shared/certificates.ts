// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const names = [
  'espooad-internal-prod.2022.pem',
  'espooad-internal-staging.2022.pem',
  'idp.test.espoon-voltti.fi.pem',
  'saml-signing.idp.tunnistautuminen.suomi.fi.2022.pem',
  'saml-signing-testi.apro.tunnistus.fi.2022.pem'
] as const

export type TrustedCertificates = (typeof names)[number]

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const certificates: Record<TrustedCertificates, string> = {} as any
for (const name of names) {
  certificates[name] = fs.readFileSync(
    path.resolve(__dirname, '../../config/certificates', name),
    'utf-8'
  )
}

export default certificates
