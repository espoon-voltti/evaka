// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { z } from 'zod'
import { Profile, VerifiedCallback, VerifyWithoutRequest } from 'passport-saml'
import { logWarn } from '../logging'
import { EvakaSessionUser } from './index'

export function toSamlVerifyFunction(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  schema: z.ZodObject<any>,
  verify: (profile: Profile) => Promise<EvakaSessionUser>
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
        .then((user) => {
          // Despite what the typings say, passport-saml assumes
          // we give it back a valid Profile, including at least some of these
          // SAML-specific fields
          const samlUser: EvakaSessionUser & Profile = {
            ...user,
            issuer: profile.issuer,
            nameID: profile.nameID,
            nameIDFormat: profile.nameIDFormat,
            nameQualifier: profile.nameQualifier,
            spNameQualifier: profile.spNameQualifier,
            sessionIndex: profile.sessionIndex
          }
          done(null, samlUser)
        })
        .catch(done)
    }
  }
}
