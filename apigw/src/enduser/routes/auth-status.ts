// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { getUserDetails } from '../../shared/service-client.js'
import { toRequestHandler } from '../../shared/express.js'
import { appCommit } from '../../shared/config.js'

export default toRequestHandler(async (req, res) => {
  if (req.user && req.user.id) {
    const data = await getUserDetails(req, req.user.id)
    res.status(200).send({ loggedIn: true, user: data, apiVersion: appCommit })
  } else {
    res.status(200).send({ loggedIn: false, apiVersion: appCommit })
  }
})
