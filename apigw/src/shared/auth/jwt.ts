// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { readFileSync } from 'node:fs'

import jwt from 'jsonwebtoken'

import { jwtKid, jwtPrivateKey, nodeEnv } from '../config.js'

const privateKey = readFileSync(jwtPrivateKey)
const jwtLifetimeSeconds = 60 * 60 // 1 hour
let jwtToken: string | undefined

export function getJwt(): string {
  if (!jwtToken) {
    jwtToken = jwt.sign({}, privateKey, {
      algorithm: 'RS256',
      expiresIn: jwtLifetimeSeconds,
      keyid: jwtKid
    })

    if (nodeEnv !== 'test') {
      // Calculate a new JWT one minute before the old one expires
      setTimeout(
        () => {
          jwtToken = undefined
        },
        (jwtLifetimeSeconds - 60) * 1000
      )
    }
  }
  return jwtToken
}
