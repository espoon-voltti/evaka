// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/**
 * Returns the ip address as an IPv4 address if the ip address is an IPv4-mapped IPv6 address. Otherwise, returns the
 * IPv6 address.
 */
export const ipv6ToIpv4 = (ipAddress: string): string =>
  ipAddress.substring(0, 7) === '::ffff:' ? ipAddress.substring(7) : ipAddress
