// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import _ from 'lodash'
import * as validators from 'vuelidate/lib/validators'
import withParams from 'vuelidate/lib/withParams'

export const required = {
  validation: {
    required: validators.required
  },
  errorMsg: '#NAME# on vaadittu tieto'
}

export const isValidPostalCode = (value) =>
  _.isEmpty(value) || /^[0-9]{5}$/.test(value)

export const postalCode = {
  validation: {
    postalCode: withParams({ type: 'postalCode' }, isValidPostalCode)
  },
  errorMsg: 'Virheellinen postinumero'
}

export const isValidEmail = validators.email

export const email = {
  validation: {
    email: isValidEmail
  },
  errorMsg: 'Virheellinen sähköpostiosoite'
}

const CHECK_DIGITS = [
  '0',
  '1',
  '2',
  '3',
  '4',
  '5',
  '6',
  '7',
  '8',
  '9',
  'A',
  'B',
  'C',
  'D',
  'E',
  'F',
  'H',
  'J',
  'K',
  'L',
  'M',
  'N',
  'P',
  'R',
  'S',
  'T',
  'U',
  'V',
  'W',
  'X',
  'Y'
]

export const isValidIdentityNumber = (value) => {
  if (_.isEmpty(value)) {
    return true
  }
  const matches = value.match(
    /^(\d{2})(\d{2})(\d{2})[A+-](\d{3})[\dA-Z]$/
  )
  if (matches) {
    const birthNumber = value.substring(0, 6)
    const identityNumber = value.substring(7, 10)
    const checkDigit = CHECK_DIGITS[parseInt(birthNumber + identityNumber) % 31]
    return value.slice(-1) === checkDigit
  }
  return false
}

export const identityNumber = {
  validation: {
    identityNumber: withParams(
      { type: 'identityNumber' },
      isValidIdentityNumber
    )
  },
  errorMsg: 'Virheellinen henkilötunnus'
}

export const isValidPhoneNumber = (value) =>
  value ? /^\+?\d{6,}$/.test(value) : true

export const phoneNumber = {
  validation: {
    phoneNumber: withParams({ type: 'phoneNumber' }, isValidPhoneNumber)
  },
  errorMsg: 'Virheellinen puhelinnumero'
}

export const isValidTimeString = (value) => {
  if (!value) return true
  const isCorrectForm = /^\d{1,2}:\d{2}$/.test(value)
  if (!isCorrectForm) return false
  const [hours, mins] = value.split(':').map((item) => parseInt(item))
  return hours >= 0 && hours < 24 && mins >= 0 && mins < 60
}
