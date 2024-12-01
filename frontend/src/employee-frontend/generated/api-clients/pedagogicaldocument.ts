// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AxiosHeaders } from 'axios'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PedagogicalDocument } from 'lib-common/generated/api-types/pedagogicaldocument'
import { PedagogicalDocumentPostBody } from 'lib-common/generated/api-types/pedagogicaldocument'
import { UUID } from 'lib-common/types'
import { client } from '../../api/client'
import { deserializeJsonPedagogicalDocument } from 'lib-common/generated/api-types/pedagogicaldocument'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.pedagogicaldocument.PedagogicalDocumentController.createPedagogicalDocument
*/
export async function createPedagogicalDocument(
  request: {
    body: PedagogicalDocumentPostBody
  },
  headers?: AxiosHeaders
): Promise<PedagogicalDocument> {
  const { data: json } = await client.request<JsonOf<PedagogicalDocument>>({
    url: uri`/employee/pedagogical-document`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<PedagogicalDocumentPostBody>
  })
  return deserializeJsonPedagogicalDocument(json)
}


/**
* Generated from fi.espoo.evaka.pedagogicaldocument.PedagogicalDocumentController.deletePedagogicalDocument
*/
export async function deletePedagogicalDocument(
  request: {
    documentId: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/pedagogical-document/${request.documentId}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pedagogicaldocument.PedagogicalDocumentController.getChildPedagogicalDocuments
*/
export async function getChildPedagogicalDocuments(
  request: {
    childId: UUID
  },
  headers?: AxiosHeaders
): Promise<PedagogicalDocument[]> {
  const { data: json } = await client.request<JsonOf<PedagogicalDocument[]>>({
    url: uri`/employee/pedagogical-document/child/${request.childId}`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonPedagogicalDocument(e))
}


/**
* Generated from fi.espoo.evaka.pedagogicaldocument.PedagogicalDocumentController.updatePedagogicalDocument
*/
export async function updatePedagogicalDocument(
  request: {
    documentId: UUID,
    body: PedagogicalDocumentPostBody
  },
  headers?: AxiosHeaders
): Promise<PedagogicalDocument> {
  const { data: json } = await client.request<JsonOf<PedagogicalDocument>>({
    url: uri`/employee/pedagogical-document/${request.documentId}`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<PedagogicalDocumentPostBody>
  })
  return deserializeJsonPedagogicalDocument(json)
}
