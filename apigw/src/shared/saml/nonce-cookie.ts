// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { sign, unsign } from 'cookie-signature'
import type express from 'express'

import type { SessionType } from '../session.ts'

export type NonceCookieConfig = {
  namePrefix: string
  path: string
  secret: string
}

export type NonceOutcome = 'match' | 'mismatch' | 'missing'

const cookieOptions = (config: NonceCookieConfig) =>
  ({
    httpOnly: true,
    secure: true,
    sameSite: 'none',
    path: config.path
  }) as const

const cookieName = (config: NonceCookieConfig, requestId: string) =>
  `${config.namePrefix}.${requestId}`

export const nonceCookiePrefix = (
  strategyName: string,
  sessionType: SessionType
): string => `evaka.saml.nonce.${strategyName}.${sessionType}`

export function setNonceCookie(
  res: express.Response,
  config: NonceCookieConfig,
  requestId: string
): void {
  res.cookie(
    cookieName(config, requestId),
    `s:${sign(requestId, config.secret)}`,
    { ...cookieOptions(config), maxAge: 60 * 60 * 1000, signed: false }
  )
}

export function checkNonceCookie(
  rawCookies: Record<string, unknown>,
  config: NonceCookieConfig,
  inResponseTo: string | undefined
): {
  outcome: NonceOutcome
  cookieId: string | undefined
  otherCookies: number
} {
  const name =
    inResponseTo !== undefined ? cookieName(config, inResponseTo) : undefined
  const otherCookies = Object.keys(rawCookies).filter(
    (n) => n.startsWith(`${config.namePrefix}.`) && n !== name
  ).length
  const raw = name !== undefined ? rawCookies[name] : undefined
  if (raw === undefined)
    return { outcome: 'missing', cookieId: undefined, otherCookies }
  const cookieId =
    typeof raw === 'string' && raw.startsWith('s:')
      ? unsign(raw.slice(2), config.secret) || undefined
      : undefined
  return {
    outcome: cookieId === inResponseTo ? 'match' : 'mismatch',
    cookieId,
    otherCookies
  }
}

export function clearNonceCookie(
  res: express.Response,
  config: NonceCookieConfig,
  requestId: string
): void {
  res.clearCookie(cookieName(config, requestId), cookieOptions(config))
}
