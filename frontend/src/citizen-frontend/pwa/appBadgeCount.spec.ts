// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it } from 'vitest'

import { appBadgeCount } from './appBadgeCount'

describe('appBadgeCount', () => {
  it('is unknown when logged out', () => {
    expect(appBadgeCount(false, [1, 2])).toBeUndefined()
  })

  it('is unknown while any count is loading', () => {
    expect(appBadgeCount(true, [1, 'loading', 2])).toBeUndefined()
  })

  it('sums the counts', () => {
    expect(appBadgeCount(true, [1, 0, 2, 3])).toBe(6)
  })

  it('counts a failed source as zero', () => {
    expect(appBadgeCount(true, [1, 'failed', 2])).toBe(3)
  })
})
