// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Profile, VerifiedCallback, VerifyWithoutRequest } from 'passport-saml'
import { SamlUser } from '../routes/auth/saml/types'

export function toSamlVerifyFunction(
  verify: (profile: Profile) => Promise<SamlUser>
): VerifyWithoutRequest {
  return (profile: Profile | null | undefined, done: VerifiedCallback) => {
    if (!profile) {
      done(new Error('No SAML profile'))
    } else {
      verify(profile)
        .then((user) => done(null, user))
        .catch(done)
    }
  }
}
