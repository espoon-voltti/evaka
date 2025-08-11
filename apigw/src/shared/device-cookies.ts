// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { createHash } from 'node:crypto'

import type express from 'express'

export const AUTH_HISTORY_COOKIE_PREFIX = '__Host-evaka-device-user-'

export const filterValidDeviceAuthHistory = (
  signedCookies: Record<string, unknown>,
  invalidSignatureCallback?: (cookieName: string, hash: string) => void
): string[] => {
  return Object.entries(signedCookies).reduce<string[]>(
    (acc, [name, value]) => {
      if (!name.startsWith(AUTH_HISTORY_COOKIE_PREFIX)) return acc
      const hash = name.substring(AUTH_HISTORY_COOKIE_PREFIX.length)
      if (value === false) invalidSignatureCallback?.(name, hash)
      else acc.push(hash)
      return acc
    },
    []
  )
}

export const setDeviceAuthHistoryCookie = (
  res: express.Response,
  userId: string
): void => {
  // create a new cookie for the user if it's a new browser
  const hashGenerator = createHash('sha256')
  hashGenerator.update(userId)
  const hash = hashGenerator.digest('hex')

  const cookieName = `${AUTH_HISTORY_COOKIE_PREFIX}${hash}`
  res.cookie(cookieName, hash, {
    secure: true,
    httpOnly: true,
    sameSite: 'strict',
    signed: true,
    expires: new Date(Date.now() + 90 * 24 * 60 * 60 * 1000) // 90 days
  })
}
