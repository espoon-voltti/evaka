// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { EvakaSessionUser } from '../../shared/auth/index.js'
import { appCommit } from '../../shared/config.js'
import { toRequestHandler } from '../../shared/express.js'
import {
  CitizenUserResponse,
  getCitizenDetails
} from '../../shared/service-client.js'
import { Sessions } from '../../shared/session.js'

export interface AuthStatus {
  loggedIn: boolean
  user?: CitizenUserResponse
  apiVersion: string
  authLevel?: 'STRONG' | 'WEAK'
}

const getAuthLevel = (user: EvakaSessionUser): 'STRONG' | 'WEAK' => {
  switch (user.userType) {
    case 'CITIZEN_WEAK':
      return 'WEAK'
    case 'CITIZEN_STRONG':
      return 'STRONG'
    default:
      throw Error(`Invalid user type ${user.userType}`)
  }
}

export const authStatus = (sessions: Sessions) =>
  toRequestHandler(async (req, res) => {
    const user = sessions.getUser(req)
    let status: AuthStatus
    if (user && user.id) {
      const data = await getCitizenDetails(req, user.id)
      status = {
        loggedIn: true,
        user: data,
        apiVersion: appCommit,
        authLevel: getAuthLevel(user)
      }
    } else {
      status = { loggedIn: false, apiVersion: appCommit }
    }
    res.status(200).send(status)
  })
