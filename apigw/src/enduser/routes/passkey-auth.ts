// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'
import {
  generateAuthenticationOptions,
  generateRegistrationOptions,
  verifyAuthenticationResponse,
  verifyRegistrationResponse
} from '@simplewebauthn/server'
import type { AuthenticatorTransportFuture } from '@simplewebauthn/server'

import type { Config } from '../../shared/config.ts'
import type { RedisClient } from '../../shared/redis-client.ts'
import {
  listCitizenPasskeyCredentials,
  touchCitizenPasskeyCredential,
  upsertCitizenPasskeyCredential
} from '../../shared/service-client-passkey.ts'
import type { Sessions } from '../../shared/session.ts'
import {
  generateToken,
  putChallenge,
  takeChallenge
} from './passkey-challenge-store.ts'

function personIdToUserHandle(personId: string): Uint8Array<ArrayBuffer> {
  const hex = personId.replace(/-/g, '')
  const buf = new ArrayBuffer(16)
  const bytes = new Uint8Array(buf)
  for (let i = 0; i < 16; i++) {
    bytes[i] = parseInt(hex.substring(i * 2, i * 2 + 2), 16)
  }
  return bytes
}

function userHandleToPersonId(
  userHandleBase64: string | undefined
): string | null {
  if (!userHandleBase64) return null
  const raw = Buffer.from(userHandleBase64, 'base64url')
  if (raw.length !== 16) return null
  const hex = raw.toString('hex')
  return `${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20, 32)}`
}

function deviceHintFromUserAgent(ua: string | undefined): string | null {
  if (!ua) return null
  const lc = ua.toLowerCase()
  if (lc.includes('iphone')) return 'iPhone (Safari)'
  if (lc.includes('ipad')) return 'iPad (Safari)'
  if (lc.includes('android')) return 'Android (Chrome)'
  if (lc.includes('macintosh')) return 'Mac (Safari)'
  if (lc.includes('windows')) return 'Windows (Chrome)'
  return 'Unknown device'
}

export function passkeyAuthRoutes(
  config: Config,
  sessions: Sessions<'citizen'>,
  redis: RedisClient
): express.Router {
  const router = express.Router()

  // POST /register/options — start registration ceremony
  router.post('/register/options', express.json(), async (req, res) => {
    const user = req.user
    if (!user || user.userType !== 'CITIZEN_STRONG') {
      res.status(403).json({ code: 'strong-auth-required' })
      return
    }

    const existing = await listCitizenPasskeyCredentials(req, user.id)
    const options = await generateRegistrationOptions({
      rpName: config.passkey.rpName,
      rpID: config.passkey.rpId,
      userName: user.id,
      userID: personIdToUserHandle(user.id),
      attestationType: 'none',
      authenticatorSelection: {
        residentKey: 'required',
        userVerification: 'preferred'
      },
      excludeCredentials: existing.map((c) => ({
        id: c.credentialId,
        transports: c.transports as AuthenticatorTransportFuture[]
      }))
    })

    const token = generateToken()
    await putChallenge(redis, token, {
      challenge: options.challenge,
      flow: 'register',
      personId: user.id
    })

    res.json({ token, options })
  })

  // POST /register/verify — complete registration ceremony
  router.post('/register/verify', express.json(), async (req, res) => {
    const user = req.user
    if (!user || user.userType !== 'CITIZEN_STRONG') {
      res.status(403).json({ code: 'strong-auth-required' })
      return
    }

    const { token, attestation } = req.body as {
      token: string
      attestation: unknown
    }

    const entry = await takeChallenge(redis, token)
    if (!entry || entry.flow !== 'register') {
      res.status(410).json({ code: 'passkey-challenge-expired' })
      return
    }

    if (entry.personId !== user.id) {
      res.status(403).json({ code: 'strong-auth-required' })
      return
    }

    let verified: boolean
    let registrationInfo: {
      credential: { id: string; publicKey: Uint8Array; counter: number }
    } | undefined

    try {
      const result = await verifyRegistrationResponse({
        response: attestation as Parameters<
          typeof verifyRegistrationResponse
        >[0]['response'],
        expectedChallenge: entry.challenge,
        expectedOrigin: config.passkey.origin,
        expectedRPID: config.passkey.rpId
      })
      verified = result.verified
      registrationInfo = result.registrationInfo as typeof registrationInfo
    } catch {
      res.status(401).json({ code: 'passkey-verification-failed' })
      return
    }

    if (!verified || !registrationInfo) {
      res.status(401).json({ code: 'passkey-verification-failed' })
      return
    }

    const { credential } = registrationInfo
    const deviceHint = deviceHintFromUserAgent(req.get('User-Agent'))
    const transportsRaw = (
      attestation as { response?: { transports?: string[] } }
    ).response?.transports

    await upsertCitizenPasskeyCredential(req, {
      personId: user.id,
      credentialId: credential.id,
      publicKey: Buffer.from(credential.publicKey).toString('base64url'),
      signCounter: credential.counter,
      transports: transportsRaw ?? [],
      label: deviceHint ?? 'Passkey',
      deviceHint
    })

    res.json({ credentialId: credential.id, label: deviceHint ?? 'Passkey' })
  })

  // POST /login/options — start login ceremony
  router.post('/login/options', express.json(), async (req, res) => {
    const options = await generateAuthenticationOptions({
      rpID: config.passkey.rpId,
      userVerification: 'preferred'
    })

    const token = generateToken()
    await putChallenge(redis, token, {
      challenge: options.challenge,
      flow: 'login'
    })

    res.json({ token, options })
  })

  // POST /login/verify — complete login ceremony
  router.post('/login/verify', express.json(), async (req, res) => {
    const { token, assertion } = req.body as {
      token: string
      assertion: unknown
    }

    const entry = await takeChallenge(redis, token)
    if (!entry || entry.flow !== 'login') {
      res.status(410).json({ code: 'passkey-challenge-expired' })
      return
    }

    const assertionObj = assertion as {
      id: string
      response: { userHandle?: string }
    }

    const personId = userHandleToPersonId(assertionObj.response?.userHandle)
    if (!personId) {
      res.status(401).json({ code: 'passkey-verification-failed' })
      return
    }

    const credentials = await listCitizenPasskeyCredentials(req, personId)
    const stored = credentials.find((c) => c.credentialId === assertionObj.id)
    if (!stored) {
      res.status(401).json({ code: 'passkey-verification-failed' })
      return
    }

    let verified: boolean
    let newCounter: number

    try {
      const result = await verifyAuthenticationResponse({
        response: assertion as Parameters<
          typeof verifyAuthenticationResponse
        >[0]['response'],
        expectedChallenge: entry.challenge,
        expectedOrigin: config.passkey.origin,
        expectedRPID: config.passkey.rpId,
        credential: {
          id: stored.credentialId,
          publicKey: Buffer.from(stored.publicKey, 'base64url'),
          counter: stored.signCounter,
          transports: stored.transports as AuthenticatorTransportFuture[]
        }
      })
      verified = result.verified
      newCounter = result.authenticationInfo.newCounter
    } catch {
      res.status(401).json({ code: 'passkey-verification-failed' })
      return
    }

    if (!verified) {
      res.status(401).json({ code: 'passkey-verification-failed' })
      return
    }

    await touchCitizenPasskeyCredential(
      req,
      personId,
      stored.credentialId,
      newCounter
    )

    await sessions.login(req, {
      id: personId,
      authType: 'citizen-passkey',
      userType: 'CITIZEN_WEAK',
      credentialId: stored.credentialId
    })

    res.sendStatus(200)
  })

  return router
}
