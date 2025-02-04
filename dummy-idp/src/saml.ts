// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import samlp, { ProfileMapper } from 'samlp'
import { SessionData } from 'express-session'
import { toSfiSamlAttrs } from './model'

// This is 100% samlp internal stuff but @types/samlp doesn't export this type
interface SessionParticipant {
  serviceProviderId: string
  nameId: string
  nameIdFormat: string
  sessionIndex: string
  serviceProviderLogoutURL: string
  cert: Buffer
  binding?: string
}

// samlp requires an implementation of this, but @types/samlp doesn't export any types
export class SessionParticipants {
  constructor(private sessions: SessionParticipant[]) {}
  get(
    issuer: string,
    sessionIndices: string[],
    nameId: string,
    cb: (err: any, sp: SessionParticipant | undefined) => void
  ) {
    cb(
      null,
      this.sessions.find(
        (sp) =>
          sp.serviceProviderId === issuer &&
          sp.nameId === nameId &&
          sessionIndices.some((idx) => sp.sessionIndex === idx)
      )
    )
  }
  hasElements(): boolean {
    return !!this.sessions.length
  }
  getFirst(cb: (err: any, sp: SessionParticipant | undefined) => void) {
    cb(null, this.sessions[0])
  }
  remove(
    serviceProviderId: string,
    sessionIndex: string,
    nameId: string,
    cb: (err: any, removed: SessionParticipant | undefined) => void
  ) {
    const index = this.sessions.findIndex(
      (sp) =>
        sp.serviceProviderId === serviceProviderId &&
        sp.nameId === nameId &&
        sp.sessionIndex === sessionIndex
    )
    cb(null, this.sessions.splice(index, 1)[0])
  }
}

export class SimpleProfileMapper implements ProfileMapper {
  constructor(private profile: NonNullable<SessionData['user']>) {}

  metadata: samlp.MetadataItem[] = [] // we don't use the metadata functionality of samlp
  getClaims() {
    return toSfiSamlAttrs(this.profile.person)
  }
  getNameIdentifier() {
    return {
      nameIdentifier: this.profile.nameId,
      nameIdentifierFormat:
        'urn:oasis:names:tc:SAML:2.0:nameid-format:transient'
    }
  }
}
