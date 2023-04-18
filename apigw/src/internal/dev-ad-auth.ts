// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Router } from 'express'
import { assertStringProp } from '../shared/express'
import _ from 'lodash'
import {
  getEmployeeByExternalId,
  getEmployees,
  upsertEmployee
} from '../shared/dev-api'
import { createDevAuthRouter } from '../shared/auth/dev-auth'
import {
  employeeLogin,
  EmployeeLoginRequest,
  UserRole
} from '../shared/service-client'

export function createDevAdRouter(): Router {
  return createDevAuthRouter({
    sessionType: 'employee',
    root: '/employee',
    strategyName: 'dev-ad',
    loginFormHandler: async (req, res) => {
      const employees = _.sortBy(await getEmployees(), ({ id }) => id)
      const employeeInputs = employees
        .map(({ externalId, firstName, lastName }) => {
          if (!externalId) return ''
          const [_, aad] = externalId.split(':')
          return `<div><input type="radio" id="${aad}" name="preset" value="${aad}" /><label for="${aad}">${firstName} ${lastName}</label></div>`
        })
        .filter((line) => !!line)

      const formQuery =
        typeof req.query.RelayState === 'string'
          ? `?RelayState=${encodeURIComponent(req.query.RelayState)}`
          : ''
      const formUri = `${req.baseUrl}/login/callback${formQuery}`

      res.contentType('text/html').send(`
          <html>
          <body>
            <h1>Devausympäristön AD-kirjautuminen</h1>
            <form action="${formUri}" method="post">
                ${employeeInputs.join('\n')}
                <div style="margin-top: 20px">
                  <input type="radio" id="custom" name="preset" value="custom" checked/><label for="custom">Custom (täytä tiedot alle)</label>
                </div>
                <h2>Custom</h2>
                <label for="aad">AAD: </label>
                <input id="aad-input" name="aad" value="cf5bcd6e-3d0e-4d8e-84a0-5ae2e4e65034" required
                    pattern="[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"/>
                <div>
                  <label for="firstName">Etunimi: </label>
                  <input name="firstName" value="Seppo"/>
                </div>
                <div>
                  <label for="lastName">Sukunimi: </label>
                  <input name="lastName" value="Sorsa"/>
                </div>
                <div>
                  <label for="email">Email: </label>
                  <input name="email" value="seppo.sorsa@espoo.fi"/>
                </div>
                <div>
                  <label for="roles">Roolit:</label><br>
                  <input id="evaka-espoo-officeholder" type="checkbox" name="roles" value="SERVICE_WORKER" checked /><label for="evaka-espoo-officeholder">Palveluohjaaja</label><br>
                  <input id="evaka-espoo-financeadmin" type="checkbox" name="roles" value="FINANCE_ADMIN" checked /><label for="evaka-espoo-financeadmin">Laskutus</label><br>
                  <input id="evaka-espoo-director" type="checkbox" name="roles" value="DIRECTOR" /><label for="evaka-espoo-director">Raportointi (director)</label><br>
                  <input id="evaka-espoo-admin" type="checkbox" name="roles" value="ADMIN" /><label for="evaka-espoo-admin">Pääkäyttäjä</label><br>
                </div>
                <div style="margin-top: 20px">
                  <button type="submit">Kirjaudu</button>
                </div>
            </form>
          </body>
          </html>
        `)
    },
    verifyUser: async (req) => {
      const preset = assertStringProp(req.body, 'preset')

      let loginRequest: EmployeeLoginRequest
      if (preset === 'custom') {
        const roles = Array.isArray(req.body.roles)
          ? req.body.roles
          : req.body.roles !== undefined
          ? [assertStringProp(req.body, 'roles')]
          : []
        const aad = assertStringProp(req.body, 'aad')
        const externalId = `espoo-ad:${aad}}`
        const firstName = assertStringProp(req.body, 'firstName')
        const lastName = assertStringProp(req.body, 'lastName')
        const email = assertStringProp(req.body, 'email')

        await upsertEmployee({
          externalId,
          firstName,
          lastName,
          email,
          roles: roles as UserRole[]
        })
        loginRequest = { externalId, firstName, lastName, email }
      } else {
        const externalId = `espoo-ad:${preset}`
        const employee = await getEmployeeByExternalId(externalId)
        loginRequest = {
          externalId,
          firstName: employee.firstName,
          lastName: employee.lastName,
          email: employee.email ?? ''
        }
      }
      const person = await employeeLogin(loginRequest)
      return {
        id: person.id,
        userType: 'EMPLOYEE',
        globalRoles: person.globalRoles,
        allScopedRoles: person.allScopedRoles
      }
    }
  })
}
