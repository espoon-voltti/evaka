// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Strategy } from 'passport'
import { Request } from 'express'
import { SamlUser } from '../routes/auth/saml/types'
import { assertStringProp } from '../express'

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

    const preset = assertStringProp(req.body, 'preset')

    if (preset === 'custom') {
      const roles = Array.isArray(req.body.roles)
        ? req.body.roles
        : req.body.roles !== undefined
        ? [assertStringProp(req.body, 'roles')]
        : []

      this.profileUpserter(
        assertStringProp(req.body, 'aad'),
        roles,
        assertStringProp(req.body, 'firstName'),
        assertStringProp(req.body, 'lastName'),
        assertStringProp(req.body, 'email')
      )
        .then((samlUser) => this.success(samlUser))
        .catch((err) => this.error(err))
    } else {
      this.profileGetter(preset)
        .then((samlUser) => this.success(samlUser))
        .catch((err) => this.error(err))
    }
  }
}
