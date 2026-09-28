// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it } from 'vitest'

import { hasSubMenuAttention } from './attention'

describe('hasSubMenuAttention', () => {
  it.each<[boolean, number, boolean]>([
    [false, 0, false],
    [true, 0, true],
    [false, 1, true],
    [true, 3, true]
  ])(
    'with personal details tasks %s and %i unread decisions is %s',
    (hasPersonalDetailsTasks, unreadDecisions, expected) => {
      expect(
        hasSubMenuAttention({ hasPersonalDetailsTasks, unreadDecisions })
      ).toBe(expected)
    }
  )
})
