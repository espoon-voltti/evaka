// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { autocomplete } from './time'

describe('autocomplete', () => {
  it.each([
    // backspace
    ['12:00', '12:0', '12:0'],
    ['12:0', '12:', '12:'],
    ['12:', '12', '12'],
    ['12', '1', '1'],
    ['1', '', ''],
    // two digit hour
    ['0', '00', '00:'],
    ['0', '01', '01:'],
    ['0', '09', '09:'],
    ['1', '10', '10:'],
    ['1', '11', '11:'],
    ['1', '19', '19:'],
    ['2', '20', '20:'],
    ['2', '21', '21:'],
    ['2', '23', '23:'],
    // single digit hour
    ['', '3', '03:'],
    ['', '9', '09:'],
    // pad hour start after colon
    ['0', '0:', '00:'],
    ['1', '1:', '01:'],
    ['2', '2:', '02:'],
    ['3', '3:', '03:'],
    ['9', '9:', '09:'],
    // insert colon after third digit (state after backspacing a colon)
    ['00', '000', '00:0'],
    ['01', '011', '01:1'],
    ['09', '093', '09:3'],
    ['10', '100', '10:0'],
    ['20', '204', '20:4'],
    // convert periods into colons
    ['10', '10.', '10:'],
    ['0', '0.', '00:'],
    ['1', '1.', '01:'],
    ['9', '9.', '09:'],
    // ignore characters that aren't a digit, a colon or a period
    ['', 'a', ''],
    ['10', '10,', '10'],
    ['12', '12-', '12'],
    ['1', '1O', '1'],
    ['abc', 'abcd', '']
  ])(
    `autocomplete(previous: '%s', current: '%s') => '%s'`,
    (previous, current, expected) => {
      expect(autocomplete(previous, current)).toBe(expected)
    }
  )
})
