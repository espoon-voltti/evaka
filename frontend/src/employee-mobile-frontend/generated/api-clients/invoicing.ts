// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { Employee } from 'lib-common/generated/api-types/pis'
import { FeeAlteration } from 'lib-common/generated/api-types/invoicing'
import { FeeAlterationWithPermittedActions } from 'lib-common/generated/api-types/invoicing'
import { FeeThresholds } from 'lib-common/generated/api-types/invoicing'
import { FeeThresholdsWithId } from 'lib-common/generated/api-types/invoicing'
import { GenerateDecisionsBody } from 'lib-common/generated/api-types/invoicing'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PagedPayments } from 'lib-common/generated/api-types/invoicing'
import { SearchPaymentsRequest } from 'lib-common/generated/api-types/invoicing'
import { ServiceNeedOptionVoucherValueRange } from 'lib-common/generated/api-types/invoicing'
import { UUID } from 'lib-common/types'
import { client } from '../../client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonEmployee } from 'lib-common/generated/api-types/pis'
import { deserializeJsonFeeAlterationWithPermittedActions } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonFeeThresholdsWithId } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonPagedPayments } from 'lib-common/generated/api-types/invoicing'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeAlterationController.createFeeAlteration
*/
export async function createFeeAlteration(
  request: {
    body: FeeAlteration
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/fee-alterations`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<FeeAlteration>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeAlterationController.deleteFeeAlteration
*/
export async function deleteFeeAlteration(
  request: {
    feeAlterationId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/fee-alterations/${request.feeAlterationId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeAlterationController.getFeeAlterations
*/
export async function getFeeAlterations(
  request: {
    personId: UUID
  }
): Promise<FeeAlterationWithPermittedActions[]> {
  const params = createUrlSearchParams(
    ['personId', request.personId]
  )
  const { data: json } = await client.request<JsonOf<FeeAlterationWithPermittedActions[]>>({
    url: uri`/fee-alterations`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonFeeAlterationWithPermittedActions(e))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeAlterationController.updateFeeAlteration
*/
export async function updateFeeAlteration(
  request: {
    feeAlterationId: UUID,
    body: FeeAlteration
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/fee-alterations/${request.feeAlterationId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<FeeAlteration>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionGeneratorController.generateDecisions
*/
export async function generateDecisions(
  request: {
    body: GenerateDecisionsBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/fee-decision-generator/generate`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<GenerateDecisionsBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FinanceBasicsController.createFeeThresholds
*/
export async function createFeeThresholds(
  request: {
    body: FeeThresholds
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/finance-basics/fee-thresholds`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<FeeThresholds>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FinanceBasicsController.createVoucherValue
*/
export async function createVoucherValue(
  request: {
    body: ServiceNeedOptionVoucherValueRange
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/finance-basics/voucher-values`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ServiceNeedOptionVoucherValueRange>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FinanceBasicsController.getFeeThresholds
*/
export async function getFeeThresholds(): Promise<FeeThresholdsWithId[]> {
  const { data: json } = await client.request<JsonOf<FeeThresholdsWithId[]>>({
    url: uri`/finance-basics/fee-thresholds`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonFeeThresholdsWithId(e))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FinanceBasicsController.updateFeeThresholds
*/
export async function updateFeeThresholds(
  request: {
    id: UUID,
    body: FeeThresholds
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/finance-basics/fee-thresholds/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<FeeThresholds>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FinanceDecisionController.getSelectableFinanceDecisionHandlers
*/
export async function getSelectableFinanceDecisionHandlers(): Promise<Employee[]> {
  const { data: json } = await client.request<JsonOf<Employee[]>>({
    url: uri`/finance-decisions/selectable-handlers`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonEmployee(e))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.PaymentController.createPaymentDrafts
*/
export async function createPaymentDrafts(): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/payments/create-drafts`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.PaymentController.deleteDraftPayments
*/
export async function deleteDraftPayments(
  request: {
    body: UUID[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/payments/delete-drafts`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<UUID[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.PaymentController.searchPayments
*/
export async function searchPayments(
  request: {
    body: SearchPaymentsRequest
  }
): Promise<PagedPayments> {
  const { data: json } = await client.request<JsonOf<PagedPayments>>({
    url: uri`/payments/search`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<SearchPaymentsRequest>
  })
  return deserializeJsonPagedPayments(json)
}
