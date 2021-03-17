// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import jwt from 'jsonwebtoken'
import { readFileSync } from 'fs'
import { jwtKid, jwtPrivateKey } from '../config'

const privateKey = readFileSync(jwtPrivateKey)

export function createJwt(payload: {
  kind: 'SuomiFI' | 'AD' | 'AdDummy'
  sub: string
  scope: string
}): string {
  return jwt.sign(payload, privateKey, {
    algorithm: 'RS256',
    expiresIn: '48h',
    keyid: jwtKid
  })
}
