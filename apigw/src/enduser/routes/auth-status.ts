// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { EvakaSessionUser } from '../../shared/auth/index.ts'
import { appCommit } from '../../shared/config.ts'
import { toRequestHandler } from '../../shared/express.ts'
import type { CitizenUserResponse } from '../../shared/service-client.ts'
import { getCitizenDetails } from '../../shared/service-client.ts'
import type { Sessions } from '../../shared/session.ts'

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

export const citizenAuthStatus = (sessions: Sessions<'citizen'>) =>
  toRequestHandler(async (req, res) => {
    const user = sessions.getUser(req)
    const data = user?.id ? await getCitizenDetails(req, user.id) : undefined
    if (user && data) {
      res.status(200).send({
        loggedIn: true,
        user: data,
        apiVersion: appCommit,
        authLevel: getAuthLevel(user)
      } satisfies AuthStatus)
      return
    }
    if (user && !data) {
      // Citizen no longer exists, log them out. This can happen e.g. in dev
      // environment if the database is reset
      await sessions.destroy(req, res)
    }
    res
      .status(200)
      .send({ loggedIn: false, apiVersion: appCommit } satisfies AuthStatus)
  })
