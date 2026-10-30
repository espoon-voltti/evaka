// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sum from 'lodash/sum'

export type AppBadgeSource = number | 'loading' | 'failed'

export function appBadgeCount(
  loggedIn: boolean,
  sources: AppBadgeSource[]
): number | undefined {
  if (!loggedIn || sources.includes('loading')) return undefined
  return sum(sources.map((source) => (source === 'failed' ? 0 : source)))
}
