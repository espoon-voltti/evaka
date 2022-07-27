// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Paged, Response, Result } from 'lib-common/api'
import { Failure, Success } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import type { Absence } from 'lib-common/generated/api-types/daycare'
import type {
  DistinctiveParams,
  FeeDecision,
  FeeDecisionDetailed,
  FeeDecisionSortParam,
  FeeDecisionStatus,
  FeeDecisionSummary,
  Invoice,
  InvoiceCodes,
  InvoiceCorrection,
  InvoiceDetailed,
  InvoiceDistinctiveParams,
  InvoiceSortParam,
  InvoiceStatus,
  InvoiceSummary,
  NewInvoiceCorrection,
  Payment,
  PersonBasic,
  PersonDetailed,
  SearchPaymentsRequest,
  SortDirection,
  VoucherValueDecisionDetailed,
  VoucherValueDecisionSortParam,
  VoucherValueDecisionStatus,
  VoucherValueDecisionSummary
} from 'lib-common/generated/api-types/invoicing'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

import { API_URL, client } from './client'

export interface SearchParams {
  area?: string[]
  unit?: UUID
  searchTerms?: string
}

export interface FeeDecisionSearchParams extends SearchParams {
  status?: FeeDecisionStatus[]
  distinctions?: DistinctiveParams[]
  startDate?: LocalDate
  endDate?: LocalDate
  searchByStartDate: boolean
  financeDecisionHandlerId?: UUID
}

export interface VoucherValueDecisionSearchParams extends SearchParams {
  status?: VoucherValueDecisionStatus
  financeDecisionHandlerId?: UUID
  startDate?: LocalDate
  endDate?: LocalDate
  searchByStartDate: boolean
}

export interface InvoiceSearchParams extends SearchParams {
  status?: InvoiceStatus[]
  distinctions?: InvoiceDistinctiveParams[]
  periodStart?: LocalDate
  periodEnd?: LocalDate
}

interface AbsenceParams {
  year: number
  month: number
}

export async function confirmFeeDecisions(
  feeDecisionIds: string[]
): Promise<Result<void>> {
  return client
    .post<void>('/fee-decisions/confirm', feeDecisionIds)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function createInvoices(): Promise<Result<void>> {
  return client
    .post<void>('/invoices/create-drafts')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function deleteInvoices(
  invoiceIds: string[]
): Promise<Result<void>> {
  return client
    .post<void>('/invoices/delete-drafts', invoiceIds)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function sendInvoices(
  invoiceIds: string[],
  invoiceDate: LocalDate,
  dueDate: LocalDate
): Promise<Result<void>> {
  return client
    .post<void>('/invoices/send', invoiceIds, {
      params: {
        invoiceDate: invoiceDate.formatIso(),
        dueDate: dueDate.formatIso()
      }
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function sendInvoicesByDate(
  invoiceDate: LocalDate,
  dueDate: LocalDate,
  areas: string[],
  periodStart: LocalDate | undefined,
  periodEnd: LocalDate | undefined,
  useCustomDatesForInvoiceSending: boolean
): Promise<Result<void>> {
  return client
    .post<void>('/invoices/send/by-date', {
      from:
        periodStart && useCustomDatesForInvoiceSending
          ? periodStart
          : LocalDate.todayInSystemTz().withDate(1),
      to:
        periodEnd && useCustomDatesForInvoiceSending
          ? periodEnd
          : LocalDate.todayInSystemTz().lastDayOfMonth(),
      areas,
      invoiceDate: invoiceDate,
      dueDate: dueDate
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function markInvoiceSent(
  invoiceIds: string[]
): Promise<Result<void>> {
  return client
    .post<void>('/invoices/mark-sent', invoiceIds)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getFeeDecision(
  id: string
): Promise<Result<FeeDecisionDetailed>> {
  return client
    .get<JsonOf<Response<FeeDecisionDetailed>>>(`/fee-decisions/${id}`)
    .then(({ data: { data: json } }) =>
      Success.of({
        ...json,
        validDuring: DateRange.parseJson(json.validDuring),
        headOfFamily: deserializePersonDetailed(json.headOfFamily),
        partner: json.partner ? deserializePersonDetailed(json.partner) : null,
        children: json.children.map((childJson) => ({
          ...childJson,
          child: deserializePersonDetailed(childJson.child)
        })),
        sentAt: json.sentAt ? new Date(json.sentAt) : null,
        financeDecisionHandlerFirstName: json.financeDecisionHandlerFirstName
          ? json.financeDecisionHandlerFirstName
          : null,
        financeDecisionHandlerLastName: json.financeDecisionHandlerLastName
          ? json.financeDecisionHandlerLastName
          : null,
        approvedAt: json.approvedAt ? new Date(json.approvedAt) : null,
        created: new Date(json.created)
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function getVoucherValueDecision(
  id: string
): Promise<Result<VoucherValueDecisionDetailed>> {
  return client
    .get<JsonOf<Response<VoucherValueDecisionDetailed>>>(
      `/value-decisions/${id}`
    )
    .then(({ data: { data: json } }) =>
      Success.of({
        ...json,
        validFrom: LocalDate.parseIso(json.validFrom),
        validTo: LocalDate.parseNullableIso(json.validTo),
        headOfFamily: deserializePersonDetailed(json.headOfFamily),
        partner: json.partner ? deserializePersonDetailed(json.partner) : null,
        child: deserializePersonDetailed(json.child),
        sentAt: json.sentAt ? new Date(json.sentAt) : null,
        approvedAt: json.approvedAt ? new Date(json.approvedAt) : null,
        created: new Date(json.created)
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function sendVoucherValueDecisions(
  ids: string[]
): Promise<Result<void>> {
  return client
    .post<void>('/value-decisions/send', ids)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getFeeDecisions(
  page: number,
  pageSize: number,
  sortBy: FeeDecisionSortParam,
  sortDirection: SortDirection,
  params: FeeDecisionSearchParams
): Promise<Result<Paged<FeeDecisionSummary>>> {
  return client
    .post<JsonOf<Paged<FeeDecisionSummary>>>('/fee-decisions/search', {
      page: page - 1,
      pageSize,
      sortBy,
      sortDirection,
      ...params
    })
    .then(({ data }) => ({
      ...data,
      data: data.data.map((json) => ({
        ...json,
        validDuring: DateRange.parseJson(json.validDuring),
        headOfFamily: deserializePersonBasic(json.headOfFamily),
        children: json.children.map((childJson) => ({
          ...childJson,
          dateOfBirth: LocalDate.parseIso(childJson.dateOfBirth)
        })),
        created: new Date(json.created),
        sentAt: json.sentAt ? new Date(json.sentAt) : null,
        approvedAt: json.approvedAt ? new Date(json.approvedAt) : null
      }))
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getPersonFeeDecisions(
  id: string
): Promise<Result<FeeDecision[]>> {
  return client
    .get<JsonOf<Response<FeeDecision[]>>>(`/fee-decisions/head-of-family/${id}`)
    .then((res) =>
      Success.of(
        res.data.data.map((json) => ({
          ...json,
          validDuring: DateRange.parseJson(json.validDuring),
          validFrom: LocalDate.parseIso(json.validFrom),
          validTo: LocalDate.parseNullableIso(json.validTo),
          sentAt: json.sentAt ? new Date(json.sentAt) : null,
          approvedAt: json.approvedAt ? new Date(json.approvedAt) : null,
          created: new Date(json.created),
          children: json.children.map((childJson) => ({
            ...childJson,
            child: {
              ...childJson.child,
              dateOfBirth: LocalDate.parseIso(childJson.child.dateOfBirth)
            }
          }))
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function getVoucherValueDecisions(
  page: number,
  pageSize: number,
  sortBy: VoucherValueDecisionSortParam,
  sortDirection: SortDirection,
  params: VoucherValueDecisionSearchParams
): Promise<Result<Paged<VoucherValueDecisionSummary>>> {
  return client
    .post<JsonOf<Paged<VoucherValueDecisionSummary>>>(
      '/value-decisions/search',
      {
        page: page - 1,
        pageSize,
        sortBy,
        sortDirection,
        ...params
      }
    )
    .then(({ data }) => ({
      ...data,
      data: data.data.map(parseVoucherValueDecisionSummaryJson)
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

const parseVoucherValueDecisionSummaryJson = (
  json: JsonOf<VoucherValueDecisionSummary>
) => ({
  ...json,
  validFrom: LocalDate.parseIso(json.validFrom),
  validTo: LocalDate.parseNullableIso(json.validTo),
  headOfFamily: deserializePersonBasic(json.headOfFamily),
  child: {
    ...json.child,
    dateOfBirth: LocalDate.parseIso(json.child.dateOfBirth)
  },
  sentAt: json.sentAt ? new Date(json.sentAt) : null,
  approvedAt: json.approvedAt ? new Date(json.approvedAt) : null,
  created: new Date(json.created)
})

export async function getPersonVoucherValueDecisions(
  id: string
): Promise<Result<VoucherValueDecisionSummary[]>> {
  return client
    .get<JsonOf<VoucherValueDecisionSummary[]>>(
      `/value-decisions/head-of-family/${id}`
    )
    .then((res) =>
      Success.of(res.data.map(parseVoucherValueDecisionSummaryJson))
    )
    .catch((e) => Failure.fromError(e))
}

export async function getPersonInvoices(
  id: string
): Promise<Result<Invoice[]>> {
  return client
    .get<JsonOf<Response<Invoice[]>>>(`/invoices/head-of-family/${id}`)
    .then((res) =>
      Success.of(
        res.data.data.map((json) => ({
          ...json,
          periodStart: LocalDate.parseIso(json.periodStart),
          periodEnd: LocalDate.parseIso(json.periodEnd),
          dueDate: LocalDate.parseIso(json.dueDate),
          invoiceDate: LocalDate.parseIso(json.invoiceDate),
          sentAt: json.sentAt ? new Date(json.sentAt) : null,
          rows: json.rows.map((rowJson) => ({
            ...rowJson,
            periodStart: LocalDate.parseIso(rowJson.periodStart),
            periodEnd: LocalDate.parseIso(rowJson.periodEnd)
          }))
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function getInvoice(id: string): Promise<Result<InvoiceDetailed>> {
  return client
    .get<JsonOf<Response<InvoiceDetailed>>>(`/invoices/${id}`)
    .then(({ data: { data: json } }) => ({
      ...json,
      periodStart: LocalDate.parseIso(json.periodStart),
      periodEnd: LocalDate.parseIso(json.periodEnd),
      dueDate: LocalDate.parseIso(json.dueDate),
      invoiceDate: LocalDate.parseIso(json.invoiceDate),
      headOfFamily: deserializePersonDetailed(json.headOfFamily),
      codebtor: json.codebtor ? deserializePersonDetailed(json.codebtor) : null,
      rows: json.rows.map((rowJson) => ({
        ...rowJson,
        periodStart: LocalDate.parseIso(rowJson.periodStart),
        periodEnd: LocalDate.parseIso(rowJson.periodEnd),
        child: deserializePersonDetailed(rowJson.child)
      })),
      sentAt: json.sentAt ? new Date(json.sentAt) : null
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getInvoices(
  page: number,
  pageSize: number,
  sortBy: InvoiceSortParam,
  sortDirection: SortDirection,
  params: InvoiceSearchParams
): Promise<Result<Paged<InvoiceSummary>>> {
  return client
    .post<JsonOf<Paged<InvoiceSummary>>>('/invoices/search', {
      page,
      pageSize,
      sortBy,
      sortDirection,
      ...params
    })
    .then(({ data }) => ({
      ...data,
      data: data.data.map((json) => ({
        ...json,
        periodStart: LocalDate.parseIso(json.periodStart),
        periodEnd: LocalDate.parseIso(json.periodEnd),
        headOfFamily: deserializePersonDetailed(json.headOfFamily),
        codebtor: json.codebtor
          ? deserializePersonDetailed(json.codebtor)
          : null,
        createdAt: json.createdAt ? new Date(json.createdAt) : null,
        sentAt: json.sentAt ? new Date(json.sentAt) : null,
        rows: json.rows.map((rowJson) => ({
          ...rowJson,
          child: deserializePersonBasic(rowJson.child)
        }))
      }))
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function updateInvoice(
  invoice: InvoiceDetailed
): Promise<Result<void>> {
  const body: Invoice = {
    ...invoice,
    headOfFamily: invoice.headOfFamily.id,
    codebtor: invoice.codebtor?.id ?? null,
    rows: invoice.rows.map((row) => ({
      ...row,
      child: row.child.id
    }))
  }
  return client
    .put<void>(`/invoices/${invoice.id}`, body)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getInvoiceCodes(): Promise<Result<InvoiceCodes>> {
  return client
    .get<JsonOf<Response<InvoiceCodes>>>('/invoices/codes')
    .then((res) => Success.of(res.data.data))
    .catch((e) => Failure.fromError(e))
}

export async function generateFeeDecisions(
  starting: string,
  targetHeads: string[]
): Promise<void> {
  return client.post(`/fee-decision-generator/generate`, {
    starting,
    targetHeads
  })
}

export async function markFeeDecisionSent(
  ids: string[]
): Promise<Result<void>> {
  return client
    .post<void>('/fee-decisions/mark-sent', ids)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function markVoucherValueDecisionSent(
  ids: string[]
): Promise<Result<void>> {
  return client
    .post<void>('/value-decisions/mark-sent', ids)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export function getFeeDecisionPdfUrl(decisionId: string): string {
  return `${API_URL}/fee-decisions/pdf/${decisionId}`
}

export function getVoucherValueDecisionPdfUrl(decisionId: string): string {
  return `${API_URL}/value-decisions/pdf/${decisionId}`
}

export async function setFeeDecisionType(
  decisionId: string,
  type: string
): Promise<Result<void>> {
  return client
    .post<void>(`/fee-decisions/set-type/${decisionId}`, {
      type: type
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function setVoucherDecisionType(
  decisionId: string,
  type: string
): Promise<Result<void>> {
  return client
    .post<void>(`/value-decisions/set-type/${decisionId}`, {
      type: type
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getAbsencesByChild(
  childId: UUID,
  params: AbsenceParams
): Promise<Result<Absence[]>> {
  return client
    .get<JsonOf<Absence[]>>(`/absences/by-child/${childId}`, {
      params
    })
    .then((res) =>
      Success.of(
        res.data.map((absence) => ({
          ...absence,
          date: LocalDate.parseIso(absence.date)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function createRetroactiveFeeDecisions(
  headOfFamily: UUID,
  date: LocalDate
): Promise<Result<void>> {
  return client
    .post(`/fee-decisions/head-of-family/${headOfFamily}/create-retroactive`, {
      from: date
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function createRetroactiveValueDecisions(
  headOfFamily: UUID,
  date: LocalDate
): Promise<Result<void>> {
  return client
    .post(
      `/value-decisions/head-of-family/${headOfFamily}/create-retroactive`,
      {
        from: date
      }
    )
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getPersonInvoiceCorrections(
  personId: UUID
): Promise<Result<InvoiceCorrection[]>> {
  return client
    .get<JsonOf<InvoiceCorrection[]>>(`/invoice-corrections/${personId}`)
    .then(({ data }) =>
      data.map((json) => ({
        ...json,
        period: FiniteDateRange.parseJson(json.period)
      }))
    )
    .then((res) => Success.of(res))
    .catch((e) => Failure.fromError(e))
}

export async function createInvoiceCorrection(
  row: NewInvoiceCorrection
): Promise<Result<void>> {
  return client
    .post('/invoice-corrections', row)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function deleteInvoiceCorrection(id: UUID): Promise<Result<void>> {
  return client
    .delete(`/invoice-corrections/${id}`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function updateInvoiceCorrectionNote(
  id: UUID,
  note: string
): Promise<Result<void>> {
  return client
    .post(`/invoice-corrections/${id}/note`, { note })
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

const deserializePersonBasic = (json: JsonOf<PersonBasic>): PersonBasic => ({
  ...json,
  dateOfBirth: LocalDate.parseIso(json.dateOfBirth)
})

const deserializePersonDetailed = (
  json: JsonOf<PersonDetailed>
): PersonDetailed => ({
  ...json,
  dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
  dateOfDeath: LocalDate.parseNullableIso(json.dateOfDeath)
})

export async function createPaymentDrafts(): Promise<Result<void>> {
  return client
    .post<void>('/payments/create-drafts')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getPayments(
  params: SearchPaymentsRequest
): Promise<Result<Paged<Payment>>> {
  return client
    .post<JsonOf<Paged<Payment>>>('/payments/search', {
      ...params,
      paymentDateStart: params.paymentDateStart
        ? params.paymentDateStart.formatIso()
        : null,
      paymentDateEnd: params.paymentDateEnd
        ? params.paymentDateEnd.formatIso()
        : null
    })
    .then(({ data }) => ({
      ...data,
      data: data.data.map((json) => ({
        ...json,
        created: HelsinkiDateTime.parseIso(json.created),
        updated: HelsinkiDateTime.parseIso(json.updated),
        period: DateRange.parseJson(json.period),
        paymentDate: json.paymentDate
          ? LocalDate.parseIso(json.paymentDate)
          : null,
        dueDate: json.dueDate ? LocalDate.parseIso(json.dueDate) : null,
        sentAt: json.sentAt ? HelsinkiDateTime.parseIso(json.sentAt) : null
      }))
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function sendPayments(
  paymentsIds: string[],
  paymentDate: LocalDate,
  dueDate: LocalDate
): Promise<Result<void>> {
  return client
    .post<void>('/payments/send', paymentsIds, {
      params: {
        invoiceDate: paymentDate.formatIso(),
        dueDate: dueDate.formatIso()
      }
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
