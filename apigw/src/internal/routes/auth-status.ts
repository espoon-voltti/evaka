// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { eq } from 'lodash'
import { toRequestHandler } from '../../shared/express'
import {
  getEmployeeDetails,
  getMobileDevice
} from '../../shared/service-client'
import { logoutExpress } from '../../shared/session'
import { fromCallback } from '../../shared/promise-utils'

export default toRequestHandler(async (req, res) => {
  const user = req.user
  if (user) {
    if (user.userType === 'MOBILE') {
      const device = await getMobileDevice(req, user.id)
      const globalRoles = user.globalRoles ?? []
      const allScopedRoles = user.allScopedRoles ?? ['MOBILE']
      if (device) {
        const { id, name, unitId } = device
        res.status(200).json({
          loggedIn: true,
          user: { id, name, unitId },
          globalRoles,
          allScopedRoles,
          roles: [...globalRoles, ...allScopedRoles]
        })
      } else {
        // device has been removed
        await logoutExpress(req, res, 'employee')
        res.status(200).json({ loggedIn: false })
      }
    } else {
      const {
        id,
        firstName,
        lastName,
        globalRoles,
        allScopedRoles
      } = await getEmployeeDetails(req, user.id)
      const name = [firstName, lastName].filter((x) => !!x).join(' ')

      // Refresh roles if necessary
      if (
        !eq(user.globalRoles, globalRoles) ||
        !eq(user.allScopedRoles, allScopedRoles)
      ) {
        await fromCallback((cb) =>
          req.logIn({ ...user, globalRoles, allScopedRoles }, cb)
        )
      }

      res.status(200).json({
        loggedIn: true,
        user: { id, name },
        globalRoles,
        allScopedRoles,
        roles: [...globalRoles, ...allScopedRoles]
      })
    }
  } else {
    res.status(200).json({ loggedIn: false })
  }
})
