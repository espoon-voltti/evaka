// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Request } from 'express'
import { parse } from 'fast-xml-parser'
import { logDebug, logError } from '../../../logging'
import {
  PassportSamlError,
  PrimaryStatusCodeValue,
  SecondaryStatusCodeValue,
  StatusObject
} from './types'

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

  const statusObject: StatusObject = parse(error.statusXml, {
    ignoreAttributes: false,
    parseAttributeValue: true
  })

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
