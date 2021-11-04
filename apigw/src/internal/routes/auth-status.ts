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
import { appCommit } from '../../shared/config'

export default toRequestHandler(async (req, res) => {
  const user = req.user
  if (user) {
    if (user.userType === 'MOBILE') {
      const device = await getMobileDevice(req, user.id)
      if (!device) {
        // device has been removed
        await logoutExpress(req, res, 'employee')
        res.status(200).json({ loggedIn: false, apiVersion: appCommit })
      } else {
        const globalRoles = user.globalRoles ?? []
        const allScopedRoles = user.allScopedRoles ?? ['MOBILE']
        const employeeId = user.mobileEmployeeId
        const { id, name, unitId } = device
        res.status(200).json({
          loggedIn: true,
          user: { id, name, unitId, employeeId },
          globalRoles,
          allScopedRoles,
          roles: [...globalRoles, ...allScopedRoles],
          apiVersion: appCommit
        })
      }
    } else {
      const {
        id,
        firstName,
        lastName,
        globalRoles,
        allScopedRoles,
        accessibleFeatures
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
        user: { id, name, accessibleFeatures: accessibleFeatures ?? {} },
        globalRoles,
        allScopedRoles,
        roles: [...globalRoles, ...allScopedRoles],
        apiVersion: appCommit
      })
    }
  } else {
    res.status(200).json({ loggedIn: false, apiVersion: appCommit })
  }
})
