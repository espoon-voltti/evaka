// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { AssistanceAction } from 'lib-common/generated/api-types/assistanceaction'
import type { AssistanceActionId } from 'lib-common/generated/api-types/shared'
import type { AssistanceActionOption } from 'lib-common/generated/api-types/assistanceaction'
import type { AssistanceActionRequest } from 'lib-common/generated/api-types/assistanceaction'
import type { AssistanceFactorId } from 'lib-common/generated/api-types/shared'
import type { AssistanceFactorUpdate } from 'lib-common/generated/api-types/assistance'
import type { AssistanceResponse } from 'lib-common/generated/api-types/assistance'
import type { DaycareAssistanceId } from 'lib-common/generated/api-types/shared'
import type { DaycareAssistanceUpdate } from 'lib-common/generated/api-types/assistance'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { OtherAssistanceMeasureId } from 'lib-common/generated/api-types/shared'
import type { OtherAssistanceMeasureUpdate } from 'lib-common/generated/api-types/assistance'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { PreschoolAssistanceId } from 'lib-common/generated/api-types/shared'
import type { PreschoolAssistanceUpdate } from 'lib-common/generated/api-types/assistance'
import { client } from '../../api/client'
import { deserializeJsonAssistanceAction } from 'lib-common/generated/api-types/assistanceaction'
import { deserializeJsonAssistanceResponse } from 'lib-common/generated/api-types/assistance'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.createAssistanceAction
*/
export async function createAssistanceAction(
  request: {
    childId: PersonId,
    body: AssistanceActionRequest
  }
): Promise<AssistanceAction> {
  const { data: json } = await client.request<JsonOf<AssistanceAction>>({
    url: uri`/employee/children/${request.childId}/assistance-actions`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<AssistanceActionRequest>
  })
  return deserializeJsonAssistanceAction(json)
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.createAssistanceFactor
*/
export async function createAssistanceFactor(
  request: {
    child: PersonId,
    body: AssistanceFactorUpdate
  }
): Promise<AssistanceFactorId> {
  const { data: json } = await client.request<JsonOf<AssistanceFactorId>>({
    url: uri`/employee/children/${request.child}/assistance-factors`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<AssistanceFactorUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.createDaycareAssistance
*/
export async function createDaycareAssistance(
  request: {
    child: PersonId,
    body: DaycareAssistanceUpdate
  }
): Promise<DaycareAssistanceId> {
  const { data: json } = await client.request<JsonOf<DaycareAssistanceId>>({
    url: uri`/employee/children/${request.child}/daycare-assistances`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DaycareAssistanceUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.createOtherAssistanceMeasure
*/
export async function createOtherAssistanceMeasure(
  request: {
    child: PersonId,
    body: OtherAssistanceMeasureUpdate
  }
): Promise<OtherAssistanceMeasureId> {
  const { data: json } = await client.request<JsonOf<OtherAssistanceMeasureId>>({
    url: uri`/employee/children/${request.child}/other-assistance-measures`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<OtherAssistanceMeasureUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.createPreschoolAssistance
*/
export async function createPreschoolAssistance(
  request: {
    child: PersonId,
    body: PreschoolAssistanceUpdate
  }
): Promise<PreschoolAssistanceId> {
  const { data: json } = await client.request<JsonOf<PreschoolAssistanceId>>({
    url: uri`/employee/children/${request.child}/preschool-assistances`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PreschoolAssistanceUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.deleteAssistanceAction
*/
export async function deleteAssistanceAction(
  request: {
    id: AssistanceActionId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-actions/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.deleteAssistanceFactor
*/
export async function deleteAssistanceFactor(
  request: {
    id: AssistanceFactorId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-factors/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.deleteDaycareAssistance
*/
export async function deleteDaycareAssistance(
  request: {
    id: DaycareAssistanceId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycare-assistances/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.deleteOtherAssistanceMeasure
*/
export async function deleteOtherAssistanceMeasure(
  request: {
    id: OtherAssistanceMeasureId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/other-assistance-measures/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.deletePreschoolAssistance
*/
export async function deletePreschoolAssistance(
  request: {
    id: PreschoolAssistanceId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/preschool-assistances/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.getAssistanceActionOptions
*/
export async function getAssistanceActionOptions(): Promise<AssistanceActionOption[]> {
  const { data: json } = await client.request<JsonOf<AssistanceActionOption[]>>({
    url: uri`/employee/assistance-action-options`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.getChildAssistance
*/
export async function getChildAssistance(
  request: {
    child: PersonId
  }
): Promise<AssistanceResponse> {
  const { data: json } = await client.request<JsonOf<AssistanceResponse>>({
    url: uri`/employee/children/${request.child}/assistance`.toString(),
    method: 'GET'
  })
  return deserializeJsonAssistanceResponse(json)
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.updateAssistanceAction
*/
export async function updateAssistanceAction(
  request: {
    id: AssistanceActionId,
    body: AssistanceActionRequest
  }
): Promise<AssistanceAction> {
  const { data: json } = await client.request<JsonOf<AssistanceAction>>({
    url: uri`/employee/assistance-actions/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<AssistanceActionRequest>
  })
  return deserializeJsonAssistanceAction(json)
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.updateAssistanceFactor
*/
export async function updateAssistanceFactor(
  request: {
    id: AssistanceFactorId,
    body: AssistanceFactorUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-factors/${request.id}`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<AssistanceFactorUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.updateDaycareAssistance
*/
export async function updateDaycareAssistance(
  request: {
    id: DaycareAssistanceId,
    body: DaycareAssistanceUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/daycare-assistances/${request.id}`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DaycareAssistanceUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.updateOtherAssistanceMeasure
*/
export async function updateOtherAssistanceMeasure(
  request: {
    id: OtherAssistanceMeasureId,
    body: OtherAssistanceMeasureUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/other-assistance-measures/${request.id}`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<OtherAssistanceMeasureUpdate>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.updatePreschoolAssistance
*/
export async function updatePreschoolAssistance(
  request: {
    id: PreschoolAssistanceId,
    body: PreschoolAssistanceUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/preschool-assistances/${request.id}`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PreschoolAssistanceUpdate>
  })
  return json
}
