// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// oxlint-disable no-console

import express from 'express'
import session from 'express-session'

import { config } from './config'
import {
  clearUsers,
  enterTestMode,
  exitTestMode,
  getVtjPerson,
  samlSingleLogoutRoute,
  samlSingleSignOnConfirmRoute,
  samlSingleSignOnFinishRoute,
  samlSingleSignOnRoute,
  upsertUser
} from './routes'

const app = express()
app.use(
  session({
    cookie: {
      maxAge: 32 * 60 * 1000
    },
    secret: 'localtestonly',
    resave: false,
    rolling: false,
    saveUninitialized: false
  })
)

app.get('/health', (_, res) => {
  res.sendStatus(200)
})

app.post('/idp/users/clear', clearUsers)
app.post('/idp/users', express.json(), upsertUser)
app.get('/idp/users/:ssn', getVtjPerson)
app.post('/idp/test-mode/enter', enterTestMode)
app.post('/idp/test-mode/exit', exitTestMode)

app.get(
  '/idp/sso',
  (req, res, next) => {
    if (req.query.action === 'destroy') req.session.regenerate(next)
    else next()
  },
  samlSingleSignOnRoute
)
app.get('/idp/sso-login-confirm', samlSingleSignOnConfirmRoute)
app.get('/idp/sso-login-finish', samlSingleSignOnFinishRoute)
app.get('/idp/slo', samlSingleLogoutRoute)

app.listen(config.HTTP_PORT, () => {
  console.log(`dummy-idp listening on port ${config.HTTP_PORT}`)
})
