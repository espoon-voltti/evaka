// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { EMAIL_REGEXP, PHONE_REGEXP } from '../form-validation'

export function regexp<E extends string>(r: RegExp, error: E) {
  return (value: string): E | 'required' | undefined =>
    value === '' ? 'required' : r.test(value) ? undefined : error
}

export function regexpOrBlank<E extends string>(r: RegExp, error: E) {
  return (value: string): E | undefined =>
    value === '' ? undefined : r.test(value) ? undefined : error
}

export const optionalPhoneNumber = regexpOrBlank(PHONE_REGEXP, 'phone')
export const requiredPhoneNumber = regexp(PHONE_REGEXP, 'phone')

export const optionalEmail = regexpOrBlank(EMAIL_REGEXP, 'email')
export const requiredEmail = regexp(EMAIL_REGEXP, 'email')

export const nonBlank = (s: string) =>
  s.trim() === '' ? 'required' : undefined
