// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ssn } from './form-validation'

describe('form-validation', () => {
  describe('ssn', () => {
    it('passes with valid SSN', () => {
      ;[
        '240514A1831',
        '240514B1831',
        '240514-1831',
        '240514U1831',
        '240514+1831'
      ].forEach((str) => {
        expect(ssn(str)).toEqual(undefined)
      })
    })

    it('fails if does not match regexp', () => {
      const str = '240514:1831'
      expect(ssn(str)).toEqual('ssn')
    })

    it('fails if date is invalid', () => {
      const str = '320514A1836'
      expect(ssn(str)).toEqual('ssn')
    })

    it('fails if check char is invalid', () => {
      const str = '240514A1836'
      expect(ssn(str)).toEqual('ssn')
    })
  })
})
