// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { createHash, createHmac } from 'node:crypto'

import type express from 'express'

export const AUTH_HISTORY_COOKIE_PREFIX = '__Host-evaka-device-user-'

export const cookieSignatureOk = (
  hash: string,
  signature: string,
  cookieSecret: string
): boolean => {
  const hmac = createHmac('sha256', cookieSecret)
  hmac.update(hash)
  return hmac.digest('hex') === signature
}

export const filterValidDeviceAuthHistory = (
  cookies: Record<string, string>,
  cookieSecret: string,
  invalidSignatureCallback?: (cookieName: string, hash: string) => void
): string[] => {
  const validHashes: string[] = []

  Object.keys(cookies)
    .filter((name) => name.startsWith(AUTH_HISTORY_COOKIE_PREFIX))
    .forEach((cookieName) => {
      const signature = cookies[cookieName]
      const hash = cookieName.substring(AUTH_HISTORY_COOKIE_PREFIX.length)

      if (cookieSignatureOk(hash, signature, cookieSecret)) {
        validHashes.push(hash)
      } else if (invalidSignatureCallback) {
        invalidSignatureCallback(cookieName, hash)
      }
    })

  return validHashes
}

export const setDeviceAuthHistoryCookie = (
  res: express.Response,
  userId: string,
  cookieSecret: string
): void => {
  // create a new cookie for the user if it's a new browser
  const hashGenerator = createHash('sha256')
  hashGenerator.update(userId)
  const hash = hashGenerator.digest('hex')

  const cookieName = `${AUTH_HISTORY_COOKIE_PREFIX}${hash}`
  const signatureHmac = createHmac('sha256', cookieSecret)
  signatureHmac.update(hash)
  const signature = signatureHmac.digest('hex')
  res.cookie(cookieName, signature, {
    secure: true,
    httpOnly: true,
    sameSite: 'strict',
    expires: new Date(Date.now() + 90 * 24 * 60 * 60 * 1000) // 90 days
  })
}
