// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { AnnulAssistanceNeedDecisionRequest } from 'lib-common/generated/api-types/assistanceneed'
import type { AnnulAssistanceNeedPreschoolDecisionRequest } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedDecision } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedDecisionBasicsResponse } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedDecisionId } from 'lib-common/generated/api-types/shared'
import type { AssistanceNeedDecisionRequest } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedDecisionResponse } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedPreschoolDecision } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedPreschoolDecisionBasicsResponse } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedPreschoolDecisionForm } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedPreschoolDecisionId } from 'lib-common/generated/api-types/shared'
import type { AssistanceNeedPreschoolDecisionResponse } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedVoucherCoefficient } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedVoucherCoefficientId } from 'lib-common/generated/api-types/shared'
import type { AssistanceNeedVoucherCoefficientRequest } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedVoucherCoefficientResponse } from 'lib-common/generated/api-types/assistanceneed'
import type { DecideAssistanceNeedDecisionRequest } from 'lib-common/generated/api-types/assistanceneed'
import type { DecideAssistanceNeedPreschoolDecisionRequest } from 'lib-common/generated/api-types/assistanceneed'
import type { Employee } from 'lib-common/generated/api-types/pis'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { UpdateDecisionMakerForAssistanceNeedDecisionRequest } from 'lib-common/generated/api-types/assistanceneed'
import type { Uri } from 'lib-common/uri'
import { client } from '../../api/client'
import { deserializeJsonAssistanceNeedDecision } from 'lib-common/generated/api-types/assistanceneed'
import { deserializeJsonAssistanceNeedDecisionBasicsResponse } from 'lib-common/generated/api-types/assistanceneed'
import { deserializeJsonAssistanceNeedDecisionResponse } from 'lib-common/generated/api-types/assistanceneed'
import { deserializeJsonAssistanceNeedPreschoolDecision } from 'lib-common/generated/api-types/assistanceneed'
import { deserializeJsonAssistanceNeedPreschoolDecisionBasicsResponse } from 'lib-common/generated/api-types/assistanceneed'
import { deserializeJsonAssistanceNeedPreschoolDecisionResponse } from 'lib-common/generated/api-types/assistanceneed'
import { deserializeJsonAssistanceNeedVoucherCoefficient } from 'lib-common/generated/api-types/assistanceneed'
import { deserializeJsonAssistanceNeedVoucherCoefficientResponse } from 'lib-common/generated/api-types/assistanceneed'
import { deserializeJsonEmployee } from 'lib-common/generated/api-types/pis'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.annulAssistanceNeedDecision
*/
export async function annulAssistanceNeedDecision(
  request: {
    id: AssistanceNeedDecisionId,
    body: AnnulAssistanceNeedDecisionRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-need-decision/${request.id}/annul`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<AnnulAssistanceNeedDecisionRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.createAssistanceNeedDecision
*/
export async function createAssistanceNeedDecision(
  request: {
    childId: PersonId,
    body: AssistanceNeedDecisionRequest
  }
): Promise<AssistanceNeedDecision> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedDecision>>({
    url: uri`/employee/children/${request.childId}/assistance-needs/decision`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<AssistanceNeedDecisionRequest>
  })
  return deserializeJsonAssistanceNeedDecision(json)
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.decideAssistanceNeedDecision
*/
export async function decideAssistanceNeedDecision(
  request: {
    id: AssistanceNeedDecisionId,
    body: DecideAssistanceNeedDecisionRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-need-decision/${request.id}/decide`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DecideAssistanceNeedDecisionRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.deleteAssistanceNeedDecision
*/
export async function deleteAssistanceNeedDecision(
  request: {
    id: AssistanceNeedDecisionId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-need-decision/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.getAssistanceDecisionMakerOptions
*/
export async function getAssistanceDecisionMakerOptions(
  request: {
    id: AssistanceNeedDecisionId
  }
): Promise<Employee[]> {
  const { data: json } = await client.request<JsonOf<Employee[]>>({
    url: uri`/employee/assistance-need-decision/${request.id}/decision-maker-option`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonEmployee(e))
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.getAssistanceNeedDecision
*/
export async function getAssistanceNeedDecision(
  request: {
    id: AssistanceNeedDecisionId
  }
): Promise<AssistanceNeedDecisionResponse> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedDecisionResponse>>({
    url: uri`/employee/assistance-need-decision/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonAssistanceNeedDecisionResponse(json)
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.getAssistanceNeedDecisionPdf
*/
export function getAssistanceNeedDecisionPdf(
  request: {
    id: AssistanceNeedDecisionId
  }
): { url: Uri } {
  return {
    url: uri`/employee/assistance-need-decision/${request.id}/pdf`.withBaseUrl(client.defaults.baseURL ?? '')
  }
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.getAssistanceNeedDecisions
*/
export async function getAssistanceNeedDecisions(
  request: {
    childId: PersonId
  }
): Promise<AssistanceNeedDecisionBasicsResponse[]> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedDecisionBasicsResponse[]>>({
    url: uri`/employee/children/${request.childId}/assistance-needs/decisions`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonAssistanceNeedDecisionBasicsResponse(e))
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.markAssistanceNeedDecisionAsOpened
*/
export async function markAssistanceNeedDecisionAsOpened(
  request: {
    id: AssistanceNeedDecisionId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-need-decision/${request.id}/mark-as-opened`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.revertToUnsentAssistanceNeedDecision
*/
export async function revertToUnsentAssistanceNeedDecision(
  request: {
    id: AssistanceNeedDecisionId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-need-decision/${request.id}/revert-to-unsent`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.sendAssistanceNeedDecision
*/
export async function sendAssistanceNeedDecision(
  request: {
    id: AssistanceNeedDecisionId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-need-decision/${request.id}/send`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.updateAssistanceNeedDecision
*/
export async function updateAssistanceNeedDecision(
  request: {
    id: AssistanceNeedDecisionId,
    body: AssistanceNeedDecisionRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-need-decision/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<AssistanceNeedDecisionRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.updateAssistanceNeedDecisionDecisionMaker
*/
export async function updateAssistanceNeedDecisionDecisionMaker(
  request: {
    id: AssistanceNeedDecisionId,
    body: UpdateDecisionMakerForAssistanceNeedDecisionRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-need-decision/${request.id}/update-decision-maker`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<UpdateDecisionMakerForAssistanceNeedDecisionRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.annulAssistanceNeedPreschoolDecision
*/
export async function annulAssistanceNeedPreschoolDecision(
  request: {
    id: AssistanceNeedPreschoolDecisionId,
    body: AnnulAssistanceNeedPreschoolDecisionRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-need-preschool-decisions/${request.id}/annul`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<AnnulAssistanceNeedPreschoolDecisionRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.createAssistanceNeedPreschoolDecision
*/
export async function createAssistanceNeedPreschoolDecision(
  request: {
    childId: PersonId
  }
): Promise<AssistanceNeedPreschoolDecision> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedPreschoolDecision>>({
    url: uri`/employee/children/${request.childId}/assistance-need-preschool-decisions`.toString(),
    method: 'POST'
  })
  return deserializeJsonAssistanceNeedPreschoolDecision(json)
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.decideAssistanceNeedPreschoolDecision
*/
export async function decideAssistanceNeedPreschoolDecision(
  request: {
    id: AssistanceNeedPreschoolDecisionId,
    body: DecideAssistanceNeedPreschoolDecisionRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-need-preschool-decisions/${request.id}/decide`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<DecideAssistanceNeedPreschoolDecisionRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.deleteAssistanceNeedPreschoolDecision
*/
export async function deleteAssistanceNeedPreschoolDecision(
  request: {
    id: AssistanceNeedPreschoolDecisionId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-need-preschool-decisions/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.getAssistanceNeedPreschoolDecision
*/
export async function getAssistanceNeedPreschoolDecision(
  request: {
    id: AssistanceNeedPreschoolDecisionId
  }
): Promise<AssistanceNeedPreschoolDecisionResponse> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedPreschoolDecisionResponse>>({
    url: uri`/employee/assistance-need-preschool-decisions/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonAssistanceNeedPreschoolDecisionResponse(json)
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.getAssistanceNeedPreschoolDecisionPdf
*/
export function getAssistanceNeedPreschoolDecisionPdf(
  request: {
    id: AssistanceNeedPreschoolDecisionId
  }
): { url: Uri } {
  return {
    url: uri`/employee/assistance-need-preschool-decisions/${request.id}/pdf`.withBaseUrl(client.defaults.baseURL ?? '')
  }
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.getAssistanceNeedPreschoolDecisions
*/
export async function getAssistanceNeedPreschoolDecisions(
  request: {
    childId: PersonId
  }
): Promise<AssistanceNeedPreschoolDecisionBasicsResponse[]> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedPreschoolDecisionBasicsResponse[]>>({
    url: uri`/employee/children/${request.childId}/assistance-need-preschool-decisions`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonAssistanceNeedPreschoolDecisionBasicsResponse(e))
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.getAssistancePreschoolDecisionMakerOptions
*/
export async function getAssistancePreschoolDecisionMakerOptions(
  request: {
    id: AssistanceNeedPreschoolDecisionId
  }
): Promise<Employee[]> {
  const { data: json } = await client.request<JsonOf<Employee[]>>({
    url: uri`/employee/assistance-need-preschool-decisions/${request.id}/decision-maker-options`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonEmployee(e))
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.markAssistanceNeedPreschoolDecisionAsOpened
*/
export async function markAssistanceNeedPreschoolDecisionAsOpened(
  request: {
    id: AssistanceNeedPreschoolDecisionId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-need-preschool-decisions/${request.id}/mark-as-opened`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.revertAssistanceNeedPreschoolDecisionToUnsent
*/
export async function revertAssistanceNeedPreschoolDecisionToUnsent(
  request: {
    id: AssistanceNeedPreschoolDecisionId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-need-preschool-decisions/${request.id}/unsend`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.sendAssistanceNeedPreschoolDecisionForDecision
*/
export async function sendAssistanceNeedPreschoolDecisionForDecision(
  request: {
    id: AssistanceNeedPreschoolDecisionId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-need-preschool-decisions/${request.id}/send`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.updateAssistanceNeedPreschoolDecision
*/
export async function updateAssistanceNeedPreschoolDecision(
  request: {
    id: AssistanceNeedPreschoolDecisionId,
    body: AssistanceNeedPreschoolDecisionForm
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-need-preschool-decisions/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<AssistanceNeedPreschoolDecisionForm>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientController.createAssistanceNeedVoucherCoefficient
*/
export async function createAssistanceNeedVoucherCoefficient(
  request: {
    childId: PersonId,
    body: AssistanceNeedVoucherCoefficientRequest
  }
): Promise<AssistanceNeedVoucherCoefficient> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedVoucherCoefficient>>({
    url: uri`/employee/children/${request.childId}/assistance-need-voucher-coefficients`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<AssistanceNeedVoucherCoefficientRequest>
  })
  return deserializeJsonAssistanceNeedVoucherCoefficient(json)
}


/**
* Generated from fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientController.deleteAssistanceNeedVoucherCoefficient
*/
export async function deleteAssistanceNeedVoucherCoefficient(
  request: {
    id: AssistanceNeedVoucherCoefficientId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/assistance-need-voucher-coefficients/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientController.getAssistanceNeedVoucherCoefficients
*/
export async function getAssistanceNeedVoucherCoefficients(
  request: {
    childId: PersonId
  }
): Promise<AssistanceNeedVoucherCoefficientResponse[]> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedVoucherCoefficientResponse[]>>({
    url: uri`/employee/children/${request.childId}/assistance-need-voucher-coefficients`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonAssistanceNeedVoucherCoefficientResponse(e))
}


/**
* Generated from fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientController.updateAssistanceNeedVoucherCoefficient
*/
export async function updateAssistanceNeedVoucherCoefficient(
  request: {
    id: AssistanceNeedVoucherCoefficientId,
    body: AssistanceNeedVoucherCoefficientRequest
  }
): Promise<AssistanceNeedVoucherCoefficient> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedVoucherCoefficient>>({
    url: uri`/employee/assistance-need-voucher-coefficients/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<AssistanceNeedVoucherCoefficientRequest>
  })
  return deserializeJsonAssistanceNeedVoucherCoefficient(json)
}
