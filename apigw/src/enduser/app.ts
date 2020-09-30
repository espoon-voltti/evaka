// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'
import bodyParser from 'body-parser'
import cookieParser from 'cookie-parser'
import passport from 'passport'
import publicRoutes from './publicRoutes'
import routes from './routes'
import userDetails from './routes/auth-status'
import { createAuthEndpoints } from '../shared/routes/auth/suomi-fi'
import { errorHandler } from '../shared/middleware/error-handler'
import { authenticate } from '../shared/auth'
import session, { refreshLogoutToken } from '../shared/session'
import setupLoggingMiddleware from '../shared/logging'
import { csrf } from '../shared/middleware/csrf'
import { trustReverseProxy } from '../shared/reverse-proxy'
import nocache from 'nocache'
import helmet from 'helmet'
import tracing from '../shared/middleware/tracing'

const app = express()
trustReverseProxy(app)
app.set('etag', false)
app.use(nocache())
app.use(helmet())
app.get('/health', (req, res) => res.status(200).json({ status: 'UP' }))
app.use(tracing)
app.use(bodyParser.json())
app.use(cookieParser())
app.use(session('enduser'))
app.use(passport.initialize())
app.use(passport.session())
passport.serializeUser((user, done) => done(null, user))
passport.deserializeUser((user, done) => done(null, user))
app.use(refreshLogoutToken('enduser'))
setupLoggingMiddleware(app)

app.use('/api/application/', publicRoutes)
app.use('/api/application/', createAuthEndpoints('enduser'))
app.use(csrf)
app.use('/api/application/', userDetails('enduser'))
app.all('*', authenticate)
app.use('/api/application/', routes)
app.use(errorHandler(false))

export default app
