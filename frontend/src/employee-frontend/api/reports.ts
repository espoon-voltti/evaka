// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import { ApplicationStatus } from 'lib-common/generated/api-types/application'
import { CareType, ProviderType } from 'lib-common/generated/api-types/daycare'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import {
  ApplicationsReportRow,
  AssistanceNeedDecisionsReportRow,
  AssistanceNeedsAndActionsReport,
  AssistanceNeedsAndActionsReportByChild,
  AttendanceReservationReportByChildRow,
  AttendanceReservationReportRow,
  ChildAgeLanguageReportRow,
  ChildrenInDifferentAddressReportRow,
  DecisionsReportRow,
  DuplicatePeopleReportRow,
  EndedPlacementsReportRow,
  FamilyConflictReportRow,
  FamilyContactReportRow,
  FamilyDaycareMealReportResult,
  FuturePreschoolersReportRow,
  InvoiceReport,
  ManualDuplicationReportRow,
  ManualDuplicationReportViewMode,
  MissingHeadOfFamilyReportRow,
  MissingServiceNeedReportRow,
  OccupancyGroupReportResultRow,
  OccupancyUnitReportResultRow,
  PartnersInDifferentAddressReportRow,
  PlacementCountReportResult,
  PlacementGuaranteeReportRow,
  PlacementSketchingReportRow,
  PreschoolGroupsReportRow,
  PresenceReportRow,
  RawReportRow,
  Report,
  ServiceNeedReportRow,
  ServiceVoucherReport,
  ServiceVoucherUnitReport,
  SextetReportRow,
  StartingPlacementsRow,
  UnitsReportRow,
  VardaErrorReportRow
} from 'lib-common/generated/api-types/reports'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import { client } from './client'

export interface PeriodFilters {
  from: LocalDate
  to: LocalDate
}

export async function getPermittedReports(): Promise<Result<Set<Report>>> {
  return client
    .get<JsonOf<Report[]>>('/reports')
    .then((res) => Success.of(new Set(res.data)))
    .catch((e) => Failure.fromError(e))
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
  range: DateRange
  showIntentionalDuplicates: boolean
}

export async function getMissingHeadOfFamilyReport(
  filters: MissingHeadOfFamilyReportFilters
): Promise<MissingHeadOfFamilyReportRow[]> {
  return client
    .get<JsonOf<MissingHeadOfFamilyReportRow[]>>(
      '/reports/missing-head-of-family',
      {
        params: {
          from: filters.range.start.formatIso(),
          to: filters.range.end?.formatIso(),
          showIntentionalDuplicates: filters.showIntentionalDuplicates
        }
      }
    )
    .then((res) =>
      res.data.map((row) => ({
        ...row,
        rangesWithoutHead: row.rangesWithoutHead.map((range) =>
          FiniteDateRange.parseJson(range)
        )
      }))
    )
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
): Promise<FamilyContactReportRow[]> {
  return client
    .get<JsonOf<FamilyContactReportRow[]>>('/reports/family-contacts', {
      params: { unitId }
    })
    .then((res) => res.data)
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

export async function getAssistanceNeedsAndActionsReportByChild(
  filters: AssistanceNeedsAndActionsReportFilters
): Promise<Result<AssistanceNeedsAndActionsReportByChild>> {
  return client
    .get<JsonOf<AssistanceNeedsAndActionsReportByChild>>(
      '/reports/assistance-needs-and-actions/by-child',
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
  unitTypes: CareType[]
}

export type OccupancyReportRow =
  | OccupancyUnitReportResultRow
  | OccupancyGroupReportResultRow

export async function getOccupanciesReport(
  filters: OccupancyReportFilters
): Promise<OccupancyReportRow[]> {
  return client
    .get<JsonOf<OccupancyReportRow[]>>(
      `/reports/occupancy-by-${filters.type.split('_')[0].toLowerCase()}`,
      {
        params: {
          ...filters,
          type: filters.type.split('_')[1],
          unitTypes: filters.unitTypes.join(',')
        }
      }
    )
    .then((res) => res.data)
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

export interface DuplicatePeopleFilters {
  showIntentionalDuplicates: boolean
}

export async function getDuplicatePeopleReport(
  params: DuplicatePeopleFilters
): Promise<Result<DuplicatePeopleReportRow[]>> {
  return client
    .get<JsonOf<DuplicatePeopleReportRow[]>>('/reports/duplicate-people', {
      params
    })
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
): Promise<ServiceVoucherReport> {
  return client
    .get<JsonOf<ServiceVoucherReport>>(`/reports/service-voucher-value/units`, {
      params: {
        ...filters
      }
    })
    .then((res) => ({
      locked: LocalDate.parseNullableIso(res.data.locked),
      rows: res.data.rows
    }))
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

export interface PlacementCountReportFilters {
  examinationDate: LocalDate
  placementTypes: PlacementType[]
  providerTypes: ProviderType[]
}

export async function getPlacementCountReport(
  filters: PlacementCountReportFilters
): Promise<Result<PlacementCountReportResult>> {
  return client
    .get<JsonOf<PlacementCountReportResult>>(`/reports/placement-count`, {
      params: {
        examinationDate: filters.examinationDate.formatIso(),
        placementTypes: filters.placementTypes.join(','),
        providerTypes: filters.providerTypes.join(',')
      }
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export interface PlacementGuaranteeReportFilters {
  date: LocalDate
  unitId: UUID | null
}

export async function getPlacementGuaranteeReport(
  filters: PlacementGuaranteeReportFilters
): Promise<PlacementGuaranteeReportRow[]> {
  return client
    .get<JsonOf<PlacementGuaranteeReportRow[]>>(
      '/reports/placement-guarantee',
      { params: filters }
    )
    .then((response) =>
      response.data.map((row) => ({
        ...row,
        placementStartDate: LocalDate.parseIso(row.placementStartDate),
        placementEndDate: LocalDate.parseIso(row.placementEndDate)
      }))
    )
}

export interface VoucherProviderChildrenReportFilters {
  month: number
  year: number
}

export interface PlacementSketchingReportFilters {
  earliestPreferredStartDate: LocalDate
  placementStartDate: LocalDate
  applicationStatus: ApplicationStatus[]
  earliestApplicationSentDate: LocalDate | null
  latestApplicationSentDate: LocalDate | null
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
          placementStartDate: filters.placementStartDate.formatIso(),
          applicationStatus: filters.applicationStatus.join(','),
          earliestApplicationSentDate:
            filters.earliestApplicationSentDate?.formatIso(),
          latestApplicationSentDate:
            filters.latestApplicationSentDate?.formatIso()
        }
      }
    )
    .then((res) =>
      Success.of(
        res.data.map((row) => ({
          ...row,
          childDob: LocalDate.parseIso(row.childDob),
          preferredStartDate: LocalDate.parseIso(row.preferredStartDate),
          sentDate: LocalDate.parseIso(row.sentDate),
          childMovingDate: row.childMovingDate
            ? LocalDate.parseIso(row.childMovingDate)
            : null
        }))
      )
    )
}

export async function getVardaErrorsReport(): Promise<VardaErrorReportRow[]> {
  return client
    .get<JsonOf<VardaErrorReportRow[]>>(`/reports/varda-errors`)
    .then((res) =>
      res.data.map((row) => ({
        ...row,
        updated: HelsinkiDateTime.parseIso(row.updated),
        created: HelsinkiDateTime.parseIso(row.created),
        resetTimeStamp: row.resetTimeStamp
          ? HelsinkiDateTime.parseIso(row.resetTimeStamp)
          : null
      }))
    )
}

export async function startVardaUpdate(): Promise<void> {
  await client.post(`/varda/start-update`)
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

export interface AttendanceReservationReportFilters {
  range: FiniteDateRange
  groupIds: UUID[]
}

export async function getAttendanceReservationReport(
  unitId: string,
  filters: AttendanceReservationReportFilters
): Promise<Result<AttendanceReservationReportRow[]>> {
  return client
    .get<JsonOf<AttendanceReservationReportRow[]>>(
      `/reports/attendance-reservation/${unitId}`,
      {
        params: {
          start: filters.range.start.formatIso(),
          end: filters.range.end.formatIso(),
          groupIds: filters.groupIds.join(',')
        }
      }
    )
    .then(({ data }) =>
      Success.of(
        data.map((row) => ({
          ...row,
          dateTime: HelsinkiDateTime.parseIso(row.dateTime)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function getAssistanceReservationReportByChild(
  unitId: string,
  filters: AttendanceReservationReportFilters
): Promise<Result<AttendanceReservationReportByChildRow[]>> {
  return client
    .get<JsonOf<AttendanceReservationReportByChildRow[]>>(
      `/reports/attendance-reservation/${unitId}/by-child`,
      {
        params: {
          start: filters.range.start.formatIso(),
          end: filters.range.end.formatIso(),
          groupIds: filters.groupIds.join(',')
        }
      }
    )
    .then(({ data }) =>
      Success.of(
        data.map((row) => ({
          ...row,
          date: LocalDate.parseIso(row.date),
          reservationStartTime:
            row.reservationStartTime !== null
              ? LocalTime.parse(row.reservationStartTime)
              : null,
          reservationEndTime:
            row.reservationEndTime !== null
              ? LocalTime.parse(row.reservationEndTime)
              : null
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export interface ManualDuplicationReportFilters {
  viewMode: ManualDuplicationReportViewMode
}

export async function getManualDuplicationReport(
  filters: ManualDuplicationReportFilters
): Promise<Result<ManualDuplicationReportRow[]>> {
  const params = {
    viewMode: filters.viewMode
  }
  return client
    .get<JsonOf<ManualDuplicationReportRow[]>>('/reports/manual-duplication', {
      params
    })
    .then(({ data }) =>
      Success.of(
        data.map((row) => ({
          ...row,
          dateOfBirth: LocalDate.parseIso(row.dateOfBirth),
          connectedStartDate: LocalDate.parseIso(row.connectedStartDate),
          connectedEndDate: LocalDate.parseIso(row.connectedEndDate),
          preschoolStartDate: LocalDate.parseIso(row.preschoolStartDate),
          preschoolEndDate: LocalDate.parseIso(row.preschoolEndDate)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function getFamilyDaycareMealCountReport(
  filters: FamilyDaycareMealCountReportFilters
): Promise<Result<FamilyDaycareMealReportResult>> {
  if (filters.startDate > filters.endDate) {
    return Failure.of<FamilyDaycareMealReportResult>({
      message: 'Start date after end date'
    })
  }
  return client
    .get<JsonOf<FamilyDaycareMealReportResult>>(
      `/reports/family-daycare-meal-count`,
      {
        params: {
          startDate: filters.startDate.formatIso(),
          endDate: filters.endDate.formatIso()
        }
      }
    )
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export interface FamilyDaycareMealCountReportFilters {
  startDate: LocalDate
  endDate: LocalDate
}

export async function getFuturePreschoolersReport(): Promise<
  FuturePreschoolersReportRow[]
> {
  return client
    .get<JsonOf<FuturePreschoolersReportRow[]>>('/reports/future-preschoolers')
    .then((res) => res.data)
}

export async function getPreschoolGroupsReport(
  municipal: boolean
): Promise<PreschoolGroupsReportRow[]> {
  return client
    .get<JsonOf<PreschoolGroupsReportRow[]>>(
      '/reports/future-preschoolers/groups',
      { params: { municipal: municipal } }
    )
    .then((res) => res.data)
}

export async function getUnitsReport(): Promise<UnitsReportRow[]> {
  return client
    .get<JsonOf<UnitsReportRow[]>>('/reports/units')
    .then((res) => res.data)
}
