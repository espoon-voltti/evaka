// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { formatCents, isValidCents, parseCents } from '../utils/money'

describe('utils/money', () => {
  describe('formatCents', () => {
    it('1', () => {
      expect(formatCents(1)).toBe('0,01')
    })

    it('10', () => {
      expect(formatCents(10)).toBe('0,10')
    })

    it('100', () => {
      expect(formatCents(100)).toBe('1')
    })

    it('101', () => {
      expect(formatCents(101)).toBe('1,01')
    })

    it('10050', () => {
      expect(formatCents(10050)).toBe('100,50')
    })

    it('129371823', () => {
      expect(formatCents(129371823)).toBe('1293718,23')
    })

    it('undefined', () => {
      expect(formatCents(undefined)).toBe(undefined)
    })

    it('-10050', () => {
      expect(formatCents(-10050)).toBe('-100,50')
    })
  })

  describe('isValidCents', () => {
    it('0,01', () => {
      expect(isValidCents('0,01')).toBe(true)
    })

    it('0,1', () => {
      expect(isValidCents('0,1')).toBe(true)
    })

    it('1', () => {
      expect(isValidCents('1')).toBe(true)
    })

    it('1,01', () => {
      expect(isValidCents('1,01')).toBe(true)
    })

    it('1293718,23', () => {
      expect(isValidCents('1293718,23')).toBe(true)
    })

    it('empty string', () => {
      expect(isValidCents('')).toBe(false)
    })

    it('"akjsdfh"', () => {
      expect(isValidCents('akjsdfh')).toBe(false)
    })

    it('1,001', () => {
      expect(isValidCents('1,001')).toBe(false)
    })

    it('-1,01', () => {
      expect(isValidCents('-1,01')).toBe(true)
    })
  })

  describe('parseCents', () => {
    it('0,01', () => {
      expect(parseCents('0,01')).toBe(1)
    })

    it('0,1', () => {
      expect(parseCents('0,1')).toBe(10)
    })

    it('1', () => {
      expect(parseCents('1')).toBe(100)
    })

    it('1,01', () => {
      expect(parseCents('1,01')).toBe(101)
    })

    it('100,5', () => {
      expect(parseCents('100,5')).toBe(10050)
    })

    it('1293718,23', () => {
      expect(parseCents('1293718,23')).toBe(129371823)
    })

    it('empty string', () => {
      expect(parseCents('')).toBe(undefined)
    })

    it('"akjsdfh"', () => {
      expect(parseCents('akjsdfh')).toBe(undefined)
    })

    it('1,001', () => {
      expect(parseCents('1,001')).toBe(undefined)
    })

    it('-1,01', () => {
      expect(parseCents('-1,01')).toBe(-101)
    })
  })
})
