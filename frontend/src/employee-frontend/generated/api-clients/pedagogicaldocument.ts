// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

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
  }
): Promise<PedagogicalDocument> {
  const { data: json } = await client.request<JsonOf<PedagogicalDocument>>({
    url: uri`/pedagogical-document`.toString(),
    method: 'POST',
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
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/pedagogical-document/${request.documentId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pedagogicaldocument.PedagogicalDocumentController.getChildPedagogicalDocuments
*/
export async function getChildPedagogicalDocuments(
  request: {
    childId: UUID
  }
): Promise<PedagogicalDocument[]> {
  const { data: json } = await client.request<JsonOf<PedagogicalDocument[]>>({
    url: uri`/pedagogical-document/child/${request.childId}`.toString(),
    method: 'GET'
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
  }
): Promise<PedagogicalDocument> {
  const { data: json } = await client.request<JsonOf<PedagogicalDocument>>({
    url: uri`/pedagogical-document/${request.documentId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<PedagogicalDocumentPostBody>
  })
  return deserializeJsonPedagogicalDocument(json)
}
