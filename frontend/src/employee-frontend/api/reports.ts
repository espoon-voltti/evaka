// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { client } from './client'
import {
  ChildAgeLanguageReportRow,
  ChildrenInDifferentAddressReportRow,
  EndedPlacementsReportRow,
  FamilyConflictReportRow,
  MissingHeadOfFamilyReportRow,
  MissingServiceNeedReportRow,
  OccupancyReportRow,
  PartnersInDifferentAddressReportRow,
  InvoiceReport,
  DuplicatePeopleReportRow,
  StartingPlacementsRow,
  ApplicationsReportRow,
  PresenceReportRow,
  ServiceNeedReportRow,
  RawReportRow,
  FamilyContactsReportRow,
  PlacementSketchingRow,
  AssistanceNeedsAndActionsReportRow,
  VoucherServiceProviderUnitReport,
  VoucherServiceProviderReport,
  InvalidServiceNeedReportRow,
  DecisionsReportRow
} from '../types/reports'
import { UUID } from '../types'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import FiniteDateRange from 'lib-common/finite-date-range'

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

export async function getInvalidServiceNeedReport(): Promise<
  Result<InvalidServiceNeedReportRow[]>
> {
  return client
    .get<JsonOf<InvalidServiceNeedReportRow[]>>('/reports/invalid-service-need')
    .then((res) =>
      res.data.map((row) => ({
        ...row,
        startDate: LocalDate.parseIso(row.startDate),
        endDate: LocalDate.parseIso(row.endDate)
      }))
    )
    .then((rows) => Success.of(rows))
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
): Promise<Result<FamilyContactsReportRow[]>> {
  return client
    .get<JsonOf<FamilyContactsReportRow[]>>('/reports/family-contacts', {
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
): Promise<Result<AssistanceNeedsAndActionsReportRow[]>> {
  return client
    .get<JsonOf<AssistanceNeedsAndActionsReportRow[]>>(
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
  careAreaId: UUID
  type: OccupancyReportType
}

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
): Promise<Result<VoucherServiceProviderReport>> {
  return client
    .get<JsonOf<VoucherServiceProviderReport>>(
      `/reports/service-voucher-value/units`,
      {
        params: {
          ...filters
        }
      }
    )
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
): Promise<Result<VoucherServiceProviderUnitReport>> {
  return client
    .get<JsonOf<VoucherServiceProviderUnitReport>>(
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
): Promise<Result<PlacementSketchingRow[]>> {
  return client
    .get<JsonOf<PlacementSketchingRow[]>>(`/reports/placement-sketching`, {
      params: {
        earliestPreferredStartDate:
          filters.earliestPreferredStartDate.formatIso(),
        placementStartDate: filters.placementStartDate.formatIso()
      }
    })
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
