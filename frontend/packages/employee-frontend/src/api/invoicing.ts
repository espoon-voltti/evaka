// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from '@evaka/lib-common/src/json'
import LocalDate from '@evaka/lib-common/src/local-date'
import { Failure, Response, Result, Success } from '@evaka/lib-common/src/api'
import { API_URL, client } from '../api/client'
import { SearchOrder, UUID } from '../types'
import { Absence, deserializeAbsence } from '../types/absence'
import { deserializeIncome } from '../types/income'
import {
  deserializePeriodic,
  deserializePersonBasic,
  deserializePersonDetailed,
  FeeDecision,
  FeeDecisionDetailed,
  FeeDecisionSummary,
  VoucherValueDecisionSummary,
  VoucherValueDecisionDetailed,
  Invoice,
  InvoiceCodes,
  InvoiceDetailed,
  InvoiceSearchResult
} from '../types/invoicing'

export interface SearchParams {
  status?: string
  area?: string
  unit?: string
  distinctions?: string
  searchTerms?: string
}

export interface FeeDecisionSearchParams extends SearchParams {
  startDate?: string
  endDate?: string
  searchByStartDate: boolean
  financeDecisionHandlerId?: string
}

export interface VoucherValueDecisionSearchParams {
  status?: string
  area?: string
  unit?: string
  searchTerms?: string
  financeDecisionHandlerId?: string
}

export interface InvoiceSearchParams extends SearchParams {
  periodStart?: string
  periodEnd?: string
}

interface AbsenceParams {
  year: number
  month: number
}

export async function confirmFeeDecisions(
  feeDecisionIds: string[]
): Promise<void> {
  return client
    .post<void>('/fee-decisions/confirm', feeDecisionIds)
    .then((res) => res.data)
}

export async function createInvoices(): Promise<void> {
  return client.post<void>('/invoices/create-drafts').then((res) => res.data)
}

export async function deleteInvoices(invoiceIds: string[]): Promise<void> {
  return client
    .post<void>('/invoices/delete-drafts', invoiceIds)
    .then((res) => res.data)
}

export async function sendInvoices(
  invoiceIds: string[],
  invoiceDate: LocalDate,
  dueDate: LocalDate
): Promise<void> {
  return client
    .post<void>('/invoices/send', invoiceIds, {
      params: {
        invoiceDate: invoiceDate.formatIso(),
        dueDate: dueDate.formatIso()
      }
    })
    .then((res) => res.data)
}

export async function sendInvoicesByDate(
  invoiceDate: LocalDate,
  dueDate: LocalDate,
  areas: string[],
  periodStart: LocalDate | undefined,
  periodEnd: LocalDate | undefined,
  useCustomDatesForInvoiceSending: boolean
): Promise<void> {
  return client
    .post<void>('/invoices/send/by-date', {
      from:
        periodStart && useCustomDatesForInvoiceSending
          ? periodStart
          : LocalDate.today().withDate(1),
      to:
        periodEnd && useCustomDatesForInvoiceSending
          ? periodEnd
          : LocalDate.today().lastDayOfMonth(),
      areas,
      invoiceDate: invoiceDate,
      dueDate: dueDate
    })
    .then((res) => res.data)
}

export async function markInvoiceSent(invoiceIds: string[]): Promise<void> {
  const request = await client.post<void>('/invoices/mark-sent', invoiceIds)
  return request.data
}

export async function getFeeDecision(
  id: string
): Promise<Result<FeeDecisionDetailed>> {
  return client
    .get<JsonOf<Response<FeeDecisionDetailed>>>(`/fee-decisions/${id}`)
    .then(({ data: { data: json } }) =>
      Success.of({
        ...json,
        validFrom: LocalDate.parseIso(json.validFrom),
        validTo: LocalDate.parseNullableIso(json.validTo),
        headOfFamily: deserializePersonDetailed(json.headOfFamily),
        partner: json.partner ? deserializePersonDetailed(json.partner) : null,
        headOfFamilyIncome: json.headOfFamilyIncome
          ? deserializeIncome(json.headOfFamilyIncome)
          : null,
        partnerIncome: json.partnerIncome
          ? deserializeIncome(json.partnerIncome)
          : null,
        parts: json.parts.map((partJson) => ({
          ...partJson,
          child: deserializePersonDetailed(partJson.child)
        })),
        createdAt: new Date(json.createdAt),
        sentAt: json.sentAt ? new Date(json.sentAt) : null,
        financeDecisionHandlerName: json.financeDecisionHandlerName
          ? json.financeDecisionHandlerName
          : null,
        approvedAt: json.approvedAt ? new Date(json.approvedAt) : null
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
        headOfFamilyIncome: json.headOfFamilyIncome
          ? deserializeIncome(json.headOfFamilyIncome)
          : null,
        partnerIncome: json.partnerIncome
          ? deserializeIncome(json.partnerIncome)
          : null,
        parts: json.parts.map((partJson) => ({
          ...partJson,
          child: deserializePersonDetailed(partJson.child)
        })),
        createdAt: new Date(json.createdAt),
        sentAt: json.sentAt ? new Date(json.sentAt) : null,
        approvedAt: json.approvedAt ? new Date(json.approvedAt) : null
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function sendVoucherValueDecisions(ids: string[]): Promise<void> {
  return client.post<void>('/value-decisions/send', ids).then((res) => res.data)
}

export type FeeDecisionSearchResponse = {
  data: FeeDecisionSummary[]
  page: number
  total: number
  pages: number
}

export type SortByFeeDecisions =
  | 'HEAD_OF_FAMILY'
  | 'VALIDITY'
  | 'NUMBER'
  | 'CREATED'
  | 'SENT'
  | 'STATUS'
  | 'FINAL_PRICE'

export type VoucherValueDecisionSearchResponse = {
  data: VoucherValueDecisionSummary[]
  page: number
  total: number
  pages: number
}

export type SortByVoucherValueDecisions = 'HEAD_OF_FAMILY' | 'STATUS'

export type SortByInvoices =
  | 'HEAD_OF_FAMILY'
  | 'CHILDREN'
  | 'START'
  | 'END'
  | 'SUM'
  | 'STATUS'

export async function getFeeDecisions(
  page: number,
  pageSize: number,
  sortBy: SortByFeeDecisions,
  sortDirection: SearchOrder,
  params: FeeDecisionSearchParams
): Promise<Result<FeeDecisionSearchResponse>> {
  return client
    .get<JsonOf<FeeDecisionSearchResponse>>('/fee-decisions/search', {
      params: { page: page - 1, pageSize, sortBy, sortDirection, ...params }
    })
    .then(({ data }) => ({
      ...data,
      data: data.data.map((json) => ({
        ...json,
        validFrom: LocalDate.parseIso(json.validFrom),
        validTo: LocalDate.parseNullableIso(json.validTo),
        headOfFamily: deserializePersonBasic(json.headOfFamily),
        parts: json.parts.map((partJson) => ({
          child: {
            ...partJson.child,
            dateOfBirth: LocalDate.parseIso(partJson.child.dateOfBirth)
          }
        })),
        createdAt: new Date(json.createdAt),
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
          validFrom: LocalDate.parseIso(json.validFrom),
          validTo: LocalDate.parseNullableIso(json.validTo),
          createdAt: new Date(json.createdAt),
          sentAt: json.sentAt ? new Date(json.sentAt) : null
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function getVoucherValueDecisions(
  page: number,
  pageSize: number,
  sortBy: SortByVoucherValueDecisions,
  sortDirection: SearchOrder,
  params: VoucherValueDecisionSearchParams
): Promise<Result<VoucherValueDecisionSearchResponse>> {
  return client
    .get<JsonOf<VoucherValueDecisionSearchResponse>>(
      '/value-decisions/search',
      {
        params: { page: page - 1, pageSize, sortBy, sortDirection, ...params }
      }
    )
    .then(({ data }) => ({
      ...data,
      data: data.data.map((json) => ({
        ...json,
        validFrom: LocalDate.parseIso(json.validFrom),
        validTo: LocalDate.parseNullableIso(json.validTo),
        headOfFamily: deserializePersonBasic(json.headOfFamily),
        parts: json.parts.map((partJson) => ({
          child: {
            ...partJson.child,
            dateOfBirth: LocalDate.parseIso(partJson.child.dateOfBirth)
          }
        })),
        createdAt: new Date(json.createdAt),
        sentAt: json.sentAt ? new Date(json.sentAt) : null,
        approvedAt: json.approvedAt ? new Date(json.approvedAt) : null
      }))
    }))
    .then((v) => Success.of(v))
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
          ...deserializePeriodic(json),
          sentAt: json.sentAt ? new Date(json.sentAt) : null
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
      ...deserializePeriodic(json),
      dueDate: LocalDate.parseIso(json.dueDate),
      invoiceDate: LocalDate.parseIso(json.invoiceDate),
      headOfFamily: deserializePersonDetailed(json.headOfFamily),
      rows: json.rows.map((rowJson) => ({
        ...rowJson,
        ...deserializePeriodic(rowJson),
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
  sortBy: SortByInvoices,
  sortDirection: SearchOrder,
  params: InvoiceSearchParams
): Promise<Result<InvoiceSearchResult>> {
  return client
    .get<JsonOf<InvoiceSearchResult>>('/invoices/search', {
      params: { page, pageSize, sortBy, sortDirection, ...params }
    })
    .then(({ data }) => ({
      ...data,
      data: data.data.map((json) => ({
        ...json,
        ...deserializePeriodic(json),
        headOfFamily: deserializePersonDetailed(json.headOfFamily),
        rows: json.rows.map((rowJson) => ({
          child: {
            ...rowJson.child,
            dateOfBirth: LocalDate.parseIso(rowJson.child.dateOfBirth)
          }
        }))
      }))
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function updateInvoice(invoice: InvoiceDetailed): Promise<void> {
  return client
    .put<void>(`/invoices/${invoice.id}`, invoice)
    .then((res) => res.data)
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

export async function markFeeDecisionSent(ids: string[]): Promise<void> {
  const request = await client.post<void>('/fee-decisions/mark-sent', ids)
  return request.data
}

export async function markVoucherValueDecisionSent(
  ids: string[]
): Promise<void> {
  const request = await client.post<void>('/value-decisions/mark-sent', ids)
  return request.data
}

export function getFeeDecisionPdfUrl(decisionId: string): string {
  return `${API_URL}/fee-decisions/pdf/${decisionId}`
}

export function getVoucherValueDecisionPdfUrl(decisionId: string): string {
  return `${API_URL}/value-decisions/pdf/${decisionId}`
}

export async function setDecisionType(
  decisionId: string,
  type: string
): Promise<void> {
  const request = await client.post<void>(
    `/fee-decisions/set-type/${decisionId}`,
    {
      type: type
    }
  )
  return request.data
}

export async function getAbsencesByChild(
  childId: UUID,
  params: AbsenceParams
): Promise<Result<Record<string, Absence[]>>> {
  return client
    .get<JsonOf<Response<{ absences: Record<string, Absence[]> }>>>(
      `/absences/by-child/${childId}`,
      {
        params
      }
    )
    .then((res) =>
      Success.of(
        Object.fromEntries(
          Object.entries(res.data.data.absences).map(([key, absences]) => [
            key,
            absences.map(deserializeAbsence)
          ])
        )
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function createRetroactiveDecisions(
  headOfFamily: UUID,
  date: LocalDate
): Promise<void> {
  await client.post(
    `/fee-decisions/head-of-family/${headOfFamily}/create-retroactive`,
    {
      from: date
    }
  )
  return void undefined
}
