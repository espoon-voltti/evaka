// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Profile, SamlConfig, Strategy } from 'passport-saml'
import type { Strategy as DummyStrategy } from 'passport-dummy'
import type { SessionType } from '../../../session'
import type { UserType } from '../../../service-client'

export interface SamlEndpointConfig {
  strategyName: string
  strategy: Strategy | DummyStrategy
  samlConfig: SamlConfig
  sessionType: SessionType
  pathIdentifier: string
}

export interface PassportSamlError extends Error {
  statusXml?: string
}

export type PrimaryStatusCodeValue =
  | 'urn:oasis:names:tc:SAML:2.0:status:Success'
  | 'urn:oasis:names:tc:SAML:2.0:status:Requester'
  | 'urn:oasis:names:tc:SAML:2.0:status:Responder'
  | 'urn:oasis:names:tc:SAML:2.0:status:VersionMismatch'

export type SecondaryStatusCodeValue =
  | 'urn:oasis:names:tc:SAML:2.0:status:AuthnFailed'
  | 'urn:oasis:names:tc:SAML:2.0:status:RequestDenied'

export interface StatusObject {
  Status: {
    StatusCode: {
      '@_Value': PrimaryStatusCodeValue
      StatusCode?: {
        '@_Value': SecondaryStatusCodeValue
      }
    }
    StatusMessage: string
  }
}

export interface SamlUser extends Profile {
  // eVaka id
  id?: string | undefined
  userType?: UserType | undefined
  // all are optional because of legacy sessions
  roles?: string[] | undefined
  globalRoles?: string[] | undefined
  allScopedRoles?: string[] | undefined
  mobileEmployeeId?: string | undefined
}
