// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import ipaddr from 'ipaddr.js'

// An IPv6 client can freely switch addresses within its /64 prefix, so the
// whole prefix identifies one client
export function clientIpKey(ip: string | undefined): string | undefined {
  if (ip === undefined || !ipaddr.isValid(ip)) return undefined
  const address = ipaddr.process(ip)
  if (address instanceof ipaddr.IPv4) return address.toString()
  const prefix = new ipaddr.IPv6([...address.parts.slice(0, 4), 0, 0, 0, 0])
  return `${prefix.toString()}/64`
}
