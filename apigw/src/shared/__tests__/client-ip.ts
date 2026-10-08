// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, test } from 'vitest'

import { clientIpKey } from '../client-ip.ts'

describe('clientIpKey', () => {
  test('uses an IPv4 address as is', () => {
    expect(clientIpKey('84.12.34.56')).toBe('84.12.34.56')
  })

  test('converts an IPv4-mapped IPv6 address to IPv4', () => {
    expect(clientIpKey('::ffff:84.12.34.56')).toBe('84.12.34.56')
  })

  test('uses the /64 prefix of an IPv6 address', () => {
    expect(clientIpKey('2001:db8:1:2:aaaa:bbbb:cccc:dddd')).toBe(
      '2001:db8:1:2::/64'
    )
  })

  test('gives the same key to all spellings of addresses in the same /64', () => {
    expect(clientIpKey('2001:0db8:0001:0002::1')).toBe('2001:db8:1:2::/64')
    expect(clientIpKey('2001:DB8:1:2:0:0:0:ffff')).toBe('2001:db8:1:2::/64')
  })

  test('gives different keys to different /64 prefixes', () => {
    expect(clientIpKey('2001:db8:1:3::1')).toBe('2001:db8:1:3::/64')
  })

  test('returns undefined when the address is missing or invalid', () => {
    expect(clientIpKey(undefined)).toBeUndefined()
    expect(clientIpKey('not-an-ip')).toBeUndefined()
  })
})
