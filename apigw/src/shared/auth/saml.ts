// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { z } from 'zod'
import { Profile, VerifiedCallback, VerifyWithoutRequest } from 'passport-saml'
import { SamlUser } from '../routes/auth/saml/types'
import { logWarn } from '../logging'

export function toSamlVerifyFunction(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  schema: z.ZodObject<any>,
  verify: (profile: Profile) => Promise<SamlUser>
): VerifyWithoutRequest {
  return (profile: Profile | null | undefined, done: VerifiedCallback) => {
    if (!profile) {
      done(new Error('No SAML profile'))
    } else {
      const parseResult = schema.safeParse(profile)
      if (!parseResult.success) {
        logWarn(
          `SAML profile parsing failed: ${parseResult.error.message}`,
          undefined,
          {
            issuer: profile.issuer
          },
          parseResult.error
        )
      }
      verify(profile)
        .then((user) => done(null, user))
        .catch(done)
    }
  }
}
