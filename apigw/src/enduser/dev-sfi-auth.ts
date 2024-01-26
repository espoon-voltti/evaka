// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Router } from 'express'
import { assertStringProp } from '../shared/express.js'
import _ from 'lodash'
import { getCitizens } from '../shared/dev-api.js'
import { createDevAuthRouter } from '../shared/auth/dev-auth.js'
import { citizenLogin } from '../shared/service-client.js'
import { Sessions } from '../shared/session.js'

export function createDevSfiRouter(sessions: Sessions): Router {
  return createDevAuthRouter({
    sessions,
    root: '/',
    strategyName: 'dev-sfi',
    loginFormHandler: async (req, res) => {
      const defaultSsn = '070644-937X'

      const citizens = _.orderBy(
        await getCitizens(),
        [
          ({ ssn }) => defaultSsn === ssn,
          ({ dependantCount }) => dependantCount,
          ({ ssn }) => ssn
        ],
        ['desc', 'desc', 'asc']
      )
      const citizenInputs = citizens
        .map(({ ssn, firstName, lastName, dependantCount }) => {
          if (!ssn) return ''
          const checked = ssn === defaultSsn ? 'checked' : ''
          return `<div><input type="radio" id="${ssn}" name="preset" value="${ssn}" ${checked}/><label for="${ssn}">${firstName} ${lastName} (${dependantCount} huollettavaa)</label></div>`
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
              ${citizenInputs.join('\n')}
            </form>
          </body>
          </html>
        `)
    },
    verifyUser: async (req) => {
      const socialSecurityNumber = assertStringProp(req.body, 'preset')
      const person = await citizenLogin({
        socialSecurityNumber,
        firstName: '',
        lastName: ''
      })
      return {
        id: person.id,
        userType: 'CITIZEN_STRONG',
        globalRoles: [],
        allScopedRoles: []
      }
    }
  })
}
