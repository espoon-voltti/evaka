// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const badPins = [
  '1234',
  '0000',
  '1111',
  '2222',
  '3333',
  '4444',
  '5555',
  '6666',
  '7777',
  '8888',
  '9999'
]

export function isValidPinCode(pin: string) {
  return !badPins.includes(pin) && /^\d{4}$/.test(pin)
}
