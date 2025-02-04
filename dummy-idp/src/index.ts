// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'
import session from 'express-session'
import {
  clearUsers,
  samlSingleLogoutRoute,
  samlSingleSignOnConfirmRoute,
  samlSingleSignOnFinishRoute,
  samlSingleSignOnRoute,
  upsertUser
} from './routes'
import { config } from './config'

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

app.get('/idp/sso', samlSingleSignOnRoute)
app.get('/idp/sso-login-confirm', samlSingleSignOnConfirmRoute)
app.get('/idp/sso-login-finish', samlSingleSignOnFinishRoute)
app.get('/idp/slo', samlSingleLogoutRoute)

app.listen(config.HTTP_PORT, () => {
  console.log(`dummy-idp listening on port ${config.HTTP_PORT}`)
})
