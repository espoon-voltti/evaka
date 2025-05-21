// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { PasswordConstraints } from './generated/api-types/shared'
import { isPasswordStructureValid } from './password'

describe('isPasswordStructureValid', () => {
  const unconstrained: PasswordConstraints = {
    minLength: 1,
    maxLength: 128,
    minDigits: 0,
    minLowers: 0,
    minSymbols: 0,
    minUppers: 0
  }
  it('checks minLength correctly', () => {
    const constraints = { ...unconstrained, minLength: 4 }
    expect(isPasswordStructureValid(constraints, '123')).toBeFalsy()
    expect(isPasswordStructureValid(constraints, '1234')).toBeTruthy()
    expect(isPasswordStructureValid(constraints, '12345')).toBeTruthy()
  })
  it('checks maxLength correctly', () => {
    const constraints = { ...unconstrained, maxLength: 4 }
    expect(isPasswordStructureValid(constraints, '12345')).toBeFalsy()
    expect(isPasswordStructureValid(constraints, '1234')).toBeTruthy()
    expect(isPasswordStructureValid(constraints, '123')).toBeTruthy()
  })
  it('checks minLowers correctly', () => {
    const constraints = { ...unconstrained, minLowers: 1 }
    expect(isPasswordStructureValid(constraints, '1_2')).toBeFalsy()
    expect(isPasswordStructureValid(constraints, '1A2')).toBeFalsy()
    expect(isPasswordStructureValid(constraints, '1a2')).toBeTruthy()
    expect(isPasswordStructureValid(constraints, '1ab')).toBeTruthy()
    expect(isPasswordStructureValid(constraints, '1ä2')).toBeTruthy()
  })
  it('checks minUppers correctly', () => {
    const constraints = { ...unconstrained, minUppers: 1 }
    expect(isPasswordStructureValid(constraints, '1_2')).toBeFalsy()
    expect(isPasswordStructureValid(constraints, '1a2')).toBeFalsy()
    expect(isPasswordStructureValid(constraints, '1A2')).toBeTruthy()
    expect(isPasswordStructureValid(constraints, '1AB')).toBeTruthy()
    expect(isPasswordStructureValid(constraints, '1Ä2')).toBeTruthy()
  })
  it('checks minDigits correctly', () => {
    const constraints = { ...unconstrained, minDigits: 1 }
    expect(isPasswordStructureValid(constraints, 'abc')).toBeFalsy()
    expect(isPasswordStructureValid(constraints, 'a1c')).toBeTruthy()
    expect(isPasswordStructureValid(constraints, 'a12')).toBeTruthy()
  })
  it('checks minSymbols correctly', () => {
    const constraints = { ...unconstrained, minSymbols: 1 }
    expect(isPasswordStructureValid(constraints, '123')).toBeFalsy()
    expect(isPasswordStructureValid(constraints, 'abc')).toBeFalsy()
    expect(isPasswordStructureValid(constraints, 'a#c')).toBeTruthy()
    expect(isPasswordStructureValid(constraints, 'a#2')).toBeTruthy()
    expect(isPasswordStructureValid(constraints, '💩')).toBeTruthy()
  })
})
