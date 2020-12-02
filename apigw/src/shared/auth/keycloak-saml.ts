import {
  Profile,
  Strategy as SamlStrategy,
  VerifiedCallback
} from 'passport-saml'
import { SamlUser } from '../routes/auth/saml/types'
import { getOrCreateEmployee } from '../service-client'

const host = 'http://localhost:9093'
const keycloackSamlCallbackUrl = `${host}/api/internal/auth/saml/login/callback`
const keycloakSamlIssuer = 'evaka'

// The magic url
const keycloakEntrypoint =
  'http://localhost:8080/auth/realms/evaka/protocol/saml'

export default function createKeycloakSamlStrategy(): SamlStrategy {
  return new SamlStrategy(
    {
      issuer: keycloakSamlIssuer,
      callbackUrl: keycloackSamlCallbackUrl,
      entryPoint: keycloakEntrypoint,
      logoutUrl: keycloakEntrypoint,
      acceptedClockSkewMs: -1
    },
    (profile: Profile, done: VerifiedCallback) => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      verifyKeycloakProfile(profile as KeycloakProfile)
        .then((user) => done(null, user))
        .catch(done)
    }
  )
}

interface KeycloakProfile {
  nameID: string
  nameIDFormat?: string
  nameQualifier?: string
  spNameQualifier?: string
  sessionIndex?: string
}

async function verifyKeycloakProfile(
  profile: KeycloakProfile
): Promise<SamlUser> {
  const person = await getOrCreateEmployee({
    aad: '00000000-0000-0000-0007-000000000000',
    firstName: profile.nameID.split('.')[0],
    lastName: profile.nameID.split('.')[1].split('@')[0],
    email: profile.nameID
  })
  return {
    id: person.id,
    roles: person.roles,
    userType: 'EMPLOYEE',
    nameID: profile.nameID,
    nameIDFormat: profile.nameIDFormat,
    nameQualifier: profile.nameQualifier,
    spNameQualifier: profile.spNameQualifier,
    sessionIndex: profile.sessionIndex
  }
}
