// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Profile, Strategy, VerifiedCallback } from 'passport-saml'
import { SamlUser } from '../routes/auth/saml/types'
import { Strategy as DummyStrategy } from 'passport-dummy'
import { sfiConfig, sfiMock } from '../config'
import certificates from '../certificates'
import fs from 'fs'
import { getOrCreatePerson } from '../service-client'

// Suomi.fi e-Identification – Attributes transmitted on an identified user:
//   https://esuomi.fi/suomi-fi-services/suomi-fi-e-identification/14247-2/?lang=en
// Note: Suomi.fi only returns the values we request in our SAML metadata
const SUOMI_FI_SSN_KEY = 'urn:oid:1.2.246.21'
const SUOMI_FI_GIVEN_NAME_KEY = 'urn:oid:2.5.4.42'
const SUOMI_FI_SURNAME_KEY = 'urn:oid:2.5.4.4'

interface SuomiFiProfile {
  nameID?: Profile['nameID']
  nameIDFormat?: Profile['nameIDFormat']
  nameQualifier?: Profile['nameQualifier']
  spNameQualifier?: Profile['spNameQualifier']
  sessionIndex?: Profile['sessionIndex']
  [SUOMI_FI_SSN_KEY]: string
  [SUOMI_FI_SURNAME_KEY]: string
  [SUOMI_FI_GIVEN_NAME_KEY]: string
}

const dummySuomiFiProfile: SuomiFiProfile = {
  [SUOMI_FI_SSN_KEY]: '070644-937X',
  [SUOMI_FI_GIVEN_NAME_KEY]: 'Seppo',
  [SUOMI_FI_SURNAME_KEY]: 'Sorsa'
}

async function verifyProfile(profile: SuomiFiProfile): Promise<SamlUser> {
  const person = await getOrCreatePerson({
    socialSecurityNumber: profile[SUOMI_FI_SSN_KEY],
    firstName: profile[SUOMI_FI_GIVEN_NAME_KEY],
    lastName: profile[SUOMI_FI_SURNAME_KEY]
  })
  return {
    id: person.id,
    roles: person.roles,
    userType: 'ENDUSER',
    nameID: profile.nameID,
    nameIDFormat: profile.nameIDFormat,
    nameQualifier: profile.nameQualifier,
    spNameQualifier: profile.spNameQualifier,
    sessionIndex: profile.sessionIndex
  }
}
export default function createSuomiFiStrategy(): Strategy | DummyStrategy {
  if (sfiMock) {
    return new DummyStrategy((done) => {
      verifyProfile(dummySuomiFiProfile)
        .then((user) => done(null, user))
        .catch(done)
    })
  } else {
    if (!sfiConfig) throw new Error('Missing Suomi.fi SAML configuration')
    const privateCert = fs.readFileSync(sfiConfig.privateCert, {
      encoding: 'utf8'
    })
    return new Strategy(
      {
        callbackUrl: sfiConfig.callbackUrl,
        entryPoint: sfiConfig.entryPoint,
        logoutUrl: sfiConfig.logoutUrl,
        issuer: sfiConfig.issuer,
        cert: sfiConfig.publicCert.map(
          (certificateName) => certificates[certificateName]
        ),
        privateCert: privateCert,
        decryptionPvk: privateCert,
        identifierFormat: 'urn:oasis:names:tc:SAML:2.0:nameid-format:transient',
        disableRequestedAuthnContext: true,
        signatureAlgorithm: 'sha256',
        acceptedClockSkewMs: -1
      },
      (profile: Profile, done: VerifiedCallback) => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        verifyProfile((profile as any) as SuomiFiProfile)
          .then((user) => done(null, user))
          .catch(done)
      }
    )
  }
}
