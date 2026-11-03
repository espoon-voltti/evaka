// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { CitizenPasskey } from 'lib-common/generated/api-types/user'
import type { CitizenPasskeyId } from 'lib-common/generated/api-types/shared'
import type { FinishPasskeyRegistrationRequest } from 'lib-common/generated/api-types/user'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { PasskeyRegistrationOptions } from 'lib-common/generated/api-types/user'
import type { UpdatePasskeyNameRequest } from 'lib-common/generated/api-types/user'
import { client } from '../../api-client'
import { deserializeJsonCitizenPasskey } from 'lib-common/generated/api-types/user'
import { uri } from 'lib-common/uri'


/**
* Generated from evaka.core.user.PasskeyControllerCitizen.deletePasskey
*/
export async function deletePasskey(
  request: {
    id: CitizenPasskeyId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/passkeys/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from evaka.core.user.PasskeyControllerCitizen.finishPasskeyRegistration
*/
export async function finishPasskeyRegistration(
  request: {
    body: FinishPasskeyRegistrationRequest
  }
): Promise<CitizenPasskey> {
  const { data: json } = await client.request<JsonOf<CitizenPasskey>>({
    url: uri`/citizen/passkeys/register/finish`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<FinishPasskeyRegistrationRequest>
  })
  return deserializeJsonCitizenPasskey(json)
}


/**
* Generated from evaka.core.user.PasskeyControllerCitizen.getPasskeys
*/
export async function getPasskeys(): Promise<CitizenPasskey[]> {
  const { data: json } = await client.request<JsonOf<CitizenPasskey[]>>({
    url: uri`/citizen/passkeys`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonCitizenPasskey(e))
}


/**
* Generated from evaka.core.user.PasskeyControllerCitizen.startPasskeyRegistration
*/
export async function startPasskeyRegistration(): Promise<PasskeyRegistrationOptions> {
  const { data: json } = await client.request<JsonOf<PasskeyRegistrationOptions>>({
    url: uri`/citizen/passkeys/register`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from evaka.core.user.PasskeyControllerCitizen.updatePasskeyName
*/
export async function updatePasskeyName(
  request: {
    id: CitizenPasskeyId,
    body: UpdatePasskeyNameRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/passkeys/${request.id}/name`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<UpdatePasskeyNameRequest>
  })
  return json
}
