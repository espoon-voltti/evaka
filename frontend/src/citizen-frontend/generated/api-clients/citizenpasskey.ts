// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { CitizenPasskeyCredentialSummary } from 'lib-common/generated/api-types/citizenpasskey'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { RenamePasskeyRequest } from 'lib-common/generated/api-types/citizenpasskey'
import { client } from '../../api-client'
import { deserializeJsonCitizenPasskeyCredentialSummary } from 'lib-common/generated/api-types/citizenpasskey'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.citizenpasskey.CitizenPasskeyController.listCredentials
*/
export async function listCredentials(): Promise<CitizenPasskeyCredentialSummary[]> {
  const { data: json } = await client.request<JsonOf<CitizenPasskeyCredentialSummary[]>>({
    url: uri`/citizen/passkey/credentials`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonCitizenPasskeyCredentialSummary(e))
}


/**
* Generated from fi.espoo.evaka.citizenpasskey.CitizenPasskeyController.renameCredential
*/
export async function renameCredential(
  request: {
    credentialId: string,
    body: RenamePasskeyRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/passkey/credentials/${request.credentialId}`.toString(),
    method: 'PATCH',
    data: request.body satisfies JsonCompatible<RenamePasskeyRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.citizenpasskey.CitizenPasskeyController.revokeCredential
*/
export async function revokeCredential(
  request: {
    credentialId: string
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/passkey/credentials/${request.credentialId}`.toString(),
    method: 'DELETE'
  })
  return json
}
