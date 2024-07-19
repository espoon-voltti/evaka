// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { EvakaSessionUser } from '../../shared/auth/index.js'
import { appCommit } from '../../shared/config.js'
import { getDatabaseId } from '../../shared/dev-api.js'
import { toRequestHandler } from '../../shared/express.js'
import {
  CitizenUserResponse,
  getCitizenDetails
} from '../../shared/service-client.js'

export interface AuthStatus {
  loggedIn: boolean
  antiCsrfToken?: string
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

export default toRequestHandler(async (req, res) => {
  let status: AuthStatus
  if (req.user && req.user.id) {
    const data = await getCitizenDetails(req, req.user.id, getDatabaseId(req))
    status = {
      loggedIn: true,
      antiCsrfToken: req.session.antiCsrfToken,
      user: data,
      apiVersion: appCommit,
      authLevel: getAuthLevel(req.user)
    }
  } else {
    status = { loggedIn: false, apiVersion: appCommit }
  }
  res.status(200).send(status)
})
