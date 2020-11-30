// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { toRequestHandler } from '../../shared/express'
import { getEmployeeDetails } from '../../shared/service-client'

export default toRequestHandler(async (req, res) => {
  const user = req.user
  if (user) {
    const data = await getEmployeeDetails(req, user.id)
    res.status(200).json({
      loggedIn: true,
      user: data,
      roles: user.roles
    })
  } else {
    res.status(200).json({ loggedIn: false })
  }
})
