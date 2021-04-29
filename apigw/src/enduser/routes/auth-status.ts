// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { getUserDetails } from '../../shared/service-client'
import { toRequestHandler } from '../../shared/express'

export default toRequestHandler(async (req, res) => {
  if (req.user) {
    const data = await getUserDetails(req, req.user.id)
    data.userType = req.user.userType
    res.status(200).send({ loggedIn: true, user: data })
  } else {
    res.status(200).send({ loggedIn: false })
  }
})
