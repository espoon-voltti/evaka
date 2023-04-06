// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Strategy } from 'passport'
import { Request } from 'express'
import { assertStringProp } from '../express'
import { EvakaUserFields } from '../routes/auth/saml/types'

type ProfileGetter = (userId: string) => Promise<EvakaUserFields>

export default class DevSfiStrategy extends Strategy {
  constructor(private profileGetter: ProfileGetter) {
    super()
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  authenticate(req: Request, _options?: any): any {
    const shouldRedirect = !req.url.startsWith('/auth/saml/login/callback')

    if (shouldRedirect) {
      return this.redirect(
        `${req.baseUrl}/dev-sfi-auth/login?RelayState=${req.query.RelayState}`
      )
    }

    const preset = assertStringProp(req.body, 'preset')

    this.profileGetter(preset)
      .then((samlUser) => this.success(samlUser))
      .catch((err) => this.error(err))
  }

  logout(req: Request, cb: (err: Error | null, url?: string | null) => void) {
    cb(null, null)
  }
}
