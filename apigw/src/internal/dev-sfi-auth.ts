// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Router } from 'express'
import _ from 'lodash'

import { createDevAuthRouter } from '../shared/auth/dev-auth.js'
import { getVtjPersons } from '../shared/dev-api.js'
import { assertStringProp } from '../shared/express.js'
import { employeeSuomiFiLogin } from '../shared/service-client.js'
import { Sessions } from '../shared/session.js'

export function createDevEmployeeSfiRouter(
  sessions: Sessions<'employee'>
): Router {
  return createDevAuthRouter({
    sessions,
    root: '/employee',
    loginFormHandler: async (req, res) => {
      const defaultSsn = '060195-966B'

      const persons = _.orderBy(
        await getVtjPersons(),
        [({ ssn }) => defaultSsn === ssn, ({ ssn }) => ssn],
        ['desc', 'asc']
      )
      const inputs = persons
        .map(({ ssn, firstName, lastName }) => {
          if (!ssn) return ''
          const checked = ssn === defaultSsn ? 'checked' : ''
          return `<div><input type="radio" id="${ssn}" name="preset" value="${ssn}" required ${checked}/><label for="${ssn}"><span style="font-family: monospace; user-select: all">${ssn}</span>: ${firstName} ${lastName}</label></div>`
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
            <h1>Devausympäristön Suomi.fi-kirjautuminen</h1>
            <form action="${formUri}" method="post">
              <div style="margin-bottom: 20px">
                <button type="submit">Kirjaudu</button>
              </div>
              ${inputs.join('\n')}
            </form>
          </body>
          </html>
        `)
    },
    verifyUser: async (req) => {
      const ssn = assertStringProp(req.body, 'preset')
      const persons = await getVtjPersons()
      const person = persons.find((c) => c.ssn === ssn)
      if (!person) throw new Error(`No VTJ person found with SSN ${ssn}`)
      const employee = await employeeSuomiFiLogin({
        ssn,
        firstName: person.firstName,
        lastName: person.lastName
      })
      return {
        id: employee.id,
        authType: 'dev',
        userType: 'EMPLOYEE',
        globalRoles: employee.globalRoles,
        allScopedRoles: employee.allScopedRoles
      }
    }
  })
}
