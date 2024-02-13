// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import { AnnulAssistanceNeedDecisionRequest } from 'lib-common/generated/api-types/assistanceneed'
import { AnnulAssistanceNeedPreschoolDecisionRequest } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedDecision } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedDecisionBasicsResponse } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedDecisionRequest } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedDecisionResponse } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedPreschoolDecision } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedPreschoolDecisionBasicsResponse } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedPreschoolDecisionForm } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedPreschoolDecisionResponse } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedVoucherCoefficient } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedVoucherCoefficientRequest } from 'lib-common/generated/api-types/assistanceneed'
import { AssistanceNeedVoucherCoefficientResponse } from 'lib-common/generated/api-types/assistanceneed'
import { DecideAssistanceNeedDecisionRequest } from 'lib-common/generated/api-types/assistanceneed'
import { DecideAssistanceNeedPreschoolDecisionRequest } from 'lib-common/generated/api-types/assistanceneed'
import { Employee } from 'lib-common/generated/api-types/pis'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { UpdateDecisionMakerForAssistanceNeedDecisionRequest } from 'lib-common/generated/api-types/assistanceneed'
import { UpdateDecisionMakerForAssistanceNeedPreschoolDecisionRequest } from 'lib-common/generated/api-types/assistanceneed'
import { client } from '../../client'
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
    id: UUID,
    body: AnnulAssistanceNeedDecisionRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/assistance-need-decision/${request.id}/annul`.toString(),
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
    childId: UUID,
    body: AssistanceNeedDecisionRequest
  }
): Promise<AssistanceNeedDecision> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedDecision>>({
    url: uri`/children/${request.childId}/assistance-needs/decision`.toString(),
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
    id: UUID,
    body: DecideAssistanceNeedDecisionRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/assistance-need-decision/${request.id}/decide`.toString(),
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
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/assistance-need-decision/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.getAssistanceDecisionMakerOptions
*/
export async function getAssistanceDecisionMakerOptions(
  request: {
    id: UUID
  }
): Promise<Employee[]> {
  const { data: json } = await client.request<JsonOf<Employee[]>>({
    url: uri`/assistance-need-decision/${request.id}/decision-maker-option`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonEmployee(e))
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.getAssistanceNeedDecision
*/
export async function getAssistanceNeedDecision(
  request: {
    id: UUID
  }
): Promise<AssistanceNeedDecisionResponse> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedDecisionResponse>>({
    url: uri`/assistance-need-decision/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonAssistanceNeedDecisionResponse(json)
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.getAssistanceNeedDecisions
*/
export async function getAssistanceNeedDecisions(
  request: {
    childId: UUID
  }
): Promise<AssistanceNeedDecisionBasicsResponse[]> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedDecisionBasicsResponse[]>>({
    url: uri`/children/${request.childId}/assistance-needs/decisions`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonAssistanceNeedDecisionBasicsResponse(e))
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.markAssistanceNeedDecisionAsOpened
*/
export async function markAssistanceNeedDecisionAsOpened(
  request: {
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/assistance-need-decision/${request.id}/mark-as-opened`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.revertToUnsentAssistanceNeedDecision
*/
export async function revertToUnsentAssistanceNeedDecision(
  request: {
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/assistance-need-decision/${request.id}/revert-to-unsent`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.sendAssistanceNeedDecision
*/
export async function sendAssistanceNeedDecision(
  request: {
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/assistance-need-decision/${request.id}/send`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionController.updateAssistanceNeedDecision
*/
export async function updateAssistanceNeedDecision(
  request: {
    id: UUID,
    body: AssistanceNeedDecisionRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/assistance-need-decision/${request.id}`.toString(),
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
    id: UUID,
    body: UpdateDecisionMakerForAssistanceNeedDecisionRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/assistance-need-decision/${request.id}/update-decision-maker`.toString(),
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
    id: UUID,
    body: AnnulAssistanceNeedPreschoolDecisionRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/assistance-need-preschool-decisions/${request.id}/annul`.toString(),
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
    childId: UUID
  }
): Promise<AssistanceNeedPreschoolDecision> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedPreschoolDecision>>({
    url: uri`/children/${request.childId}/assistance-need-preschool-decisions`.toString(),
    method: 'POST'
  })
  return deserializeJsonAssistanceNeedPreschoolDecision(json)
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.decideAssistanceNeedPreschoolDecision
*/
export async function decideAssistanceNeedPreschoolDecision(
  request: {
    id: UUID,
    body: DecideAssistanceNeedPreschoolDecisionRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/assistance-need-preschool-decisions/${request.id}/decide`.toString(),
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
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/assistance-need-preschool-decisions/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.getAssistanceNeedPreschoolDecision
*/
export async function getAssistanceNeedPreschoolDecision(
  request: {
    id: UUID
  }
): Promise<AssistanceNeedPreschoolDecisionResponse> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedPreschoolDecisionResponse>>({
    url: uri`/assistance-need-preschool-decisions/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonAssistanceNeedPreschoolDecisionResponse(json)
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.getAssistanceNeedPreschoolDecisions
*/
export async function getAssistanceNeedPreschoolDecisions(
  request: {
    childId: UUID
  }
): Promise<AssistanceNeedPreschoolDecisionBasicsResponse[]> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedPreschoolDecisionBasicsResponse[]>>({
    url: uri`/children/${request.childId}/assistance-need-preschool-decisions`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonAssistanceNeedPreschoolDecisionBasicsResponse(e))
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.getAssistancePreschoolDecisionMakerOptions
*/
export async function getAssistancePreschoolDecisionMakerOptions(
  request: {
    id: UUID
  }
): Promise<Employee[]> {
  const { data: json } = await client.request<JsonOf<Employee[]>>({
    url: uri`/assistance-need-preschool-decisions/${request.id}/decision-maker-options`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonEmployee(e))
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.markAssistanceNeedPreschoolDecisionAsOpened
*/
export async function markAssistanceNeedPreschoolDecisionAsOpened(
  request: {
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/assistance-need-preschool-decisions/${request.id}/mark-as-opened`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.revertAssistanceNeedPreschoolDecisionToUnsent
*/
export async function revertAssistanceNeedPreschoolDecisionToUnsent(
  request: {
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/assistance-need-preschool-decisions/${request.id}/unsend`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.sendAssistanceNeedPreschoolDecisionForDecision
*/
export async function sendAssistanceNeedPreschoolDecisionForDecision(
  request: {
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/assistance-need-preschool-decisions/${request.id}/send`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.updateAssistanceNeedPreschoolDecision
*/
export async function updateAssistanceNeedPreschoolDecision(
  request: {
    id: UUID,
    body: AssistanceNeedPreschoolDecisionForm
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/assistance-need-preschool-decisions/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<AssistanceNeedPreschoolDecisionForm>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionController.updateAssistanceNeedPreschoolDecisionDecisionMaker
*/
export async function updateAssistanceNeedPreschoolDecisionDecisionMaker(
  request: {
    id: UUID,
    body: UpdateDecisionMakerForAssistanceNeedPreschoolDecisionRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/assistance-need-preschool-decisions/${request.id}/decision-maker`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<UpdateDecisionMakerForAssistanceNeedPreschoolDecisionRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientController.createAssistanceNeedVoucherCoefficient
*/
export async function createAssistanceNeedVoucherCoefficient(
  request: {
    childId: UUID,
    body: AssistanceNeedVoucherCoefficientRequest
  }
): Promise<AssistanceNeedVoucherCoefficient> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedVoucherCoefficient>>({
    url: uri`/children/${request.childId}/assistance-need-voucher-coefficients`.toString(),
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
    id: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/assistance-need-voucher-coefficients/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientController.getAssistanceNeedVoucherCoefficients
*/
export async function getAssistanceNeedVoucherCoefficients(
  request: {
    childId: UUID
  }
): Promise<AssistanceNeedVoucherCoefficientResponse[]> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedVoucherCoefficientResponse[]>>({
    url: uri`/children/${request.childId}/assistance-need-voucher-coefficients`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonAssistanceNeedVoucherCoefficientResponse(e))
}


/**
* Generated from fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientController.updateAssistanceNeedVoucherCoefficient
*/
export async function updateAssistanceNeedVoucherCoefficient(
  request: {
    id: UUID,
    body: AssistanceNeedVoucherCoefficientRequest
  }
): Promise<AssistanceNeedVoucherCoefficient> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedVoucherCoefficient>>({
    url: uri`/assistance-need-voucher-coefficients/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<AssistanceNeedVoucherCoefficientRequest>
  })
  return deserializeJsonAssistanceNeedVoucherCoefficient(json)
}
