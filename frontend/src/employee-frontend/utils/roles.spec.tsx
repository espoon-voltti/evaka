// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it } from 'vitest'

import type { User } from 'lib-common/api-types/employee-auth'

import { hasGlobalAction } from './roles'

const user = (
  permittedGlobalActions: User['permittedGlobalActions']
): User => ({
  id: '00000000-0000-0000-0000-000000000000' as User['id'],
  name: 'Test',
  userType: 'EMPLOYEE',
  accessibleFeatures: {} as User['accessibleFeatures'],
  permittedGlobalActions,
  startPage: 'SEARCH'
})

describe('hasGlobalAction', () => {
  it('returns true when the action is permitted', () => {
    expect(hasGlobalAction(user(['CREATE_UNIT']), 'CREATE_UNIT')).toBe(true)
  })

  it('returns false when the action is not permitted', () => {
    expect(hasGlobalAction(user(['REPORTS_PAGE']), 'CREATE_UNIT')).toBe(false)
  })

  it('returns false when the user is undefined', () => {
    expect(hasGlobalAction(undefined, 'CREATE_UNIT')).toBe(false)
  })
})
