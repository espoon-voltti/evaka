// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { SamlConfig } from 'passport-saml'
import passportSaml from 'passport-saml'
import type { SessionType } from '../../../session'
import passport from 'passport'

export interface SamlEndpointConfig {
  strategyName: string
  strategy: passport.Strategy & {
    logout: passportSaml.Strategy['logout']
  }
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
