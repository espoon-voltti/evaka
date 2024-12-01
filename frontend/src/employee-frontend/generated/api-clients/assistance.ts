// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AssistanceAction } from 'lib-common/generated/api-types/assistanceaction'
import { AssistanceActionOption } from 'lib-common/generated/api-types/assistanceaction'
import { AssistanceActionRequest } from 'lib-common/generated/api-types/assistanceaction'
import { AssistanceFactorUpdate } from 'lib-common/generated/api-types/assistance'
import { AssistanceResponse } from 'lib-common/generated/api-types/assistance'
import { AxiosHeaders } from 'axios'
import { DaycareAssistanceUpdate } from 'lib-common/generated/api-types/assistance'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { OtherAssistanceMeasureUpdate } from 'lib-common/generated/api-types/assistance'
import { PreschoolAssistanceUpdate } from 'lib-common/generated/api-types/assistance'
import { UUID } from 'lib-common/types'
import { client } from '../../api/client'
import { deserializeJsonAssistanceAction } from 'lib-common/generated/api-types/assistanceaction'
import { deserializeJsonAssistanceResponse } from 'lib-common/generated/api-types/assistance'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.createAssistanceAction
*/
export async function createAssistanceAction(
  request: {
    childId: UUID,
    body: AssistanceActionRequest
  },
  headers?: AxiosHeaders
): Promise<AssistanceAction> {
  const { data: json } = await client.request<JsonOf<AssistanceAction>>({
    url: uri`/employee/children/${request.childId}/assistance-actions`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<AssistanceActionRequest>
  })
  return deserializeJsonAssistanceAction(json)
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.createAssistanceFactor
*/
export async function createAssistanceFactor(
  request: {
    child: UUID,
    body: AssistanceFactorUpdate
  },
  headers?: AxiosHeaders
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/employee/children/${request.child}/assistance-factors`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<AssistanceFactorUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.createDaycareAssistance
*/
export async function createDaycareAssistance(
  request: {
    child: UUID,
    body: DaycareAssistanceUpdate
  },
  headers?: AxiosHeaders
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/employee/children/${request.child}/daycare-assistances`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<DaycareAssistanceUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.createOtherAssistanceMeasure
*/
export async function createOtherAssistanceMeasure(
  request: {
    child: UUID,
    body: OtherAssistanceMeasureUpdate
  },
  headers?: AxiosHeaders
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/employee/children/${request.child}/other-assistance-measures`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<OtherAssistanceMeasureUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.createPreschoolAssistance
*/
export async function createPreschoolAssistance(
  request: {
    child: UUID,
    body: PreschoolAssistanceUpdate
  },
  headers?: AxiosHeaders
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/employee/children/${request.child}/preschool-assistances`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<PreschoolAssistanceUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.deleteAssistanceAction
*/
export async function deleteAssistanceAction(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-actions/${request.id}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.deleteAssistanceFactor
*/
export async function deleteAssistanceFactor(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-factors/${request.id}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.deleteDaycareAssistance
*/
export async function deleteDaycareAssistance(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycare-assistances/${request.id}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.deleteOtherAssistanceMeasure
*/
export async function deleteOtherAssistanceMeasure(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/other-assistance-measures/${request.id}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.deletePreschoolAssistance
*/
export async function deletePreschoolAssistance(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/preschool-assistances/${request.id}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.getAssistanceActionOptions
*/
export async function getAssistanceActionOptions(
  headers?: AxiosHeaders
): Promise<AssistanceActionOption[]> {
  const { data: json } = await client.request<JsonOf<AssistanceActionOption[]>>({
    url: uri`/employee/assistance-action-options`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.getChildAssistance
*/
export async function getChildAssistance(
  request: {
    child: UUID
  },
  headers?: AxiosHeaders
): Promise<AssistanceResponse> {
  const { data: json } = await client.request<JsonOf<AssistanceResponse>>({
    url: uri`/employee/children/${request.child}/assistance`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonAssistanceResponse(json)
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.updateAssistanceAction
*/
export async function updateAssistanceAction(
  request: {
    id: UUID,
    body: AssistanceActionRequest
  },
  headers?: AxiosHeaders
): Promise<AssistanceAction> {
  const { data: json } = await client.request<JsonOf<AssistanceAction>>({
    url: uri`/employee/assistance-actions/${request.id}`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<AssistanceActionRequest>
  })
  return deserializeJsonAssistanceAction(json)
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.updateAssistanceFactor
*/
export async function updateAssistanceFactor(
  request: {
    id: UUID,
    body: AssistanceFactorUpdate
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-factors/${request.id}`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<AssistanceFactorUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.updateDaycareAssistance
*/
export async function updateDaycareAssistance(
  request: {
    id: UUID,
    body: DaycareAssistanceUpdate
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycare-assistances/${request.id}`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<DaycareAssistanceUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.updateOtherAssistanceMeasure
*/
export async function updateOtherAssistanceMeasure(
  request: {
    id: UUID,
    body: OtherAssistanceMeasureUpdate
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/other-assistance-measures/${request.id}`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<OtherAssistanceMeasureUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.updatePreschoolAssistance
*/
export async function updatePreschoolAssistance(
  request: {
    id: UUID,
    body: PreschoolAssistanceUpdate
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/preschool-assistances/${request.id}`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<PreschoolAssistanceUpdate>
  })
  return json
}
