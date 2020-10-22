// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Strategy } from 'passport'
import { Request } from 'express'
import { SamlUser } from '../routes/auth/saml/types'

type ProfileGetter = (userId: string) => Promise<SamlUser>

type ProfileUpserter = (
  userId: string,
  roles: string[],
  firstName: string,
  lastName: string,
  email: string
) => Promise<SamlUser>

export default class DevPassportStrategy extends Strategy {
  private profileGetter: ProfileGetter
  private profileUpserter: ProfileUpserter
  constructor(profileGetter: ProfileGetter, profileUpserter: ProfileUpserter) {
    super()
    this.profileGetter = profileGetter
    this.profileUpserter = profileUpserter
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  authenticate(req: Request, _options?: any): any {
    const shouldRedirect = !req.url.startsWith('/auth/saml/login/callback')

    if (shouldRedirect) {
      return this.redirect(
        `${req.baseUrl}/dev-auth/login?RelayState=${req.query.RelayState}`
      )
    }

    if (req.body.preset === 'custom') {
      const roles = Array.isArray(req.body.roles)
        ? req.body.roles
        : req.body.roles !== undefined
        ? [req.body.roles]
        : []

      this.profileUpserter(
        req.body.aad,
        roles,
        req.body.firstName,
        req.body.lastName,
        req.body.email
      )
        .then((samlUser) => this.success(samlUser))
        .catch(() => this.error('Something went wrong'))
    } else {
      this.profileGetter(req.body.preset)
        .then((samlUser) => this.success(samlUser))
        .catch(() => this.error('Something went wrong'))
    }
  }
}
