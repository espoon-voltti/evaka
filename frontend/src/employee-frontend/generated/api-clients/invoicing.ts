// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { CreateRetroactiveFeeDecisionsBody } from 'lib-common/generated/api-types/invoicing'
import { Employee } from 'lib-common/generated/api-types/pis'
import { EmployeeId } from 'lib-common/generated/api-types/shared'
import { FeeAlteration } from 'lib-common/generated/api-types/invoicing'
import { FeeAlterationId } from 'lib-common/generated/api-types/shared'
import { FeeAlterationWithPermittedActions } from 'lib-common/generated/api-types/invoicing'
import { FeeDecision } from 'lib-common/generated/api-types/invoicing'
import { FeeDecisionId } from 'lib-common/generated/api-types/shared'
import { FeeDecisionResponse } from 'lib-common/generated/api-types/invoicing'
import { FeeDecisionTypeRequest } from 'lib-common/generated/api-types/invoicing'
import { FeeThresholds } from 'lib-common/generated/api-types/invoicing'
import { FeeThresholdsId } from 'lib-common/generated/api-types/shared'
import { FeeThresholdsWithId } from 'lib-common/generated/api-types/invoicing'
import { GenerateDecisionsBody } from 'lib-common/generated/api-types/invoicing'
import { IncomeCoefficient } from 'lib-common/generated/api-types/invoicing'
import { IncomeId } from 'lib-common/generated/api-types/shared'
import { IncomeNotification } from 'lib-common/generated/api-types/invoicing'
import { IncomeRequest } from 'lib-common/generated/api-types/invoicing'
import { IncomeTypeOptions } from 'lib-common/generated/api-types/invoicing'
import { IncomeWithPermittedActions } from 'lib-common/generated/api-types/invoicing'
import { InvoiceCodes } from 'lib-common/generated/api-types/invoicing'
import { InvoiceCorrectionId } from 'lib-common/generated/api-types/shared'
import { InvoiceCorrectionInsert } from 'lib-common/generated/api-types/invoicing'
import { InvoiceCorrectionWithPermittedActions } from 'lib-common/generated/api-types/invoicing'
import { InvoiceDetailed } from 'lib-common/generated/api-types/invoicing'
import { InvoiceDetailedResponse } from 'lib-common/generated/api-types/invoicing'
import { InvoiceId } from 'lib-common/generated/api-types/shared'
import { InvoicePayload } from 'lib-common/generated/api-types/invoicing'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { MarkReplacementDraftSentRequest } from 'lib-common/generated/api-types/invoicing'
import { NoteUpdateBody } from 'lib-common/generated/api-types/invoicing'
import { PagedFeeDecisionSummaries } from 'lib-common/generated/api-types/invoicing'
import { PagedInvoiceSummaryResponses } from 'lib-common/generated/api-types/invoicing'
import { PagedPayments } from 'lib-common/generated/api-types/invoicing'
import { PagedVoucherValueDecisionSummaries } from 'lib-common/generated/api-types/invoicing'
import { PaymentId } from 'lib-common/generated/api-types/shared'
import { PersonId } from 'lib-common/generated/api-types/shared'
import { SearchFeeDecisionRequest } from 'lib-common/generated/api-types/invoicing'
import { SearchInvoicesRequest } from 'lib-common/generated/api-types/invoicing'
import { SearchPaymentsRequest } from 'lib-common/generated/api-types/invoicing'
import { SearchVoucherValueDecisionRequest } from 'lib-common/generated/api-types/invoicing'
import { SendPaymentsRequest } from 'lib-common/generated/api-types/invoicing'
import { ServiceNeedOptionId } from 'lib-common/generated/api-types/shared'
import { ServiceNeedOptionVoucherValueId } from 'lib-common/generated/api-types/shared'
import { ServiceNeedOptionVoucherValueRange } from 'lib-common/generated/api-types/invoicing'
import { ServiceNeedOptionVoucherValueRangeWithId } from 'lib-common/generated/api-types/invoicing'
import { VoucherValueDecisionId } from 'lib-common/generated/api-types/shared'
import { VoucherValueDecisionResponse } from 'lib-common/generated/api-types/invoicing'
import { VoucherValueDecisionSummary } from 'lib-common/generated/api-types/invoicing'
import { VoucherValueDecisionTypeRequest } from 'lib-common/generated/api-types/invoicing'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonEmployee } from 'lib-common/generated/api-types/pis'
import { deserializeJsonFeeAlterationWithPermittedActions } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonFeeDecision } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonFeeDecisionResponse } from 'lib-common/generated/api-types/invoicing'
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
import { deserializeJsonVoucherValueDecisionResponse } from 'lib-common/generated/api-types/invoicing'
import { deserializeJsonVoucherValueDecisionSummary } from 'lib-common/generated/api-types/invoicing'
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
    url: uri`/employee/fee-alterations`.toString(),
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
    feeAlterationId: FeeAlterationId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/fee-alterations/${request.feeAlterationId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeAlterationController.getFeeAlterations
*/
export async function getFeeAlterations(
  request: {
    personId: PersonId
  }
): Promise<FeeAlterationWithPermittedActions[]> {
  const params = createUrlSearchParams(
    ['personId', request.personId]
  )
  const { data: json } = await client.request<JsonOf<FeeAlterationWithPermittedActions[]>>({
    url: uri`/employee/fee-alterations`.toString(),
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
    feeAlterationId: FeeAlterationId,
    body: FeeAlteration
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/fee-alterations/${request.feeAlterationId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<FeeAlteration>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionController.confirmFeeDecisionDrafts
*/
export async function confirmFeeDecisionDrafts(
  request: {
    decisionHandlerId?: EmployeeId | null,
    body: FeeDecisionId[]
  }
): Promise<void> {
  const params = createUrlSearchParams(
    ['decisionHandlerId', request.decisionHandlerId]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/fee-decisions/confirm`.toString(),
    method: 'POST',
    params,
    data: request.body satisfies JsonCompatible<FeeDecisionId[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionController.generateRetroactiveFeeDecisions
*/
export async function generateRetroactiveFeeDecisions(
  request: {
    id: PersonId,
    body: CreateRetroactiveFeeDecisionsBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/fee-decisions/head-of-family/${request.id}/create-retroactive`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CreateRetroactiveFeeDecisionsBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionController.getFeeDecision
*/
export async function getFeeDecision(
  request: {
    id: FeeDecisionId
  }
): Promise<FeeDecisionResponse> {
  const { data: json } = await client.request<JsonOf<FeeDecisionResponse>>({
    url: uri`/employee/fee-decisions/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonFeeDecisionResponse(json)
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionController.getHeadOfFamilyFeeDecisions
*/
export async function getHeadOfFamilyFeeDecisions(
  request: {
    id: PersonId
  }
): Promise<FeeDecision[]> {
  const { data: json } = await client.request<JsonOf<FeeDecision[]>>({
    url: uri`/employee/fee-decisions/head-of-family/${request.id}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonFeeDecision(e))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionController.ignoreFeeDecisionDrafts
*/
export async function ignoreFeeDecisionDrafts(
  request: {
    body: FeeDecisionId[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/fee-decisions/ignore`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<FeeDecisionId[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionController.searchFeeDecisions
*/
export async function searchFeeDecisions(
  request: {
    body: SearchFeeDecisionRequest
  }
): Promise<PagedFeeDecisionSummaries> {
  const { data: json } = await client.request<JsonOf<PagedFeeDecisionSummaries>>({
    url: uri`/employee/fee-decisions/search`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<SearchFeeDecisionRequest>
  })
  return deserializeJsonPagedFeeDecisionSummaries(json)
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionController.setFeeDecisionSent
*/
export async function setFeeDecisionSent(
  request: {
    body: FeeDecisionId[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/fee-decisions/mark-sent`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<FeeDecisionId[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionController.setFeeDecisionType
*/
export async function setFeeDecisionType(
  request: {
    id: FeeDecisionId,
    body: FeeDecisionTypeRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/fee-decisions/set-type/${request.id}`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<FeeDecisionTypeRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionController.unignoreFeeDecisionDrafts
*/
export async function unignoreFeeDecisionDrafts(
  request: {
    body: FeeDecisionId[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/fee-decisions/unignore`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<FeeDecisionId[]>
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
    url: uri`/employee/fee-decision-generator/generate`.toString(),
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
    url: uri`/employee/finance-basics/fee-thresholds`.toString(),
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
    url: uri`/employee/finance-basics/voucher-values`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ServiceNeedOptionVoucherValueRange>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FinanceBasicsController.deleteVoucherValue
*/
export async function deleteVoucherValue(
  request: {
    id: ServiceNeedOptionVoucherValueId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/finance-basics/voucher-values/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FinanceBasicsController.getFeeThresholds
*/
export async function getFeeThresholds(): Promise<FeeThresholdsWithId[]> {
  const { data: json } = await client.request<JsonOf<FeeThresholdsWithId[]>>({
    url: uri`/employee/finance-basics/fee-thresholds`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonFeeThresholdsWithId(e))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FinanceBasicsController.getVoucherValues
*/
export async function getVoucherValues(): Promise<Partial<Record<ServiceNeedOptionId, ServiceNeedOptionVoucherValueRangeWithId[]>>> {
  const { data: json } = await client.request<JsonOf<Partial<Record<ServiceNeedOptionId, ServiceNeedOptionVoucherValueRangeWithId[]>>>>({
    url: uri`/employee/finance-basics/voucher-values`.toString(),
    method: 'GET'
  })
  return Object.fromEntries(Object.entries(json).map(
    ([k, v]) => [k, v !== undefined ? v.map(e => deserializeJsonServiceNeedOptionVoucherValueRangeWithId(e)) : v]
  ))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FinanceBasicsController.updateFeeThresholds
*/
export async function updateFeeThresholds(
  request: {
    id: FeeThresholdsId,
    body: FeeThresholds
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/finance-basics/fee-thresholds/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<FeeThresholds>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FinanceBasicsController.updateVoucherValue
*/
export async function updateVoucherValue(
  request: {
    id: ServiceNeedOptionVoucherValueId,
    body: ServiceNeedOptionVoucherValueRange
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/finance-basics/voucher-values/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<ServiceNeedOptionVoucherValueRange>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.FinanceDecisionController.getSelectableFinanceDecisionHandlers
*/
export async function getSelectableFinanceDecisionHandlers(): Promise<Employee[]> {
  const { data: json } = await client.request<JsonOf<Employee[]>>({
    url: uri`/employee/finance-decisions/selectable-handlers`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonEmployee(e))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.IncomeController.createIncome
*/
export async function createIncome(
  request: {
    body: IncomeRequest
  }
): Promise<IncomeId> {
  const { data: json } = await client.request<JsonOf<IncomeId>>({
    url: uri`/employee/incomes`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<IncomeRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.IncomeController.deleteIncome
*/
export async function deleteIncome(
  request: {
    incomeId: IncomeId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/incomes/${request.incomeId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.IncomeController.getIncomeMultipliers
*/
export async function getIncomeMultipliers(): Promise<Partial<Record<IncomeCoefficient, number>>> {
  const { data: json } = await client.request<JsonOf<Partial<Record<IncomeCoefficient, number>>>>({
    url: uri`/employee/incomes/multipliers`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.IncomeController.getIncomeNotifications
*/
export async function getIncomeNotifications(
  request: {
    personId: PersonId
  }
): Promise<IncomeNotification[]> {
  const params = createUrlSearchParams(
    ['personId', request.personId]
  )
  const { data: json } = await client.request<JsonOf<IncomeNotification[]>>({
    url: uri`/employee/incomes/notifications`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonIncomeNotification(e))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.IncomeController.getIncomeTypeOptions
*/
export async function getIncomeTypeOptions(): Promise<IncomeTypeOptions> {
  const { data: json } = await client.request<JsonOf<IncomeTypeOptions>>({
    url: uri`/employee/incomes/types`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.IncomeController.getPersonIncomes
*/
export async function getPersonIncomes(
  request: {
    personId: PersonId
  }
): Promise<IncomeWithPermittedActions[]> {
  const params = createUrlSearchParams(
    ['personId', request.personId]
  )
  const { data: json } = await client.request<JsonOf<IncomeWithPermittedActions[]>>({
    url: uri`/employee/incomes`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonIncomeWithPermittedActions(e))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.IncomeController.updateIncome
*/
export async function updateIncome(
  request: {
    incomeId: IncomeId,
    body: IncomeRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/incomes/${request.incomeId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<IncomeRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.createDraftInvoices
*/
export async function createDraftInvoices(): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoices/create-drafts`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.createReplacementDraftsForHeadOfFamily
*/
export async function createReplacementDraftsForHeadOfFamily(
  request: {
    headOfFamilyId: PersonId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoices/create-replacement-drafts/${request.headOfFamilyId}`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.deleteDraftInvoices
*/
export async function deleteDraftInvoices(
  request: {
    body: InvoiceId[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoices/delete-drafts`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<InvoiceId[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.getHeadOfFamilyInvoices
*/
export async function getHeadOfFamilyInvoices(
  request: {
    id: PersonId
  }
): Promise<InvoiceDetailed[]> {
  const { data: json } = await client.request<JsonOf<InvoiceDetailed[]>>({
    url: uri`/employee/invoices/head-of-family/${request.id}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonInvoiceDetailed(e))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.getInvoice
*/
export async function getInvoice(
  request: {
    id: InvoiceId
  }
): Promise<InvoiceDetailedResponse> {
  const { data: json } = await client.request<JsonOf<InvoiceDetailedResponse>>({
    url: uri`/employee/invoices/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonInvoiceDetailedResponse(json)
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.getInvoiceCodes
*/
export async function getInvoiceCodes(): Promise<InvoiceCodes> {
  const { data: json } = await client.request<JsonOf<InvoiceCodes>>({
    url: uri`/employee/invoices/codes`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.markInvoicesSent
*/
export async function markInvoicesSent(
  request: {
    body: InvoiceId[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoices/mark-sent`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<InvoiceId[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.markReplacementDraftSent
*/
export async function markReplacementDraftSent(
  request: {
    invoiceId: InvoiceId,
    body: MarkReplacementDraftSentRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoices/${request.invoiceId}/mark-replacement-draft-sent`.toString(),
    method: 'POST',
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
  }
): Promise<PagedInvoiceSummaryResponses> {
  const { data: json } = await client.request<JsonOf<PagedInvoiceSummaryResponses>>({
    url: uri`/employee/invoices/search`.toString(),
    method: 'POST',
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
    body: InvoiceId[]
  }
): Promise<void> {
  const params = createUrlSearchParams(
    ['invoiceDate', request.invoiceDate?.formatIso()],
    ['dueDate', request.dueDate?.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoices/send`.toString(),
    method: 'POST',
    params,
    data: request.body satisfies JsonCompatible<InvoiceId[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.sendInvoicesByDate
*/
export async function sendInvoicesByDate(
  request: {
    body: InvoicePayload
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoices/send/by-date`.toString(),
    method: 'POST',
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
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoice-corrections`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<InvoiceCorrectionInsert>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceCorrectionsController.deleteInvoiceCorrection
*/
export async function deleteInvoiceCorrection(
  request: {
    id: InvoiceCorrectionId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoice-corrections/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceCorrectionsController.getPersonInvoiceCorrections
*/
export async function getPersonInvoiceCorrections(
  request: {
    personId: PersonId
  }
): Promise<InvoiceCorrectionWithPermittedActions[]> {
  const { data: json } = await client.request<JsonOf<InvoiceCorrectionWithPermittedActions[]>>({
    url: uri`/employee/invoice-corrections/${request.personId}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonInvoiceCorrectionWithPermittedActions(e))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceCorrectionsController.updateInvoiceCorrectionNote
*/
export async function updateInvoiceCorrectionNote(
  request: {
    id: InvoiceCorrectionId,
    body: NoteUpdateBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/invoice-corrections/${request.id}/note`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<NoteUpdateBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.PaymentController.confirmDraftPayments
*/
export async function confirmDraftPayments(
  request: {
    body: PaymentId[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/payments/confirm`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PaymentId[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.PaymentController.createPaymentDrafts
*/
export async function createPaymentDrafts(): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/payments/create-drafts`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.PaymentController.deleteDraftPayments
*/
export async function deleteDraftPayments(
  request: {
    body: PaymentId[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/payments/delete-drafts`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PaymentId[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.PaymentController.revertPaymentsToDrafts
*/
export async function revertPaymentsToDrafts(
  request: {
    body: PaymentId[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/payments/revert-to-draft`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PaymentId[]>
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
    url: uri`/employee/payments/search`.toString(),
    method: 'POST',
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
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/payments/send`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<SendPaymentsRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController.generateRetroactiveVoucherValueDecisions
*/
export async function generateRetroactiveVoucherValueDecisions(
  request: {
    id: PersonId,
    body: CreateRetroactiveFeeDecisionsBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/value-decisions/head-of-family/${request.id}/create-retroactive`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CreateRetroactiveFeeDecisionsBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController.getHeadOfFamilyVoucherValueDecisions
*/
export async function getHeadOfFamilyVoucherValueDecisions(
  request: {
    headOfFamilyId: PersonId
  }
): Promise<VoucherValueDecisionSummary[]> {
  const { data: json } = await client.request<JsonOf<VoucherValueDecisionSummary[]>>({
    url: uri`/employee/value-decisions/head-of-family/${request.headOfFamilyId}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonVoucherValueDecisionSummary(e))
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController.getVoucherValueDecision
*/
export async function getVoucherValueDecision(
  request: {
    id: VoucherValueDecisionId
  }
): Promise<VoucherValueDecisionResponse> {
  const { data: json } = await client.request<JsonOf<VoucherValueDecisionResponse>>({
    url: uri`/employee/value-decisions/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonVoucherValueDecisionResponse(json)
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController.ignoreVoucherValueDecisionDrafts
*/
export async function ignoreVoucherValueDecisionDrafts(
  request: {
    body: VoucherValueDecisionId[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/value-decisions/ignore`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<VoucherValueDecisionId[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController.markVoucherValueDecisionSent
*/
export async function markVoucherValueDecisionSent(
  request: {
    body: VoucherValueDecisionId[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/value-decisions/mark-sent`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<VoucherValueDecisionId[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController.searchVoucherValueDecisions
*/
export async function searchVoucherValueDecisions(
  request: {
    body: SearchVoucherValueDecisionRequest
  }
): Promise<PagedVoucherValueDecisionSummaries> {
  const { data: json } = await client.request<JsonOf<PagedVoucherValueDecisionSummaries>>({
    url: uri`/employee/value-decisions/search`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<SearchVoucherValueDecisionRequest>
  })
  return deserializeJsonPagedVoucherValueDecisionSummaries(json)
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController.sendVoucherValueDecisionDrafts
*/
export async function sendVoucherValueDecisionDrafts(
  request: {
    decisionHandlerId?: EmployeeId | null,
    body: VoucherValueDecisionId[]
  }
): Promise<void> {
  const params = createUrlSearchParams(
    ['decisionHandlerId', request.decisionHandlerId]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/value-decisions/send`.toString(),
    method: 'POST',
    params,
    data: request.body satisfies JsonCompatible<VoucherValueDecisionId[]>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController.setVoucherValueDecisionType
*/
export async function setVoucherValueDecisionType(
  request: {
    id: VoucherValueDecisionId,
    body: VoucherValueDecisionTypeRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/value-decisions/set-type/${request.id}`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<VoucherValueDecisionTypeRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionController.unignoreVoucherValueDecisionDrafts
*/
export async function unignoreVoucherValueDecisionDrafts(
  request: {
    body: VoucherValueDecisionId[]
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/value-decisions/unignore`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<VoucherValueDecisionId[]>
  })
  return json
}
