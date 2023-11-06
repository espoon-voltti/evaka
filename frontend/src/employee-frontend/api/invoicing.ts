// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Response, Result, Success } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import { Absence } from 'lib-common/generated/api-types/daycare'
import {
  DistinctiveParams,
  FeeDecision,
  FeeDecisionDetailed,
  FeeDecisionDifference,
  FeeDecisionSortParam,
  FeeDecisionStatus,
  Invoice,
  InvoiceCodes,
  InvoiceCorrectionWithPermittedActions,
  InvoiceDetailed,
  InvoiceDetailedResponse,
  InvoiceDistinctiveParams,
  InvoiceSortParam,
  InvoiceStatus,
  NewInvoiceCorrection,
  PagedFeeDecisionSummaries,
  PagedInvoiceSummaryResponses,
  PagedPayments,
  PagedVoucherValueDecisionSummaries,
  PersonBasic,
  PersonDetailed,
  SearchPaymentsRequest,
  SortDirection,
  VoucherValueDecisionDetailed,
  VoucherValueDecisionDifference,
  VoucherValueDecisionDistinctiveParams,
  VoucherValueDecisionSortParam,
  VoucherValueDecisionStatus,
  VoucherValueDecisionSummary
} from 'lib-common/generated/api-types/invoicing'
import { Employee } from 'lib-common/generated/api-types/pis'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { API_URL, client } from './client'

export interface SearchParams {
  area?: string[]
  unit?: UUID
  searchTerms?: string
}

export interface FeeDecisionSearchParams extends SearchParams {
  statuses: FeeDecisionStatus[]
  distinctions?: DistinctiveParams[]
  startDate?: LocalDate
  endDate?: LocalDate
  searchByStartDate: boolean
  financeDecisionHandlerId?: UUID
  difference: FeeDecisionDifference[]
}

export interface VoucherValueDecisionSearchParams extends SearchParams {
  statuses: VoucherValueDecisionStatus[]
  financeDecisionHandlerId?: UUID
  startDate?: LocalDate
  endDate?: LocalDate
  searchByStartDate: boolean
  distinctions?: VoucherValueDecisionDistinctiveParams[]
  difference: VoucherValueDecisionDifference[]
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

export async function getSelectableFinanceDecisionHandlers(): Promise<
  Result<Employee[]>
> {
  return client
    .get<JsonOf<Employee[]>>('/finance-decisions/selectable-handlers')
    .then((res) =>
      Success.of(
        res.data.map((data) => ({
          ...data,
          created: HelsinkiDateTime.parseIso(data.created),
          updated:
            data.updated !== null
              ? HelsinkiDateTime.parseIso(data.updated)
              : null
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function confirmFeeDecisions(
  feeDecisionIds: string[],
  decisionHandlerId?: UUID
): Promise<Result<void>> {
  return client
    .post<void>('/fee-decisions/confirm', feeDecisionIds, {
      params: { decisionHandlerId }
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function ignoreFeeDecisionDrafts(
  feeDecisionIds: UUID[]
): Promise<Result<void>> {
  return client
    .post<void>('/fee-decisions/ignore', feeDecisionIds)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function unignoreFeeDecisionDrafts(
  feeDecisionIds: UUID[]
): Promise<Result<void>> {
  return client
    .post<void>('/fee-decisions/unignore', feeDecisionIds)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function ignoreVoucherValueDecisionDrafts(
  voucherValueDecisionIds: UUID[]
): Promise<Result<void>> {
  return client
    .post<void>('/value-decisions/ignore', voucherValueDecisionIds)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function unignoreVoucherValueDecisionDrafts(
  voucherValueDecisionIds: UUID[]
): Promise<Result<void>> {
  return client
    .post<void>('/value-decisions/unignore', voucherValueDecisionIds)
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
        sentAt: json.sentAt ? HelsinkiDateTime.parseIso(json.sentAt) : null,
        financeDecisionHandlerFirstName: json.financeDecisionHandlerFirstName
          ? json.financeDecisionHandlerFirstName
          : null,
        financeDecisionHandlerLastName: json.financeDecisionHandlerLastName
          ? json.financeDecisionHandlerLastName
          : null,
        approvedAt: json.approvedAt
          ? HelsinkiDateTime.parseIso(json.approvedAt)
          : null,
        created: HelsinkiDateTime.parseIso(json.created)
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
        sentAt: json.sentAt ? HelsinkiDateTime.parseIso(json.sentAt) : null,
        approvedAt: json.approvedAt
          ? HelsinkiDateTime.parseIso(json.approvedAt)
          : null,
        created: HelsinkiDateTime.parseIso(json.created)
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function sendVoucherValueDecisions(
  ids: string[],
  decisionHandlerId?: UUID
): Promise<Result<void>> {
  return client
    .post<void>('/value-decisions/send', ids, { params: { decisionHandlerId } })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getFeeDecisions(
  page: number,
  pageSize: number,
  sortBy: FeeDecisionSortParam,
  sortDirection: SortDirection,
  params: FeeDecisionSearchParams
): Promise<Result<PagedFeeDecisionSummaries>> {
  return client
    .post<JsonOf<PagedFeeDecisionSummaries>>('/fee-decisions/search', {
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
        created: HelsinkiDateTime.parseIso(json.created),
        sentAt: json.sentAt ? HelsinkiDateTime.parseIso(json.sentAt) : null,
        approvedAt: json.approvedAt
          ? HelsinkiDateTime.parseIso(json.approvedAt)
          : null
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
          sentAt: json.sentAt ? HelsinkiDateTime.parseIso(json.sentAt) : null,
          approvedAt: json.approvedAt
            ? HelsinkiDateTime.parseIso(json.approvedAt)
            : null,
          created: HelsinkiDateTime.parseIso(json.created),
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
): Promise<Result<PagedVoucherValueDecisionSummaries>> {
  return client
    .post<JsonOf<PagedVoucherValueDecisionSummaries>>(
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
  sentAt: json.sentAt ? HelsinkiDateTime.parseIso(json.sentAt) : null,
  approvedAt: json.approvedAt
    ? HelsinkiDateTime.parseIso(json.approvedAt)
    : null,
  created: HelsinkiDateTime.parseIso(json.created)
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
          sentAt: json.sentAt ? HelsinkiDateTime.parseIso(json.sentAt) : null,
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

export async function getInvoice(
  id: string
): Promise<Result<InvoiceDetailedResponse>> {
  return client
    .get<JsonOf<Response<InvoiceDetailedResponse>>>(`/invoices/${id}`)
    .then(
      ({
        data: {
          data: { data: json, ...rest }
        }
      }) => ({
        data: {
          ...json,
          periodStart: LocalDate.parseIso(json.periodStart),
          periodEnd: LocalDate.parseIso(json.periodEnd),
          dueDate: LocalDate.parseIso(json.dueDate),
          invoiceDate: LocalDate.parseIso(json.invoiceDate),
          headOfFamily: deserializePersonDetailed(json.headOfFamily),
          codebtor: json.codebtor
            ? deserializePersonDetailed(json.codebtor)
            : null,
          rows: json.rows.map((rowJson) => ({
            ...rowJson,
            periodStart: LocalDate.parseIso(rowJson.periodStart),
            periodEnd: LocalDate.parseIso(rowJson.periodEnd),
            child: deserializePersonDetailed(rowJson.child)
          })),
          sentAt: json.sentAt ? HelsinkiDateTime.parseIso(json.sentAt) : null
        },
        ...rest
      })
    )
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getInvoices(
  page: number,
  pageSize: number,
  sortBy: InvoiceSortParam,
  sortDirection: SortDirection,
  params: InvoiceSearchParams
): Promise<Result<PagedInvoiceSummaryResponses>> {
  return client
    .post<JsonOf<PagedInvoiceSummaryResponses>>('/invoices/search', {
      page,
      pageSize,
      sortBy,
      sortDirection,
      ...params
    })
    .then(({ data }) => ({
      ...data,
      data: data.data.map(({ data: json, permittedActions }) => ({
        data: {
          ...json,
          periodStart: LocalDate.parseIso(json.periodStart),
          periodEnd: LocalDate.parseIso(json.periodEnd),
          headOfFamily: deserializePersonDetailed(json.headOfFamily),
          codebtor: json.codebtor
            ? deserializePersonDetailed(json.codebtor)
            : null,
          createdAt: json.createdAt
            ? HelsinkiDateTime.parseIso(json.createdAt)
            : null,
          sentAt: json.sentAt ? HelsinkiDateTime.parseIso(json.sentAt) : null,
          rows: json.rows.map((rowJson) => ({
            ...rowJson,
            child: deserializePersonBasic(rowJson.child)
          }))
        },
        permittedActions
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
): Promise<Result<InvoiceCorrectionWithPermittedActions[]>> {
  return client
    .get<JsonOf<InvoiceCorrectionWithPermittedActions[]>>(
      `/invoice-corrections/${personId}`
    )
    .then(({ data }) =>
      data.map(({ data: json, permittedActions }) => ({
        data: {
          ...json,
          period: FiniteDateRange.parseJson(json.period)
        },
        permittedActions
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
): Promise<Result<PagedPayments>> {
  return client
    .post<JsonOf<PagedPayments>>('/payments/search', {
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
  paymentIds: string[],
  paymentDate: LocalDate,
  dueDate: LocalDate
): Promise<Result<void>> {
  return client
    .post<void>('/payments/send', { paymentDate, dueDate, paymentIds })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
