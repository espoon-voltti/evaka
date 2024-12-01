// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AxiosHeaders } from 'axios'
import { JsonOf } from 'lib-common/json'
import { PedagogicalDocumentCitizen } from 'lib-common/generated/api-types/pedagogicaldocument'
import { UUID } from 'lib-common/types'
import { client } from '../../api-client'
import { deserializeJsonPedagogicalDocumentCitizen } from 'lib-common/generated/api-types/pedagogicaldocument'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.pedagogicaldocument.PedagogicalDocumentControllerCitizen.getPedagogicalDocumentsForChild
*/
export async function getPedagogicalDocumentsForChild(
  request: {
    childId: UUID
  },
  headers?: AxiosHeaders
): Promise<PedagogicalDocumentCitizen[]> {
  const { data: json } = await client.request<JsonOf<PedagogicalDocumentCitizen[]>>({
    url: uri`/citizen/children/${request.childId}/pedagogical-documents`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonPedagogicalDocumentCitizen(e))
}


/**
* Generated from fi.espoo.evaka.pedagogicaldocument.PedagogicalDocumentControllerCitizen.getUnreadPedagogicalDocumentCount
*/
export async function getUnreadPedagogicalDocumentCount(
  headers?: AxiosHeaders
): Promise<Record<UUID, number>> {
  const { data: json } = await client.request<JsonOf<Record<UUID, number>>>({
    url: uri`/citizen/pedagogical-documents/unread-count`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pedagogicaldocument.PedagogicalDocumentControllerCitizen.markPedagogicalDocumentRead
*/
export async function markPedagogicalDocumentRead(
  request: {
    documentId: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/pedagogical-documents/${request.documentId}/mark-read`.toString(),
    method: 'POST',
    headers
  })
  return json
}
