// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from '~api/index'
import { client } from '~api/client'
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
  AssistanceNeedsReportRow,
  StartingPlacementsRow,
  AssistanceActionsReportRow,
  ApplicationsReportRow,
  PresenceReportRow,
  ServiceNeedReportRow,
  RawReportRow,
  FamilyContactsReportRow,
  VoucherServiceProviderRow
} from '~types/reports'
import { UUID } from '~types'
import { JsonOf } from '@evaka/lib-common/src/json'
import LocalDate from '@evaka/lib-common/src/local-date'

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
    .then((res) => Success(res.data))
    .catch(Failure)
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
      res.data.map((row) => ({
        ...row,
        day: LocalDate.parseIso(row.day),
        dateOfBirth: LocalDate.parseIso(row.dateOfBirth)
      }))
    )
    .then(Success)
    .catch(Failure)
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
      Success(
        res.data.map((row) => ({
          ...row,
          date: LocalDate.parseIso(row.date)
        }))
      )
    )
    .catch(Failure)
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
    .then((res) => Success(res.data))
    .catch(Failure)
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
    .then((res) => Success(res.data))
    .catch(Failure)
}

export async function getFamilyConflictsReport(): Promise<
  Result<FamilyConflictReportRow[]>
> {
  return client
    .get<JsonOf<FamilyConflictReportRow[]>>('/reports/family-conflicts')
    .then((res) => Success(res.data))
    .catch(Failure)
}

export async function getFamilyContactsReport(
  unitId: UUID
): Promise<Result<FamilyContactsReportRow[]>> {
  return client
    .get<JsonOf<FamilyContactsReportRow[]>>('/reports/family-contacts', {
      params: { unitId }
    })
    .then((res) => Success(res.data))
    .catch(Failure)
}

export async function getPartnersInDifferentAddressReport(): Promise<
  Result<PartnersInDifferentAddressReportRow[]>
> {
  return client
    .get<JsonOf<PartnersInDifferentAddressReportRow[]>>(
      '/reports/partners-in-different-address'
    )
    .then((res) => Success(res.data))
    .catch(Failure)
}

export async function getChildrenInDifferentAddressReport(): Promise<
  Result<ChildrenInDifferentAddressReportRow[]>
> {
  return client
    .get<JsonOf<ChildrenInDifferentAddressReportRow[]>>(
      '/reports/children-in-different-address'
    )
    .then((res) => Success(res.data))
    .catch(Failure)
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
    .then((res) => Success(res.data))
    .catch(Failure)
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
    .then((res) => Success(res.data))
    .catch(Failure)
}

export interface AssistanceNeedsReportFilters {
  date: LocalDate
}

export async function getAssistanceNeedsReport(
  filters: AssistanceNeedsReportFilters
): Promise<Result<AssistanceNeedsReportRow[]>> {
  return client
    .get<JsonOf<AssistanceNeedsReportRow[]>>('/reports/assistance-needs', {
      params: {
        date: filters.date.formatIso()
      }
    })
    .then((res) => Success(res.data))
    .catch(Failure)
}

export interface AssistanceActionsReportFilters {
  date: LocalDate
}

export async function getAssistanceActionsReport(
  filters: AssistanceActionsReportFilters
): Promise<Result<AssistanceActionsReportRow[]>> {
  return client
    .get<JsonOf<AssistanceActionsReportRow[]>>('/reports/assistance-actions', {
      params: {
        date: filters.date.formatIso()
      }
    })
    .then((res) => Success(res.data))
    .catch(Failure)
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
    .then((res) => Success(res.data))
    .catch(Failure)
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
    .then((res) => Success(res.data))
    .catch(Failure)
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
      Success(
        res.data.map((row) => ({
          ...row,
          placementEnd: LocalDate.parseIso(row.placementEnd),
          nextPlacementStart: row.nextPlacementStart
            ? LocalDate.parseIso(row.nextPlacementStart)
            : null
        }))
      )
    )
    .catch(Failure)
}

export async function getDuplicatePeopleReport(): Promise<
  Result<DuplicatePeopleReportRow[]>
> {
  return client
    .get<JsonOf<DuplicatePeopleReportRow[]>>('/reports/duplicate-people')
    .then((res) =>
      Success(
        res.data.map((row) => ({
          ...row,
          dateOfBirth: LocalDate.parseIso(row.dateOfBirth)
        }))
      )
    )
    .catch(Failure)
}

export function getStartingPlacementsReport(
  params: PlacementsReportFilters
): Promise<Result<StartingPlacementsRow[]>> {
  return client
    .get<JsonOf<StartingPlacementsRow[]>>('/reports/starting-placements', {
      params
    })
    .then((res) =>
      Success(
        res.data.map((row) => ({
          ...row,
          dateOfBirth: LocalDate.parseIso(row.dateOfBirth),
          placementStart: LocalDate.parseIso(row.placementStart)
        }))
      )
    )
    .catch(Failure)
}

export interface VoucherServiceProvidersFilters {
  year: number
  month: number
  careAreaId: UUID
}

export async function getVoucherServiceProvidersReport(
  filters: VoucherServiceProvidersFilters
): Promise<Result<VoucherServiceProviderRow[]>> {
  return client
    .get<JsonOf<VoucherServiceProviderRow[]>>(
      `/reports/voucher-service-providers}`,
      {
        params: {
          ...filters
        }
      }
    )
    .then((res) =>
      Success(
        res.data.map(
          (row) =>
            ({
              ...row,
              startDate: LocalDate.parseIso(row.startDate),
              endDate: LocalDate.parseIso(row.endDate)
            } as VoucherServiceProviderRow)
        )
      )
    )
    .catch(Failure)
}
