// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { getCitizenDetails } from '../../shared/service-client.js'
import { toRequestHandler } from '../../shared/express.js'
import { appCommit } from '../../shared/config.js'
import { EvakaSessionUser } from '../../shared/auth/index.js'

const getAuthLevel = (user: EvakaSessionUser): 'STRONG' | 'WEAK' => {
  switch (user.userType) {
    case 'CITIZEN_WEAK':
      return 'WEAK'
    case 'ENDUSER':
    case 'CITIZEN_STRONG':
      return 'STRONG'
    default:
      throw Error(`Invalid user type ${user.userType}`)
  }
}

export default toRequestHandler(async (req, res) => {
  if (req.user && req.user.id) {
    const data = await getCitizenDetails(req, req.user.id)
    res.status(200).send({
      loggedIn: true,
      user: data,
      apiVersion: appCommit,
      authLevel: getAuthLevel(req.user)
    })
  } else {
    res.status(200).send({ loggedIn: false, apiVersion: appCommit })
  }
})
