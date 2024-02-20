// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../client'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.varda.VardaController.markChildForVardaReset
*/
export async function markChildForVardaReset(
  request: {
    childId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/varda/child/reset/${request.childId}`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.varda.VardaController.runFullVardaReset
*/
export async function runFullVardaReset(): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/varda/start-reset`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.varda.VardaController.runFullVardaUpdate
*/
export async function runFullVardaUpdate(): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/varda/start-update`.toString(),
    method: 'POST'
  })
  return json
}
