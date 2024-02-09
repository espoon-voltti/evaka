// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import { CitizenGetVasuDocumentResponse } from 'lib-common/generated/api-types/vasu'
import { CitizenGetVasuDocumentSummariesResponse } from 'lib-common/generated/api-types/vasu'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../api-client'
import { deserializeJsonCitizenGetVasuDocumentResponse } from 'lib-common/generated/api-types/vasu'
import { deserializeJsonCitizenGetVasuDocumentSummariesResponse } from 'lib-common/generated/api-types/vasu'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.vasu.VasuControllerCitizen.getChildVasuSummaries
*/
export async function getChildVasuSummaries(
  request: {
    childId: UUID
  }
): Promise<CitizenGetVasuDocumentSummariesResponse> {
  const { data: json } = await client.request<JsonOf<CitizenGetVasuDocumentSummariesResponse>>({
    url: uri`/citizen/vasu/children/${request.childId}/vasu-summaries`.toString(),
    method: 'GET'
  })
  return deserializeJsonCitizenGetVasuDocumentSummariesResponse(json)
}


/**
* Generated from fi.espoo.evaka.vasu.VasuControllerCitizen.getDocument
*/
export async function getDocument(
  request: {
    id: UUID
  }
): Promise<CitizenGetVasuDocumentResponse> {
  const { data: json } = await client.request<JsonOf<CitizenGetVasuDocumentResponse>>({
    url: uri`/citizen/vasu/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonCitizenGetVasuDocumentResponse(json)
}


/**
* Generated from fi.espoo.evaka.vasu.VasuControllerCitizen.getGuardianUnreadVasuCount
*/
export async function getGuardianUnreadVasuCount(): Promise<Record<UUID, number>> {
  const { data: json } = await client.request<JsonOf<Record<UUID, number>>>({
    url: uri`/citizen/vasu/children/unread-count`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.vasu.VasuControllerCitizen.givePermissionToShare
*/
export async function givePermissionToShare(
  request: {
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/vasu/${request.id}/give-permission-to-share`.toString(),
    method: 'POST'
  })
  return json
}
