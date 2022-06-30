// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { ProviderType } from 'lib-common/generated/api-types/daycare'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import {
  ApplicationsReportRow,
  AssistanceNeedDecisionsReportRow,
  AssistanceNeedsAndActionsReport,
  ChildAgeLanguageReportRow,
  ChildrenInDifferentAddressReportRow,
  DecisionsReportRow,
  DuplicatePeopleReportRow,
  EndedPlacementsReportRow,
  FamilyConflictReportRow,
  FamilyContactReportRow,
  InvoiceReport,
  MissingHeadOfFamilyReportRow,
  MissingServiceNeedReportRow,
  OccupancyGroupReportResultRow,
  OccupancyUnitReportResultRow,
  PartnersInDifferentAddressReportRow,
  PlacementSketchingReportRow,
  PresenceReportRow,
  RawReportRow,
  ServiceNeedReportRow,
  ServiceVoucherReport,
  ServiceVoucherUnitReport,
  SextetReportRow,
  StartingPlacementsRow,
  VardaErrorReportRow
} from 'lib-common/generated/api-types/reports'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from './client'

export interface PeriodFilters {
  from: LocalDate
  to: LocalDate
}

export async function getApplicationsReport(
  filters: PeriodFilters
): Promise<Result<ApplicationsReportRow[]>> {
  return client
    .get<JsonOf<ApplicationsReportRow[]>>('/reports/applications', {
      params: {
        from: filters.from.formatIso(),
        to: filters.to.formatIso()
      }
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getDecisionsReport(
  filters: PeriodFilters
): Promise<Result<DecisionsReportRow[]>> {
  return client
    .get<JsonOf<DecisionsReportRow[]>>('/reports/decisions', {
      params: {
        from: filters.from.formatIso(),
        to: filters.to.formatIso()
      }
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getRawReport(
  filters: PeriodFilters
): Promise<Result<RawReportRow[]>> {
  return client
    .get<JsonOf<RawReportRow[]>>('/reports/raw', {
      params: {
        from: filters.from.formatIso(),
        to: filters.to.formatIso()
      }
    })
    .then((res) =>
      Success.of(
        res.data.map((row) => ({
          ...row,
          day: LocalDate.parseIso(row.day),
          dateOfBirth: LocalDate.parseIso(row.dateOfBirth)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function getPresenceReport(
  filters: PeriodFilters
): Promise<Result<PresenceReportRow[]>> {
  return client
    .get<JsonOf<PresenceReportRow[]>>('/reports/presences', {
      params: {
        from: filters.from.formatIso(),
        to: filters.to.formatIso()
      }
    })
    .then((res) =>
      Success.of(
        res.data.map((row) => ({
          ...row,
          date: LocalDate.parseIso(row.date)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export interface MissingHeadOfFamilyReportFilters {
  startDate: LocalDate
  endDate: LocalDate | null
}

export async function getMissingHeadOfFamilyReport(
  filters: MissingHeadOfFamilyReportFilters
): Promise<Result<MissingHeadOfFamilyReportRow[]>> {
  return client
    .get<JsonOf<MissingHeadOfFamilyReportRow[]>>(
      '/reports/missing-head-of-family',
      {
        params: {
          from: filters.startDate.formatIso(),
          to: filters.endDate?.formatIso()
        }
      }
    )
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export interface MissingServiceNeedReportFilters {
  startDate: LocalDate
  endDate: LocalDate | null
}

export async function getMissingServiceNeedReport(
  filters: MissingServiceNeedReportFilters
): Promise<Result<MissingServiceNeedReportRow[]>> {
  return client
    .get<JsonOf<MissingServiceNeedReportRow[]>>(
      '/reports/missing-service-need',
      {
        params: {
          from: filters.startDate.formatIso(),
          to: filters.endDate?.formatIso()
        }
      }
    )
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getFamilyConflictsReport(): Promise<
  Result<FamilyConflictReportRow[]>
> {
  return client
    .get<JsonOf<FamilyConflictReportRow[]>>('/reports/family-conflicts')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getFamilyContactsReport(
  unitId: UUID
): Promise<Result<FamilyContactReportRow[]>> {
  return client
    .get<JsonOf<FamilyContactReportRow[]>>('/reports/family-contacts', {
      params: { unitId }
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getPartnersInDifferentAddressReport(): Promise<
  Result<PartnersInDifferentAddressReportRow[]>
> {
  return client
    .get<JsonOf<PartnersInDifferentAddressReportRow[]>>(
      '/reports/partners-in-different-address'
    )
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getChildrenInDifferentAddressReport(): Promise<
  Result<ChildrenInDifferentAddressReportRow[]>
> {
  return client
    .get<JsonOf<ChildrenInDifferentAddressReportRow[]>>(
      '/reports/children-in-different-address'
    )
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export interface DateFilters {
  date: LocalDate
}

export async function getChildAgeLanguageReport(
  filters: DateFilters
): Promise<Result<ChildAgeLanguageReportRow[]>> {
  return client
    .get<JsonOf<ChildAgeLanguageReportRow[]>>('/reports/child-age-language', {
      params: {
        date: filters.date.formatIso()
      }
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getServiceNeedReport(
  filters: DateFilters
): Promise<Result<ServiceNeedReportRow[]>> {
  return client
    .get<JsonOf<ServiceNeedReportRow[]>>('/reports/service-need', {
      params: {
        date: filters.date.formatIso()
      }
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export interface AssistanceNeedsAndActionsReportFilters {
  date: LocalDate
}

export async function getAssistanceNeedsAndActionsReport(
  filters: AssistanceNeedsAndActionsReportFilters
): Promise<Result<AssistanceNeedsAndActionsReport>> {
  return client
    .get<JsonOf<AssistanceNeedsAndActionsReport>>(
      '/reports/assistance-needs-and-actions',
      {
        params: {
          date: filters.date.formatIso()
        }
      }
    )
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export type OccupancyReportType =
  | 'UNIT_CONFIRMED'
  | 'UNIT_PLANNED'
  | 'UNIT_REALIZED'
  | 'GROUP_CONFIRMED'
  | 'GROUP_REALIZED'

export interface OccupancyReportFilters {
  year: number
  month: number
  careAreaId?: UUID | null // null = unselected, undefined = select all
  type: OccupancyReportType
  providerType?: ProviderType
}

export type OccupancyReportRow =
  | OccupancyUnitReportResultRow
  | OccupancyGroupReportResultRow

export async function getOccupanciesReport(
  filters: OccupancyReportFilters
): Promise<Result<OccupancyReportRow[]>> {
  return client
    .get<JsonOf<OccupancyReportRow[]>>(
      `/reports/occupancy-by-${filters.type.split('_')[0].toLowerCase()}`,
      {
        params: {
          ...filters,
          type: filters.type.split('_')[1]
        }
      }
    )
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export interface InvoiceReportFilters {
  date: LocalDate
}

export async function getInvoiceReport({
  date
}: InvoiceReportFilters): Promise<Result<InvoiceReport>> {
  return client
    .get<JsonOf<InvoiceReport>>('/reports/invoices', {
      params: {
        date: date.formatIso()
      }
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export interface PlacementsReportFilters {
  year: number
  month: number
}

export async function getEndedPlacementsReport(
  filters: PlacementsReportFilters
): Promise<Result<EndedPlacementsReportRow[]>> {
  return client
    .get<JsonOf<EndedPlacementsReportRow[]>>('/reports/ended-placements', {
      params: filters
    })
    .then((res) =>
      Success.of(
        res.data.map((row) => ({
          ...row,
          placementEnd: LocalDate.parseIso(row.placementEnd),
          nextPlacementStart: row.nextPlacementStart
            ? LocalDate.parseIso(row.nextPlacementStart)
            : null
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function getDuplicatePeopleReport(): Promise<
  Result<DuplicatePeopleReportRow[]>
> {
  return client
    .get<JsonOf<DuplicatePeopleReportRow[]>>('/reports/duplicate-people')
    .then((res) =>
      Success.of(
        res.data.map((row) => ({
          ...row,
          dateOfBirth: LocalDate.parseIso(row.dateOfBirth)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export function getStartingPlacementsReport(
  params: PlacementsReportFilters
): Promise<Result<StartingPlacementsRow[]>> {
  return client
    .get<JsonOf<StartingPlacementsRow[]>>('/reports/starting-placements', {
      params
    })
    .then((res) =>
      Success.of(
        res.data.map((row) => ({
          ...row,
          dateOfBirth: LocalDate.parseIso(row.dateOfBirth),
          placementStart: LocalDate.parseIso(row.placementStart)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export interface VoucherServiceProvidersFilters {
  year: number
  month: number
  areaId?: UUID
}

export async function getVoucherServiceProvidersReport(
  filters: VoucherServiceProvidersFilters
): Promise<Result<ServiceVoucherReport>> {
  return client
    .get<JsonOf<ServiceVoucherReport>>(`/reports/service-voucher-value/units`, {
      params: {
        ...filters
      }
    })
    .then((res) =>
      Success.of({
        locked: LocalDate.parseNullableIso(res.data.locked),
        rows: res.data.rows
      })
    )
    .catch((e) => Failure.fromError(e))
}

export function getVoucherServiceProviderUnitReport(
  unitId: UUID,
  params: VoucherProviderChildrenReportFilters
): Promise<Result<ServiceVoucherUnitReport>> {
  return client
    .get<JsonOf<ServiceVoucherUnitReport>>(
      `/reports/service-voucher-value/units/${unitId}`,
      {
        params
      }
    )
    .then((res) =>
      Success.of({
        ...res.data,
        locked: LocalDate.parseNullableIso(res.data.locked),
        rows: res.data.rows.map((row) => ({
          ...row,
          childDateOfBirth: LocalDate.parseIso(row.childDateOfBirth),
          realizedPeriod: FiniteDateRange.parseJson(row.realizedPeriod)
        }))
      })
    )
    .catch((e) => Failure.fromError(e))
}

export function getSextetReport(
  year: number,
  placementType: PlacementType
): Promise<Result<SextetReportRow[]>> {
  return client
    .get<SextetReportRow[]>(
      `/reports/sextet?year=${year}&placementType=${placementType}`
    )
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export interface VoucherProviderChildrenReportFilters {
  month: number
  year: number
}

export interface PlacementSketchingReportFilters {
  earliestPreferredStartDate: LocalDate
  placementStartDate: LocalDate
}

export async function getPlacementSketchingReport(
  filters: PlacementSketchingReportFilters
): Promise<Result<PlacementSketchingReportRow[]>> {
  return client
    .get<JsonOf<PlacementSketchingReportRow[]>>(
      `/reports/placement-sketching`,
      {
        params: {
          earliestPreferredStartDate:
            filters.earliestPreferredStartDate.formatIso(),
          placementStartDate: filters.placementStartDate.formatIso()
        }
      }
    )
    .then((res) =>
      Success.of(
        res.data.map((row) => ({
          ...row,
          childDob: LocalDate.parseIso(row.childDob),
          preferredStartDate: LocalDate.parseIso(row.preferredStartDate),
          sentDate: LocalDate.parseIso(row.sentDate)
        }))
      )
    )
}

export async function getVardaErrorsReport(): Promise<
  Result<VardaErrorReportRow[]>
> {
  return client
    .get<JsonOf<VardaErrorReportRow[]>>(`/reports/varda-errors`)
    .then((res) =>
      Success.of(
        res.data.map((row) => ({
          ...row,
          updated: new Date(row.updated),
          created: new Date(row.created),
          resetTimeStamp: row.resetTimeStamp
            ? new Date(row.resetTimeStamp)
            : null
        }))
      )
    )
}

export async function markChildForVardaReset(
  childId: string
): Promise<Result<void>> {
  return client
    .post<void>(`/varda/mark-child-for-reset/${childId}`)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function runResetVardaChildren(): Promise<Result<void>> {
  return client
    .post<void>(`/varda/reset-children`)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function sendPatuReport(
  filters: PeriodFilters
): Promise<Result<void>> {
  return client
    .post<void>(
      `/patu-report?from=${filters.from.formatIso()}&to=${filters.to.formatIso()}`
    )
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getAssistanceNeedDecisionsReport(): Promise<
  Result<AssistanceNeedDecisionsReportRow[]>
> {
  return client
    .get<JsonOf<AssistanceNeedDecisionsReportRow[]>>(
      '/reports/assistance-need-decisions'
    )
    .then(({ data }) =>
      Success.of(
        data.map((row) => ({
          ...row,
          sentForDecision: LocalDate.parseIso(row.sentForDecision),
          decisionMade: row.decisionMade
            ? LocalDate.parseIso(row.decisionMade)
            : null
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function getAssistanceNeedDecisionUnreadCount(): Promise<
  Result<number>
> {
  return client
    .get<JsonOf<number>>('/reports/assistance-need-decisions/unread-count')
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}
