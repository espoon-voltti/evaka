// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { randomUUID as uuidv4 } from 'node:crypto'

import type { Router } from 'express'
import _ from 'lodash'
import { z } from 'zod'

import { createDevAuthRouter } from '../shared/auth/dev-auth.ts'
import { getEmployees } from '../shared/dev-api.ts'
import { assertStringProp } from '../shared/express.ts'
import { employeeLogin } from '../shared/service-client.ts'
import type { Sessions } from '../shared/session.ts'

const Employee = z.object({
  externalId: z.string(),
  firstName: z.string(),
  lastName: z.string(),
  email: z.string()
})

export function createDevAdRouter(sessions: Sessions<'employee'>): Router {
  return createDevAuthRouter({
    sessions,
    root: '/employee',
    loginFormHandler: async (req, res) => {
      const employees = _.sortBy(await getEmployees(), ({ id }) => id)
      const employeeInputs = employees
        .filter((employee) => employee.externalId && employee.email)
        .map((employee, idx) => {
          const { externalId, firstName, lastName } = employee
          const json = JSON.stringify(employee)
          return `<div>
            <input
              type="radio"
              id="${externalId}"
              name="preset"
              ${idx == 0 ? 'checked' : ''}
              value="${_.escape(json)}" />
            <label for="${externalId}">${firstName} ${lastName}</label>
          </div>`
        })

      const formQuery =
        typeof req.query.RelayState === 'string'
          ? `?RelayState=${encodeURIComponent(req.query.RelayState)}`
          : ''
      const formUri = `${req.baseUrl}/login/callback${formQuery}`

      const now = new Date()
      const time = `${now.getHours()}${now.getMinutes()}${now.getSeconds()}`
      const uuid = uuidv4()

      res.contentType('text/html').send(`
          <html>
          <body>
            <h1>Devausympäristön AD-kirjautuminen</h1>
            <form action="${formUri}" method="post">
                ${employeeInputs.join('\n')}
                <div style="margin-top: 20px">
                  <input type="radio" id="new" name="preset" value="new" /><label for="new">Luo uusi käyttäjä</label>
                </div>
                <h2>Uusi käyttäjä</h2>
                <input id="aad-input" name="externalId" value="espoo-ad:${uuid}" type="hidden" />
                <div>
                  <label for="firstName">Etunimi: </label>
                  <input name="firstName" value="Seppo ${time}"/>
                </div>
                <div>
                  <label for="lastName">Sukunimi: </label>
                  <input name="lastName" value="Sorsa"/>
                </div>
                <div>
                  <label for="email">Email: </label>
                  <input name="email" value="seppo${time}@example.com"/>
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
      const person = await employeeLogin(
        req,
        Employee.parse(preset === 'new' ? req.body : JSON.parse(preset))
      )
      return {
        id: person.id,
        authType: 'dev',
        userType: 'EMPLOYEE',
        globalRoles: person.globalRoles,
        allScopedRoles: person.allScopedRoles
      }
    }
  })
}
