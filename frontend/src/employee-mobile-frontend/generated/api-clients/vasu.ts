// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { CopyTemplateRequest } from 'lib-common/generated/api-types/vasu'
import { CreateDocumentRequest } from 'lib-common/generated/api-types/vasu'
import { CreateTemplateRequest } from 'lib-common/generated/api-types/vasu'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { UpdateDocumentRequest } from 'lib-common/generated/api-types/vasu'
import { VasuContent } from 'lib-common/generated/api-types/vasu'
import { VasuDocumentSummaryWithPermittedActions } from 'lib-common/generated/api-types/vasu'
import { VasuDocumentWithPermittedActions } from 'lib-common/generated/api-types/vasu'
import { VasuTemplate } from 'lib-common/generated/api-types/vasu'
import { VasuTemplateSummary } from 'lib-common/generated/api-types/vasu'
import { VasuTemplateUpdate } from 'lib-common/generated/api-types/vasu'
import { client } from '../../client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonVasuDocumentSummaryWithPermittedActions } from 'lib-common/generated/api-types/vasu'
import { deserializeJsonVasuDocumentWithPermittedActions } from 'lib-common/generated/api-types/vasu'
import { deserializeJsonVasuTemplate } from 'lib-common/generated/api-types/vasu'
import { deserializeJsonVasuTemplateSummary } from 'lib-common/generated/api-types/vasu'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.vasu.VasuController.createDocument
*/
export async function createDocument(
  request: {
    childId: UUID,
    body: CreateDocumentRequest
  }
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/children/${request.childId}/vasu`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CreateDocumentRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.vasu.VasuController.getDocument
*/
export async function getDocument(
  request: {
    id: UUID
  }
): Promise<VasuDocumentWithPermittedActions> {
  const { data: json } = await client.request<JsonOf<VasuDocumentWithPermittedActions>>({
    url: uri`/vasu/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonVasuDocumentWithPermittedActions(json)
}


/**
* Generated from fi.espoo.evaka.vasu.VasuController.getVasuSummariesByChild
*/
export async function getVasuSummariesByChild(
  request: {
    childId: UUID
  }
): Promise<VasuDocumentSummaryWithPermittedActions[]> {
  const { data: json } = await client.request<JsonOf<VasuDocumentSummaryWithPermittedActions[]>>({
    url: uri`/children/${request.childId}/vasu-summaries`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonVasuDocumentSummaryWithPermittedActions(e))
}


/**
* Generated from fi.espoo.evaka.vasu.VasuController.putDocument
*/
export async function putDocument(
  request: {
    id: UUID,
    body: UpdateDocumentRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/vasu/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<UpdateDocumentRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.vasu.VasuTemplateController.copyTemplate
*/
export async function copyTemplate(
  request: {
    id: UUID,
    body: CopyTemplateRequest
  }
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/vasu/templates/${request.id}/copy`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CopyTemplateRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.vasu.VasuTemplateController.deleteTemplate
*/
export async function deleteTemplate(
  request: {
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/vasu/templates/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.vasu.VasuTemplateController.editTemplate
*/
export async function editTemplate(
  request: {
    id: UUID,
    body: VasuTemplateUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/vasu/templates/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<VasuTemplateUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.vasu.VasuTemplateController.getTemplate
*/
export async function getTemplate(
  request: {
    id: UUID
  }
): Promise<VasuTemplate> {
  const { data: json } = await client.request<JsonOf<VasuTemplate>>({
    url: uri`/vasu/templates/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonVasuTemplate(json)
}


/**
* Generated from fi.espoo.evaka.vasu.VasuTemplateController.getTemplates
*/
export async function getTemplates(
  request: {
    validOnly: boolean
  }
): Promise<VasuTemplateSummary[]> {
  const params = createUrlSearchParams(
    ['validOnly', request.validOnly.toString()]
  )
  const { data: json } = await client.request<JsonOf<VasuTemplateSummary[]>>({
    url: uri`/vasu/templates`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonVasuTemplateSummary(e))
}


/**
* Generated from fi.espoo.evaka.vasu.VasuTemplateController.postTemplate
*/
export async function postTemplate(
  request: {
    body: CreateTemplateRequest
  }
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/vasu/templates`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CreateTemplateRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.vasu.VasuTemplateController.putTemplateContent
*/
export async function putTemplateContent(
  request: {
    id: UUID,
    body: VasuContent
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/vasu/templates/${request.id}/content`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<VasuContent>
  })
  return json
}
