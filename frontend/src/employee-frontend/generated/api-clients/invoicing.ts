// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { AxiosHeaders } from 'axios'
import { CreateRetroactiveFeeDecisionsBody } from 'lib-common/generated/api-types/invoicing'
import { Employee } from 'lib-common/generated/api-types/pis'
import { FeeAlteration } from 'lib-common/generated/api-types/invoicing'
import { FeeAlterationWithPermittedActions } from 'lib-common/generated/api-types/invoicing'
import { FeeDecision } from 'lib-common/generated/api-types/invoicing'
import { FeeDecisionDetailed } from 'lib-common/generated/api-types/invoicing'
import { FeeDecisionTypeRequest } from 'lib-common/generated/api-types/invoicing'
import { FeeThresholds } from 'lib-common/generated/api-types/invoicing'
import { FeeThresholdsWithId } from 'lib-common/generated/api-types/invoicing'
import { GenerateDecisionsBody } from 'lib-common/generated/api-types/invoicing'
import { IncomeCoefficient } from 'lib-common/generated/api-types/invoicing'
import { IncomeNotification } from 'lib-common/generated/api-types/invoicing'
import { IncomeRequest } from 'lib-common/generated/api-types/invoicing'
import { IncomeTypeOptions } from 'lib-common/generated/api-types/invoicing'
import { IncomeWithPermittedActions } from 'lib-common/generated/api-types/invoicing'
import { InvoiceCodes } from 'lib-common/generated/api-types/invoicing'
import { InvoiceCorrectionInsert } from 'lib-common/generated/api-types/invoicing'
import { InvoiceCorrectionWithPermittedActions } from 'lib-common/generated/api-types/invoicing'
import { InvoiceDetailed } from 'lib-common/generated/api-types/invoicing'
import { InvoiceDetailedResponse } from 'lib-common/generated/api-types/invoicing'
import { InvoicePayload } from 'lib-common/generated/api-types/invoicing'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { MarkReplacementDraftSentRequest } from 'lib-common/generated/api-types/invoicing'
import { NoteUpdateBody } from 'lib-common/generated/api-types/invoicing'
import { PagedFeeDecisionSummaries } from 'lib-common/generated/api-types/invoicing'
import { PagedInvoiceSummaryResponses } from 'lib-common/generated/api-types/invoicing'
import { PagedPayments } from 'lib-common/generated/api-types/invoicing'
import { PagedVoucherValueDecisionSummaries } from 'lib-common/generated/api-types/invoicing'
import { SearchFeeDecisionRequest } from 'lib-common/generated/api-types/invoicing'
import { SearchInvoicesRequest } from 'lib-common/generated/api-types/invoicing'
import { SearchPaymentsRequest } from 'lib-common/generated/api-types/invoicing'
import { SearchVoucherValueDecisionRequest } from 'lib-common/generated/api-types/invoicing'
import { SendPaymentsRequest } from 'lib-common/generated/api-types/invoicing'
import { ServiceNeedOptionVoucherValueRange } from 'lib-common/generated/api-types/invoicing'
import { ServiceNeedOptionVoucherValueRangeWithId } from 'lib-common/generated/api-types/invoicing'
import { UUID } from 'lib-common/types'
import { VoucherValueDecisionDetailed } from 'lib-common/generated/api-types/invoicing'
import { VoucherValueDecisionSummary } from 'lib-common/generated/api-types/invoicing'
import { VoucherValueDecisionTypeRequest } from 'lib-common/generated/api-types/invoicing'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonEmployee } from 'lib-common/generated/api-types/pis'
import { deserializeJsonFeeAlterationWithPermittedActions } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonFeeDecision } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonFeeDecisionDetailed } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonFeeThresholdsWithId } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonIncomeNotification } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonIncomeWithPermittedActions } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonInvoiceCorrectionWithPermittedActions } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonInvoiceDetailed } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonInvoiceDetailedResponse } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonPagedFeeDecisionSummaries } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonPagedInvoiceSummaryResponses } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonPagedPayments } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonPagedVoucherValueDecisionSummaries } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonServiceNeedOptionVoucherValueRangeWithId } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonVoucherValueDecisionDetailed } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonVoucherValueDecisionSummary } from 'lib-common/generated/api-types/invoicing'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeAlterationController.createFeeAlteration
*/
export async function createFeeAlteration(
  request: {
    body: FeeAlteration
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/fee-alterations`.toString(),
    method: 'POST',
    headers,
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
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/fee-alterations/${request.feeAlterationId}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeAlterationController.getFeeAlterations
*/
export async function getFeeAlterations(
  request: {
    personId: UUID
  },
  headers?: AxiosHeaders
): Promise<FeeAlterationWithPermittedActions[]> {
  const params = createUrlSearchParams(
    ['personId', request.personId]
  )
  const { data: json } = await client.request<JsonOf<FeeAlterationWithPermittedActions[]>>({
    url: uri`/employee/fee-alterations`.toString(),
    method: 'GET',
    headers,
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
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/fee-alterations/${request.feeAlterationId}`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<FeeAlteration>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionController.confirmFeeDecisionDrafts
*/
export async function confirmFeeDecisionDrafts(
  request: {
    decisionHandlerId?: UUID | null,
    body: UUID[]
  },
  headers?: AxiosHeaders
): Promise<void> {
  const params = createUrlSearchParams(
    ['decisionHandlerId', request.decisionHandlerId]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/fee-decisions/confirm`.toString(),
    method: 'POST',
    headers,
    params,
    data: request.body satisfies JsonCompatible<UUID[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionController.generateRetroactiveFeeDecisions
*/
export async function generateRetroactiveFeeDecisions(
  request: {
    id: UUID,
    body: CreateRetroactiveFeeDecisionsBody
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/fee-decisions/head-of-family/${request.id}/create-retroactive`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<CreateRetroactiveFeeDecisionsBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionController.getFeeDecision
*/
export async function getFeeDecision(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<FeeDecisionDetailed> {
  const { data: json } = await client.request<JsonOf<FeeDecisionDetailed>>({
    url: uri`/employee/fee-decisions/${request.id}`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonFeeDecisionDetailed(json)
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionController.getHeadOfFamilyFeeDecisions
*/
export async function getHeadOfFamilyFeeDecisions(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<FeeDecision[]> {
  const { data: json } = await client.request<JsonOf<FeeDecision[]>>({
    url: uri`/employee/fee-decisions/head-of-family/${request.id}`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonFeeDecision(e))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionController.ignoreFeeDecisionDrafts
*/
export async function ignoreFeeDecisionDrafts(
  request: {
    body: UUID[]
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/fee-decisions/ignore`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<UUID[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionController.searchFeeDecisions
*/
export async function searchFeeDecisions(
  request: {
    body: SearchFeeDecisionRequest
  },
  headers?: AxiosHeaders
): Promise<PagedFeeDecisionSummaries> {
  const { data: json } = await client.request<JsonOf<PagedFeeDecisionSummaries>>({
    url: uri`/employee/fee-decisions/search`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<SearchFeeDecisionRequest>
  })
  return deserializeJsonPagedFeeDecisionSummaries(json)
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionController.setFeeDecisionSent
*/
export async function setFeeDecisionSent(
  request: {
    body: UUID[]
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/fee-decisions/mark-sent`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<UUID[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionController.setFeeDecisionType
*/
export async function setFeeDecisionType(
  request: {
    id: UUID,
    body: FeeDecisionTypeRequest
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/fee-decisions/set-type/${request.id}`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<FeeDecisionTypeRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionController.unignoreFeeDecisionDrafts
*/
export async function unignoreFeeDecisionDrafts(
  request: {
    body: UUID[]
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/fee-decisions/unignore`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<UUID[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionGeneratorController.generateDecisions
*/
export async function generateDecisions(
  request: {
    body: GenerateDecisionsBody
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/fee-decision-generator/generate`.toString(),
    method: 'POST',
    headers,
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
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/finance-basics/fee-thresholds`.toString(),
    method: 'POST',
    headers,
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
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/finance-basics/voucher-values`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<ServiceNeedOptionVoucherValueRange>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FinanceBasicsController.deleteVoucherValue
*/
export async function deleteVoucherValue(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/finance-basics/voucher-values/${request.id}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FinanceBasicsController.getFeeThresholds
*/
export async function getFeeThresholds(
  headers?: AxiosHeaders
): Promise<FeeThresholdsWithId[]> {
  const { data: json } = await client.request<JsonOf<FeeThresholdsWithId[]>>({
    url: uri`/employee/finance-basics/fee-thresholds`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonFeeThresholdsWithId(e))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FinanceBasicsController.getVoucherValues
*/
export async function getVoucherValues(
  headers?: AxiosHeaders
): Promise<Record<UUID, ServiceNeedOptionVoucherValueRangeWithId[]>> {
  const { data: json } = await client.request<JsonOf<Record<UUID, ServiceNeedOptionVoucherValueRangeWithId[]>>>({
    url: uri`/employee/finance-basics/voucher-values`.toString(),
    method: 'GET',
    headers
  })
  return Object.fromEntries(Object.entries(json).map(
    ([k, v]) => [k, v.map(e => deserializeJsonServiceNeedOptionVoucherValueRangeWithId(e))]
  ))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FinanceBasicsController.updateFeeThresholds
*/
export async function updateFeeThresholds(
  request: {
    id: UUID,
    body: FeeThresholds
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/finance-basics/fee-thresholds/${request.id}`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<FeeThresholds>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FinanceBasicsController.updateVoucherValue
*/
export async function updateVoucherValue(
  request: {
    id: UUID,
    body: ServiceNeedOptionVoucherValueRange
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/finance-basics/voucher-values/${request.id}`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<ServiceNeedOptionVoucherValueRange>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FinanceDecisionController.getSelectableFinanceDecisionHandlers
*/
export async function getSelectableFinanceDecisionHandlers(
  headers?: AxiosHeaders
): Promise<Employee[]> {
  const { data: json } = await client.request<JsonOf<Employee[]>>({
    url: uri`/employee/finance-decisions/selectable-handlers`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonEmployee(e))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.IncomeController.createIncome
*/
export async function createIncome(
  request: {
    body: IncomeRequest
  },
  headers?: AxiosHeaders
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/employee/incomes`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<IncomeRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.IncomeController.deleteIncome
*/
export async function deleteIncome(
  request: {
    incomeId: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/incomes/${request.incomeId}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.IncomeController.getIncomeMultipliers
*/
export async function getIncomeMultipliers(
  headers?: AxiosHeaders
): Promise<Record<IncomeCoefficient, number>> {
  const { data: json } = await client.request<JsonOf<Record<IncomeCoefficient, number>>>({
    url: uri`/employee/incomes/multipliers`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.IncomeController.getIncomeNotifications
*/
export async function getIncomeNotifications(
  request: {
    personId: UUID
  },
  headers?: AxiosHeaders
): Promise<IncomeNotification[]> {
  const params = createUrlSearchParams(
    ['personId', request.personId]
  )
  const { data: json } = await client.request<JsonOf<IncomeNotification[]>>({
    url: uri`/employee/incomes/notifications`.toString(),
    method: 'GET',
    headers,
    params
  })
  return json.map(e => deserializeJsonIncomeNotification(e))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.IncomeController.getIncomeTypeOptions
*/
export async function getIncomeTypeOptions(
  headers?: AxiosHeaders
): Promise<IncomeTypeOptions> {
  const { data: json } = await client.request<JsonOf<IncomeTypeOptions>>({
    url: uri`/employee/incomes/types`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.IncomeController.getPersonIncomes
*/
export async function getPersonIncomes(
  request: {
    personId: UUID
  },
  headers?: AxiosHeaders
): Promise<IncomeWithPermittedActions[]> {
  const params = createUrlSearchParams(
    ['personId', request.personId]
  )
  const { data: json } = await client.request<JsonOf<IncomeWithPermittedActions[]>>({
    url: uri`/employee/incomes`.toString(),
    method: 'GET',
    headers,
    params
  })
  return json.map(e => deserializeJsonIncomeWithPermittedActions(e))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.IncomeController.updateIncome
*/
export async function updateIncome(
  request: {
    incomeId: UUID,
    body: IncomeRequest
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/incomes/${request.incomeId}`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<IncomeRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.createDraftInvoices
*/
export async function createDraftInvoices(
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoices/create-drafts`.toString(),
    method: 'POST',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.createReplacementDraftsForHeadOfFamily
*/
export async function createReplacementDraftsForHeadOfFamily(
  request: {
    headOfFamilyId: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoices/create-replacement-drafts/${request.headOfFamilyId}`.toString(),
    method: 'POST',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.deleteDraftInvoices
*/
export async function deleteDraftInvoices(
  request: {
    body: UUID[]
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoices/delete-drafts`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<UUID[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.getHeadOfFamilyInvoices
*/
export async function getHeadOfFamilyInvoices(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<InvoiceDetailed[]> {
  const { data: json } = await client.request<JsonOf<InvoiceDetailed[]>>({
    url: uri`/employee/invoices/head-of-family/${request.id}`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonInvoiceDetailed(e))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.getInvoice
*/
export async function getInvoice(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<InvoiceDetailedResponse> {
  const { data: json } = await client.request<JsonOf<InvoiceDetailedResponse>>({
    url: uri`/employee/invoices/${request.id}`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonInvoiceDetailedResponse(json)
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.getInvoiceCodes
*/
export async function getInvoiceCodes(
  headers?: AxiosHeaders
): Promise<InvoiceCodes> {
  const { data: json } = await client.request<JsonOf<InvoiceCodes>>({
    url: uri`/employee/invoices/codes`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.markInvoicesSent
*/
export async function markInvoicesSent(
  request: {
    body: UUID[]
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoices/mark-sent`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<UUID[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.markReplacementDraftSent
*/
export async function markReplacementDraftSent(
  request: {
    invoiceId: UUID,
    body: MarkReplacementDraftSentRequest
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoices/${request.invoiceId}/mark-replacement-draft-sent`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<MarkReplacementDraftSentRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.searchInvoices
*/
export async function searchInvoices(
  request: {
    body: SearchInvoicesRequest
  },
  headers?: AxiosHeaders
): Promise<PagedInvoiceSummaryResponses> {
  const { data: json } = await client.request<JsonOf<PagedInvoiceSummaryResponses>>({
    url: uri`/employee/invoices/search`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<SearchInvoicesRequest>
  })
  return deserializeJsonPagedInvoiceSummaryResponses(json)
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.sendInvoices
*/
export async function sendInvoices(
  request: {
    invoiceDate?: LocalDate | null,
    dueDate?: LocalDate | null,
    body: UUID[]
  },
  headers?: AxiosHeaders
): Promise<void> {
  const params = createUrlSearchParams(
    ['invoiceDate', request.invoiceDate?.formatIso()],
    ['dueDate', request.dueDate?.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoices/send`.toString(),
    method: 'POST',
    headers,
    params,
    data: request.body satisfies JsonCompatible<UUID[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.sendInvoicesByDate
*/
export async function sendInvoicesByDate(
  request: {
    body: InvoicePayload
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoices/send/by-date`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<InvoicePayload>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceCorrectionsController.createInvoiceCorrection
*/
export async function createInvoiceCorrection(
  request: {
    body: InvoiceCorrectionInsert
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoice-corrections`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<InvoiceCorrectionInsert>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceCorrectionsController.deleteInvoiceCorrection
*/
export async function deleteInvoiceCorrection(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoice-corrections/${request.id}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceCorrectionsController.getPersonInvoiceCorrections
*/
export async function getPersonInvoiceCorrections(
  request: {
    personId: UUID
  },
  headers?: AxiosHeaders
): Promise<InvoiceCorrectionWithPermittedActions[]> {
  const { data: json } = await client.request<JsonOf<InvoiceCorrectionWithPermittedActions[]>>({
    url: uri`/employee/invoice-corrections/${request.personId}`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonInvoiceCorrectionWithPermittedActions(e))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceCorrectionsController.updateInvoiceCorrectionNote
*/
export async function updateInvoiceCorrectionNote(
  request: {
    id: UUID,
    body: NoteUpdateBody
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoice-corrections/${request.id}/note`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<NoteUpdateBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.PaymentController.confirmDraftPayments
*/
export async function confirmDraftPayments(
  request: {
    body: UUID[]
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/payments/confirm`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<UUID[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.PaymentController.createPaymentDrafts
*/
export async function createPaymentDrafts(
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/payments/create-drafts`.toString(),
    method: 'POST',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.PaymentController.deleteDraftPayments
*/
export async function deleteDraftPayments(
  request: {
    body: UUID[]
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/payments/delete-drafts`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<UUID[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.PaymentController.revertPaymentsToDrafts
*/
export async function revertPaymentsToDrafts(
  request: {
    body: UUID[]
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/payments/revert-to-draft`.toString(),
    method: 'POST',
    headers,
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
  },
  headers?: AxiosHeaders
): Promise<PagedPayments> {
  const { data: json } = await client.request<JsonOf<PagedPayments>>({
    url: uri`/employee/payments/search`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<SearchPaymentsRequest>
  })
  return deserializeJsonPagedPayments(json)
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.PaymentController.sendPayments
*/
export async function sendPayments(
  request: {
    body: SendPaymentsRequest
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/payments/send`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<SendPaymentsRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController.generateRetroactiveVoucherValueDecisions
*/
export async function generateRetroactiveVoucherValueDecisions(
  request: {
    id: UUID,
    body: CreateRetroactiveFeeDecisionsBody
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/value-decisions/head-of-family/${request.id}/create-retroactive`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<CreateRetroactiveFeeDecisionsBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController.getHeadOfFamilyVoucherValueDecisions
*/
export async function getHeadOfFamilyVoucherValueDecisions(
  request: {
    headOfFamilyId: UUID
  },
  headers?: AxiosHeaders
): Promise<VoucherValueDecisionSummary[]> {
  const { data: json } = await client.request<JsonOf<VoucherValueDecisionSummary[]>>({
    url: uri`/employee/value-decisions/head-of-family/${request.headOfFamilyId}`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonVoucherValueDecisionSummary(e))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController.getVoucherValueDecision
*/
export async function getVoucherValueDecision(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<VoucherValueDecisionDetailed> {
  const { data: json } = await client.request<JsonOf<VoucherValueDecisionDetailed>>({
    url: uri`/employee/value-decisions/${request.id}`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonVoucherValueDecisionDetailed(json)
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController.ignoreVoucherValueDecisionDrafts
*/
export async function ignoreVoucherValueDecisionDrafts(
  request: {
    body: UUID[]
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/value-decisions/ignore`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<UUID[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController.markVoucherValueDecisionSent
*/
export async function markVoucherValueDecisionSent(
  request: {
    body: UUID[]
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/value-decisions/mark-sent`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<UUID[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController.searchVoucherValueDecisions
*/
export async function searchVoucherValueDecisions(
  request: {
    body: SearchVoucherValueDecisionRequest
  },
  headers?: AxiosHeaders
): Promise<PagedVoucherValueDecisionSummaries> {
  const { data: json } = await client.request<JsonOf<PagedVoucherValueDecisionSummaries>>({
    url: uri`/employee/value-decisions/search`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<SearchVoucherValueDecisionRequest>
  })
  return deserializeJsonPagedVoucherValueDecisionSummaries(json)
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController.sendVoucherValueDecisionDrafts
*/
export async function sendVoucherValueDecisionDrafts(
  request: {
    decisionHandlerId?: UUID | null,
    body: UUID[]
  },
  headers?: AxiosHeaders
): Promise<void> {
  const params = createUrlSearchParams(
    ['decisionHandlerId', request.decisionHandlerId]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/value-decisions/send`.toString(),
    method: 'POST',
    headers,
    params,
    data: request.body satisfies JsonCompatible<UUID[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController.setVoucherValueDecisionType
*/
export async function setVoucherValueDecisionType(
  request: {
    id: UUID,
    body: VoucherValueDecisionTypeRequest
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/value-decisions/set-type/${request.id}`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<VoucherValueDecisionTypeRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController.unignoreVoucherValueDecisionDrafts
*/
export async function unignoreVoucherValueDecisionDrafts(
  request: {
    body: UUID[]
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/value-decisions/unignore`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<UUID[]>
  })
  return json
}
