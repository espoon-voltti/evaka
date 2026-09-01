// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import useLocalStorage from 'lib-common/utils/useLocalStorage'

export type LastLoginMethod = 'passkey' | 'email'

const key = 'evaka-citizen.last-login-method'

export function rememberLastLoginMethod(method: LastLoginMethod): void {
  try {
    window.localStorage?.setItem(key, method)
  } catch {
    // ignore
  }
}

export function forgetLastLoginMethod(method: LastLoginMethod): void {
  try {
    if (window.localStorage?.getItem(key) === method) {
      window.localStorage.removeItem(key)
    }
  } catch {
    // ignore
  }
}

export function useLastLoginMethod(): LastLoginMethod | undefined {
  const [value] = useLocalStorage(
    key,
    '',
    (v): v is LastLoginMethod | '' =>
      v === 'passkey' || v === 'email' || v === ''
  )
  return value === '' ? undefined : value
}
