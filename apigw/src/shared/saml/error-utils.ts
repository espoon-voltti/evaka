// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Request } from 'express'
import { XMLParser } from 'fast-xml-parser'

import { logDebug, logError } from '../logging.js'

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

function trimStatusCodePrefix(
  statusCode: PrimaryStatusCodeValue | SecondaryStatusCodeValue
): string {
  return statusCode.replace('urn:oasis:names:tc:SAML:2.0:status:', '')
}

function parsePrimaryStatus(status: StatusObject): string {
  const primaryCode = status.Status.StatusCode['@_Value']
  return trimStatusCodePrefix(primaryCode)
}

function parseSecondaryStatus(status: StatusObject): string {
  const secondaryCode =
    status.Status.StatusCode.StatusCode &&
    status.Status.StatusCode.StatusCode['@_Value']
  if (!secondaryCode) {
    return 'No secondary status code.'
  }
  return trimStatusCodePrefix(secondaryCode)
}

function parseErrorMessage(status: StatusObject): string {
  return status.Status.StatusMessage
}

export function parseDescriptionFromSamlError(
  error: PassportSamlError,
  req: Request
): string | undefined {
  if (!error.statusXml) {
    logDebug('No statusXml found from SAML error', req, { error })
    return
  }

  const parser = new XMLParser({
    ignoreAttributes: false,
    parseAttributeValue: true
  })
  // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
  const statusObject: StatusObject = parser.parse(error.statusXml)

  if (!statusObject) {
    logError(
      'Failed to parse status object from XML',
      req,
      undefined,
      new Error('Failed to parse status object from XML')
    )
    return
  }

  return (
    `Primary status code: ${parsePrimaryStatus(statusObject)}, ` +
    `secondary status code: ${parseSecondaryStatus(statusObject)}, ` +
    `error message: ${parseErrorMessage(statusObject)}`
  )
}
