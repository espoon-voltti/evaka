// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { eq } from 'lodash'
import { toRequestHandler } from '../../shared/express'
import {
  getEmployeeDetails,
  getMobileDevice
} from '../../shared/service-client'
import { logoutExpress, saveSession } from '../../shared/session'
import { fromCallback } from '../../shared/promise-utils'
import { appCommit } from '../../shared/config'

export default toRequestHandler(async (req, res) => {
  const user = req.user
  if (user && user.id) {
    if (user.userType === 'MOBILE') {
      const device = await getMobileDevice(req, user.id)
      if (!device) {
        // device has been removed
        await logoutExpress(req, res, 'employee')
        res.status(200).json({ loggedIn: false, apiVersion: appCommit })
      } else {
        const globalRoles = user.globalRoles ?? []
        const allScopedRoles = user.allScopedRoles ?? ['MOBILE']
        const employeeId = device.employeeId ?? user.mobileEmployeeId
        const pinLoginActive = user.mobileEmployeeId
        const { id, name, unitIds, personalDevice } = device
        await saveSession(req)
        res.status(200).json({
          loggedIn: true,
          user: {
            id,
            name,
            userType: user.userType,
            unitIds,
            employeeId,
            pinLoginActive,
            personalDevice
          },
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
        accessibleFeatures,
        permittedGlobalActions
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

      await saveSession(req)
      res.status(200).json({
        loggedIn: true,
        user: {
          id,
          name,
          userType: user.userType,
          accessibleFeatures: accessibleFeatures ?? {},
          permittedGlobalActions: permittedGlobalActions ?? []
        },
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
