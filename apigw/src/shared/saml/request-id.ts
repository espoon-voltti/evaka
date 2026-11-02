// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { inflateRawSync } from 'node:zlib'

import { XMLParser } from 'fast-xml-parser'

const parser = new XMLParser({
  ignoreAttributes: false,
  removeNSPrefix: true
})

export function authnRequestId(authorizeUrl: string): string | undefined {
  try {
    const samlRequest = new URL(authorizeUrl).searchParams.get('SAMLRequest')
    if (!samlRequest) return undefined
    const xml = inflateRawSync(Buffer.from(samlRequest, 'base64')).toString()
    // oxlint-disable-next-line typescript/no-unsafe-assignment
    const parsed = parser.parse(xml)
    // oxlint-disable-next-line typescript/no-unsafe-member-access,typescript/no-unsafe-assignment
    const id = parsed?.AuthnRequest?.['@_ID']
    return typeof id === 'string' && id.length > 0 ? id : undefined
  } catch {
    return undefined
  }
}
