// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { passkeyProviderNamesByAaguid } from './passkey-provider-names'

// Authenticator data layout: rpIdHash (32 bytes), flags (1 byte),
// signCount (4 bytes), then attested credential data starting with
// the 16-byte AAGUID
const aaguidOffset = 37

function extractAaguid(authenticatorData: ArrayBuffer): string | undefined {
  if (authenticatorData.byteLength < aaguidOffset + 16) return undefined
  const bytes = new Uint8Array(authenticatorData, aaguidOffset, 16)
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

export function passkeyProviderName(
  response: AuthenticatorAttestationResponse
): string | undefined {
  if (typeof response.getAuthenticatorData !== 'function') return undefined
  const aaguid = extractAaguid(response.getAuthenticatorData())
  return aaguid ? passkeyProviderNamesByAaguid[aaguid] : undefined
}
