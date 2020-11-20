// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import csurf from 'csurf'
import express from 'express'
import { SessionType } from '../session'
import { useSecureCookies } from '../config'

// Middleware that does XSRF header checks
export const csrf = csurf({ cookie: false })

export function csrfCookieName(sessionType: SessionType): string {
  return sessionType === 'enduser' ? 'XSRF-TOKEN' : 'evaka.employee.xsrf'
}

// Returns a middleware that sets XSRF cookie that the frontend should use in
// requests. This only needs to be done in the "entry point" URL, which is called
// before any other requests for a page are done
export function csrfCookie(sessionType: SessionType): express.RequestHandler {
  return (req, res, next) => {
    const cookieName = csrfCookieName(sessionType)
    res.cookie(cookieName, req.csrfToken(), {
      httpOnly: false, // the entire point is to be readable from JS
      secure: useSecureCookies,
      sameSite: 'lax'
    })
    next()
  }
}
