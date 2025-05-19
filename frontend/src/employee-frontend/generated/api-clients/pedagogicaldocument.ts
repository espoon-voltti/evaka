// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { PedagogicalDocument } from 'lib-common/generated/api-types/pedagogicaldocument'
import type { PedagogicalDocumentId } from 'lib-common/generated/api-types/shared'
import type { PedagogicalDocumentPostBody } from 'lib-common/generated/api-types/pedagogicaldocument'
import type { PersonId } from 'lib-common/generated/api-types/shared'
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
    url: uri`/employee/pedagogical-document`.toString(),
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
    documentId: PedagogicalDocumentId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/pedagogical-document/${request.documentId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pedagogicaldocument.PedagogicalDocumentController.getChildPedagogicalDocuments
*/
export async function getChildPedagogicalDocuments(
  request: {
    childId: PersonId
  }
): Promise<PedagogicalDocument[]> {
  const { data: json } = await client.request<JsonOf<PedagogicalDocument[]>>({
    url: uri`/employee/pedagogical-document/child/${request.childId}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonPedagogicalDocument(e))
}


/**
* Generated from fi.espoo.evaka.pedagogicaldocument.PedagogicalDocumentController.updatePedagogicalDocument
*/
export async function updatePedagogicalDocument(
  request: {
    documentId: PedagogicalDocumentId,
    body: PedagogicalDocumentPostBody
  }
): Promise<PedagogicalDocument> {
  const { data: json } = await client.request<JsonOf<PedagogicalDocument>>({
    url: uri`/employee/pedagogical-document/${request.documentId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<PedagogicalDocumentPostBody>
  })
  return deserializeJsonPedagogicalDocument(json)
}
