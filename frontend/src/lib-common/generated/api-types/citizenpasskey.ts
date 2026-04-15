// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'

/**
* Generated from fi.espoo.evaka.citizenpasskey.CitizenPasskeyController.CitizenPasskeyCredentialSummary
*/
export interface CitizenPasskeyCredentialSummary {
  createdAt: HelsinkiDateTime
  credentialId: string
  deviceHint: string | null
  label: string
  lastUsedAt: HelsinkiDateTime | null
}

/**
* Generated from fi.espoo.evaka.citizenpasskey.CitizenPasskeyController.RenamePasskeyRequest
*/
export interface RenamePasskeyRequest {
  label: string
}


export function deserializeJsonCitizenPasskeyCredentialSummary(json: JsonOf<CitizenPasskeyCredentialSummary>): CitizenPasskeyCredentialSummary {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    lastUsedAt: (json.lastUsedAt != null) ? HelsinkiDateTime.parseIso(json.lastUsedAt) : null
  }
}
