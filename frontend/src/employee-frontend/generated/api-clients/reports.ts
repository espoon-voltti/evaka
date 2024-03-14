// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { ApplicationStatus } from 'lib-common/generated/api-types/application'
import { ApplicationsReportRow } from 'lib-common/generated/api-types/reports'
import { AssistanceNeedDecisionsReportRow } from 'lib-common/generated/api-types/reports'
import { AssistanceNeedsAndActionsReport } from 'lib-common/generated/api-types/reports'
import { AssistanceNeedsAndActionsReportByChild } from 'lib-common/generated/api-types/reports'
import { AttendanceReservationReportByChildRow } from 'lib-common/generated/api-types/reports'
import { AttendanceReservationReportRow } from 'lib-common/generated/api-types/reports'
import { CareType } from 'lib-common/generated/api-types/daycare'
import { ChildAgeLanguageReportRow } from 'lib-common/generated/api-types/reports'
import { ChildrenInDifferentAddressReportRow } from 'lib-common/generated/api-types/reports'
import { DecisionsReportRow } from 'lib-common/generated/api-types/reports'
import { DuplicatePeopleReportRow } from 'lib-common/generated/api-types/reports'
import { EndedPlacementsReportRow } from 'lib-common/generated/api-types/reports'
import { ExceededServiceNeedReportRow } from 'lib-common/generated/api-types/reports'
import { ExceededServiceNeedReportUnit } from 'lib-common/generated/api-types/reports'
import { FamilyConflictReportRow } from 'lib-common/generated/api-types/reports'
import { FamilyContactReportRow } from 'lib-common/generated/api-types/reports'
import { FamilyDaycareMealReportResult } from 'lib-common/generated/api-types/reports'
import { FuturePreschoolersReportRow } from 'lib-common/generated/api-types/reports'
import { InvoiceReport } from 'lib-common/generated/api-types/reports'
import { JsonOf } from 'lib-common/json'
import { ManualDuplicationReportRow } from 'lib-common/generated/api-types/reports'
import { ManualDuplicationReportViewMode } from 'lib-common/generated/api-types/reports'
import { MissingHeadOfFamilyReportRow } from 'lib-common/generated/api-types/reports'
import { MissingServiceNeedReportRow } from 'lib-common/generated/api-types/reports'
import { NonSsnChildrenReportRow } from 'lib-common/generated/api-types/reports'
import { OccupancyGroupReportResultRow } from 'lib-common/generated/api-types/reports'
import { OccupancyType } from 'lib-common/generated/api-types/occupancy'
import { OccupancyUnitReportResultRow } from 'lib-common/generated/api-types/reports'
import { PartnersInDifferentAddressReportRow } from 'lib-common/generated/api-types/reports'
import { PlacementCountReportResult } from 'lib-common/generated/api-types/reports'
import { PlacementGuaranteeReportRow } from 'lib-common/generated/api-types/reports'
import { PlacementSketchingReportRow } from 'lib-common/generated/api-types/reports'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import { PreschoolGroupsReportRow } from 'lib-common/generated/api-types/reports'
import { PresenceReportRow } from 'lib-common/generated/api-types/reports'
import { ProviderType } from 'lib-common/generated/api-types/daycare'
import { RawReportRow } from 'lib-common/generated/api-types/reports'
import { Report } from 'lib-common/generated/api-types/reports'
import { ServiceNeedReportRow } from 'lib-common/generated/api-types/reports'
import { ServiceVoucherReport } from 'lib-common/generated/api-types/reports'
import { ServiceVoucherUnitReport } from 'lib-common/generated/api-types/reports'
import { SextetReportRow } from 'lib-common/generated/api-types/reports'
import { StartingPlacementsRow } from 'lib-common/generated/api-types/reports'
import { UUID } from 'lib-common/types'
import { UnitsReportRow } from 'lib-common/generated/api-types/reports'
import { VardaErrorReportRow } from 'lib-common/generated/api-types/reports'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonAssistanceNeedDecisionsReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonAttendanceReservationReportByChildRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonAttendanceReservationReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonDuplicatePeopleReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonEndedPlacementsReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonManualDuplicationReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonMissingHeadOfFamilyReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonNonSsnChildrenReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonPlacementGuaranteeReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonPlacementSketchingReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonPresenceReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonRawReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonServiceVoucherReport } from 'lib-common/generated/api-types/reports'
import { deserializeJsonServiceVoucherUnitReport } from 'lib-common/generated/api-types/reports'
import { deserializeJsonStartingPlacementsRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonVardaErrorReportRow } from 'lib-common/generated/api-types/reports'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.reports.ApplicationsReportController.getApplicationsReport
*/
export async function getApplicationsReport(
  request: {
    from: LocalDate,
    to: LocalDate
  }
): Promise<ApplicationsReportRow[]> {
  const params = createUrlSearchParams(
    ['from', request.from.formatIso()],
    ['to', request.to.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<ApplicationsReportRow[]>>({
    url: uri`/reports/applications`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.AssistanceNeedDecisionsReport.getAssistanceNeedDecisionsReport
*/
export async function getAssistanceNeedDecisionsReport(): Promise<AssistanceNeedDecisionsReportRow[]> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedDecisionsReportRow[]>>({
    url: uri`/reports/assistance-need-decisions`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonAssistanceNeedDecisionsReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.AssistanceNeedDecisionsReport.getAssistanceNeedDecisionsReportUnreadCount
*/
export async function getAssistanceNeedDecisionsReportUnreadCount(): Promise<number> {
  const { data: json } = await client.request<JsonOf<number>>({
    url: uri`/reports/assistance-need-decisions/unread-count`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.AssistanceNeedsAndActionsReportController.getAssistanceNeedsAndActionsReport
*/
export async function getAssistanceNeedsAndActionsReport(
  request: {
    date: LocalDate
  }
): Promise<AssistanceNeedsAndActionsReport> {
  const params = createUrlSearchParams(
    ['date', request.date.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<AssistanceNeedsAndActionsReport>>({
    url: uri`/reports/assistance-needs-and-actions`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.AssistanceNeedsAndActionsReportController.getAssistanceNeedsAndActionsReportByChild
*/
export async function getAssistanceNeedsAndActionsReportByChild(
  request: {
    date: LocalDate
  }
): Promise<AssistanceNeedsAndActionsReportByChild> {
  const params = createUrlSearchParams(
    ['date', request.date.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<AssistanceNeedsAndActionsReportByChild>>({
    url: uri`/reports/assistance-needs-and-actions/by-child`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.AttendanceReservationReportController.getAttendanceReservationReportByUnit
*/
export async function getAttendanceReservationReportByUnit(
  request: {
    unitId: UUID,
    start: LocalDate,
    end: LocalDate,
    groupIds?: UUID[] | null
  }
): Promise<AttendanceReservationReportRow[]> {
  const params = createUrlSearchParams(
    ['start', request.start.formatIso()],
    ['end', request.end.formatIso()],
    ...(request.groupIds?.map((e): [string, string | null | undefined] => ['groupIds', e]) ?? [])
  )
  const { data: json } = await client.request<JsonOf<AttendanceReservationReportRow[]>>({
    url: uri`/reports/attendance-reservation/${request.unitId}`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonAttendanceReservationReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.AttendanceReservationReportController.getAttendanceReservationReportByUnitAndChild
*/
export async function getAttendanceReservationReportByUnitAndChild(
  request: {
    unitId: UUID,
    start: LocalDate,
    end: LocalDate,
    groupIds?: UUID[] | null
  }
): Promise<AttendanceReservationReportByChildRow[]> {
  const params = createUrlSearchParams(
    ['start', request.start.formatIso()],
    ['end', request.end.formatIso()],
    ...(request.groupIds?.map((e): [string, string | null | undefined] => ['groupIds', e]) ?? [])
  )
  const { data: json } = await client.request<JsonOf<AttendanceReservationReportByChildRow[]>>({
    url: uri`/reports/attendance-reservation/${request.unitId}/by-child`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonAttendanceReservationReportByChildRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.ChildAgeLanguageReportController.getChildAgeLanguageReport
*/
export async function getChildAgeLanguageReport(
  request: {
    date: LocalDate
  }
): Promise<ChildAgeLanguageReportRow[]> {
  const params = createUrlSearchParams(
    ['date', request.date.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<ChildAgeLanguageReportRow[]>>({
    url: uri`/reports/child-age-language`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.ChildrenInDifferentAddressReportController.getChildrenInDifferentAddressReport
*/
export async function getChildrenInDifferentAddressReport(): Promise<ChildrenInDifferentAddressReportRow[]> {
  const { data: json } = await client.request<JsonOf<ChildrenInDifferentAddressReportRow[]>>({
    url: uri`/reports/children-in-different-address`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.DecisionsReportController.getDecisionsReport
*/
export async function getDecisionsReport(
  request: {
    from: LocalDate,
    to: LocalDate
  }
): Promise<DecisionsReportRow[]> {
  const params = createUrlSearchParams(
    ['from', request.from.formatIso()],
    ['to', request.to.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<DecisionsReportRow[]>>({
    url: uri`/reports/decisions`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.DuplicatePeopleReportController.getDuplicatePeopleReport
*/
export async function getDuplicatePeopleReport(
  request: {
    showIntentionalDuplicates?: boolean | null
  }
): Promise<DuplicatePeopleReportRow[]> {
  const params = createUrlSearchParams(
    ['showIntentionalDuplicates', request.showIntentionalDuplicates?.toString()]
  )
  const { data: json } = await client.request<JsonOf<DuplicatePeopleReportRow[]>>({
    url: uri`/reports/duplicate-people`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonDuplicatePeopleReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.EndedPlacementsReportController.getEndedPlacementsReport
*/
export async function getEndedPlacementsReport(
  request: {
    year: number,
    month: number
  }
): Promise<EndedPlacementsReportRow[]> {
  const params = createUrlSearchParams(
    ['year', request.year.toString()],
    ['month', request.month.toString()]
  )
  const { data: json } = await client.request<JsonOf<EndedPlacementsReportRow[]>>({
    url: uri`/reports/ended-placements`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonEndedPlacementsReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.ExceededServiceNeedsReportController.getExceededServiceNeedReportRows
*/
export async function getExceededServiceNeedReportRows(
  request: {
    unitId: UUID,
    year: number,
    month: number
  }
): Promise<ExceededServiceNeedReportRow[]> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId],
    ['year', request.year.toString()],
    ['month', request.month.toString()]
  )
  const { data: json } = await client.request<JsonOf<ExceededServiceNeedReportRow[]>>({
    url: uri`/reports/exceeded-service-need/rows`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.ExceededServiceNeedsReportController.getExceededServiceNeedReportUnits
*/
export async function getExceededServiceNeedReportUnits(): Promise<ExceededServiceNeedReportUnit[]> {
  const { data: json } = await client.request<JsonOf<ExceededServiceNeedReportUnit[]>>({
    url: uri`/reports/exceeded-service-need/units`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.FamilyConflictReportController.getFamilyConflictsReport
*/
export async function getFamilyConflictsReport(): Promise<FamilyConflictReportRow[]> {
  const { data: json } = await client.request<JsonOf<FamilyConflictReportRow[]>>({
    url: uri`/reports/family-conflicts`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.FamilyContactReportController.getFamilyContactsReport
*/
export async function getFamilyContactsReport(
  request: {
    unitId: UUID
  }
): Promise<FamilyContactReportRow[]> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId]
  )
  const { data: json } = await client.request<JsonOf<FamilyContactReportRow[]>>({
    url: uri`/reports/family-contacts`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.FamilyDaycareMealReport.getFamilyDaycareMealReport
*/
export async function getFamilyDaycareMealReport(
  request: {
    startDate: LocalDate,
    endDate: LocalDate
  }
): Promise<FamilyDaycareMealReportResult> {
  const params = createUrlSearchParams(
    ['startDate', request.startDate.formatIso()],
    ['endDate', request.endDate.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<FamilyDaycareMealReportResult>>({
    url: uri`/reports/family-daycare-meal-count`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.FuturePreschoolersReport.getFuturePreschoolersGroupsReport
*/
export async function getFuturePreschoolersGroupsReport(
  request: {
    municipal: boolean
  }
): Promise<PreschoolGroupsReportRow[]> {
  const params = createUrlSearchParams(
    ['municipal', request.municipal.toString()]
  )
  const { data: json } = await client.request<JsonOf<PreschoolGroupsReportRow[]>>({
    url: uri`/reports/future-preschoolers/groups`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.FuturePreschoolersReport.getFuturePreschoolersReport
*/
export async function getFuturePreschoolersReport(): Promise<FuturePreschoolersReportRow[]> {
  const { data: json } = await client.request<JsonOf<FuturePreschoolersReportRow[]>>({
    url: uri`/reports/future-preschoolers`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.InvoiceReportController.getInvoiceReport
*/
export async function getInvoiceReport(
  request: {
    date: LocalDate
  }
): Promise<InvoiceReport> {
  const params = createUrlSearchParams(
    ['date', request.date.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<InvoiceReport>>({
    url: uri`/reports/invoices`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.ManualDuplicationReportController.getManualDuplicationReport
*/
export async function getManualDuplicationReport(
  request: {
    viewMode?: ManualDuplicationReportViewMode | null
  }
): Promise<ManualDuplicationReportRow[]> {
  const params = createUrlSearchParams(
    ['viewMode', request.viewMode?.toString()]
  )
  const { data: json } = await client.request<JsonOf<ManualDuplicationReportRow[]>>({
    url: uri`/reports/manual-duplication`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonManualDuplicationReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.MissingHeadOfFamilyReportController.getMissingHeadOfFamilyReport
*/
export async function getMissingHeadOfFamilyReport(
  request: {
    from: LocalDate,
    to?: LocalDate | null,
    showIntentionalDuplicates?: boolean | null
  }
): Promise<MissingHeadOfFamilyReportRow[]> {
  const params = createUrlSearchParams(
    ['from', request.from.formatIso()],
    ['to', request.to?.formatIso()],
    ['showIntentionalDuplicates', request.showIntentionalDuplicates?.toString()]
  )
  const { data: json } = await client.request<JsonOf<MissingHeadOfFamilyReportRow[]>>({
    url: uri`/reports/missing-head-of-family`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonMissingHeadOfFamilyReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.MissingServiceNeedReportController.getMissingServiceNeedReport
*/
export async function getMissingServiceNeedReport(
  request: {
    from: LocalDate,
    to?: LocalDate | null
  }
): Promise<MissingServiceNeedReportRow[]> {
  const params = createUrlSearchParams(
    ['from', request.from.formatIso()],
    ['to', request.to?.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<MissingServiceNeedReportRow[]>>({
    url: uri`/reports/missing-service-need`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.NonSsnChildrenReportController.getNonSsnChildrenReportRows
*/
export async function getNonSsnChildrenReportRows(): Promise<NonSsnChildrenReportRow[]> {
  const { data: json } = await client.request<JsonOf<NonSsnChildrenReportRow[]>>({
    url: uri`/reports/non-ssn-children`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonNonSsnChildrenReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.OccupancyReportController.getOccupancyGroupReport
*/
export async function getOccupancyGroupReport(
  request: {
    type: OccupancyType,
    careAreaId?: UUID | null,
    providerType?: ProviderType | null,
    unitTypes?: CareType[] | null,
    year: number,
    month: number
  }
): Promise<OccupancyGroupReportResultRow[]> {
  const params = createUrlSearchParams(
    ['type', request.type.toString()],
    ['careAreaId', request.careAreaId],
    ['providerType', request.providerType?.toString()],
    ...(request.unitTypes?.map((e): [string, string | null | undefined] => ['unitTypes', e.toString()]) ?? []),
    ['year', request.year.toString()],
    ['month', request.month.toString()]
  )
  const { data: json } = await client.request<JsonOf<OccupancyGroupReportResultRow[]>>({
    url: uri`/reports/occupancy-by-group`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.OccupancyReportController.getOccupancyUnitReport
*/
export async function getOccupancyUnitReport(
  request: {
    type: OccupancyType,
    careAreaId?: UUID | null,
    providerType?: ProviderType | null,
    unitTypes?: CareType[] | null,
    year: number,
    month: number
  }
): Promise<OccupancyUnitReportResultRow[]> {
  const params = createUrlSearchParams(
    ['type', request.type.toString()],
    ['careAreaId', request.careAreaId],
    ['providerType', request.providerType?.toString()],
    ...(request.unitTypes?.map((e): [string, string | null | undefined] => ['unitTypes', e.toString()]) ?? []),
    ['year', request.year.toString()],
    ['month', request.month.toString()]
  )
  const { data: json } = await client.request<JsonOf<OccupancyUnitReportResultRow[]>>({
    url: uri`/reports/occupancy-by-unit`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.PartnersInDifferentAddressReportController.getPartnersInDifferentAddressReport
*/
export async function getPartnersInDifferentAddressReport(): Promise<PartnersInDifferentAddressReportRow[]> {
  const { data: json } = await client.request<JsonOf<PartnersInDifferentAddressReportRow[]>>({
    url: uri`/reports/partners-in-different-address`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.PlacementCountReportController.getPlacementCountReport
*/
export async function getPlacementCountReport(
  request: {
    examinationDate: LocalDate,
    providerTypes?: ProviderType[] | null,
    placementTypes?: PlacementType[] | null
  }
): Promise<PlacementCountReportResult> {
  const params = createUrlSearchParams(
    ['examinationDate', request.examinationDate.formatIso()],
    ...(request.providerTypes?.map((e): [string, string | null | undefined] => ['providerTypes', e.toString()]) ?? []),
    ...(request.placementTypes?.map((e): [string, string | null | undefined] => ['placementTypes', e.toString()]) ?? [])
  )
  const { data: json } = await client.request<JsonOf<PlacementCountReportResult>>({
    url: uri`/reports/placement-count`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.PlacementGuaranteeReportController.getPlacementGuaranteeReport
*/
export async function getPlacementGuaranteeReport(
  request: {
    date: LocalDate,
    unitId?: UUID | null
  }
): Promise<PlacementGuaranteeReportRow[]> {
  const params = createUrlSearchParams(
    ['date', request.date.formatIso()],
    ['unitId', request.unitId]
  )
  const { data: json } = await client.request<JsonOf<PlacementGuaranteeReportRow[]>>({
    url: uri`/reports/placement-guarantee`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonPlacementGuaranteeReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.PlacementSketchingReportController.getPlacementSketchingReport
*/
export async function getPlacementSketchingReport(
  request: {
    placementStartDate: LocalDate,
    earliestPreferredStartDate?: LocalDate | null,
    applicationStatus?: ApplicationStatus[] | null,
    earliestApplicationSentDate?: LocalDate | null,
    latestApplicationSentDate?: LocalDate | null
  }
): Promise<PlacementSketchingReportRow[]> {
  const params = createUrlSearchParams(
    ['placementStartDate', request.placementStartDate.formatIso()],
    ['earliestPreferredStartDate', request.earliestPreferredStartDate?.formatIso()],
    ...(request.applicationStatus?.map((e): [string, string | null | undefined] => ['applicationStatus', e.toString()]) ?? []),
    ['earliestApplicationSentDate', request.earliestApplicationSentDate?.formatIso()],
    ['latestApplicationSentDate', request.latestApplicationSentDate?.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<PlacementSketchingReportRow[]>>({
    url: uri`/reports/placement-sketching`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonPlacementSketchingReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.PresenceReportController.getPresenceReport
*/
export async function getPresenceReport(
  request: {
    from: LocalDate,
    to: LocalDate
  }
): Promise<PresenceReportRow[]> {
  const params = createUrlSearchParams(
    ['from', request.from.formatIso()],
    ['to', request.to.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<PresenceReportRow[]>>({
    url: uri`/reports/presences`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonPresenceReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.RawReportController.getRawReport
*/
export async function getRawReport(
  request: {
    from: LocalDate,
    to: LocalDate
  }
): Promise<RawReportRow[]> {
  const params = createUrlSearchParams(
    ['from', request.from.formatIso()],
    ['to', request.to.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<RawReportRow[]>>({
    url: uri`/reports/raw`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonRawReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.ReportPermissions.getPermittedReports
*/
export async function getPermittedReports(): Promise<Report[]> {
  const { data: json } = await client.request<JsonOf<Report[]>>({
    url: uri`/reports`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.ServiceNeedReport.getServiceNeedReport
*/
export async function getServiceNeedReport(
  request: {
    date: LocalDate
  }
): Promise<ServiceNeedReportRow[]> {
  const params = createUrlSearchParams(
    ['date', request.date.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<ServiceNeedReportRow[]>>({
    url: uri`/reports/service-need`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.ServiceVoucherValueReportController.getServiceVoucherReportForAllUnits
*/
export async function getServiceVoucherReportForAllUnits(
  request: {
    year: number,
    month: number,
    areaId?: UUID | null
  }
): Promise<ServiceVoucherReport> {
  const params = createUrlSearchParams(
    ['year', request.year.toString()],
    ['month', request.month.toString()],
    ['areaId', request.areaId]
  )
  const { data: json } = await client.request<JsonOf<ServiceVoucherReport>>({
    url: uri`/reports/service-voucher-value/units`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonServiceVoucherReport(json)
}


/**
* Generated from fi.espoo.evaka.reports.ServiceVoucherValueReportController.getServiceVoucherReportForUnit
*/
export async function getServiceVoucherReportForUnit(
  request: {
    unitId: UUID,
    year: number,
    month: number
  }
): Promise<ServiceVoucherUnitReport> {
  const params = createUrlSearchParams(
    ['year', request.year.toString()],
    ['month', request.month.toString()]
  )
  const { data: json } = await client.request<JsonOf<ServiceVoucherUnitReport>>({
    url: uri`/reports/service-voucher-value/units/${request.unitId}`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonServiceVoucherUnitReport(json)
}


/**
* Generated from fi.espoo.evaka.reports.SextetReportController.getSextetReport
*/
export async function getSextetReport(
  request: {
    year: number,
    placementType: PlacementType
  }
): Promise<SextetReportRow[]> {
  const params = createUrlSearchParams(
    ['year', request.year.toString()],
    ['placementType', request.placementType.toString()]
  )
  const { data: json } = await client.request<JsonOf<SextetReportRow[]>>({
    url: uri`/reports/sextet`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.StartingPlacementsReportController.getStartingPlacementsReport
*/
export async function getStartingPlacementsReport(
  request: {
    year: number,
    month: number
  }
): Promise<StartingPlacementsRow[]> {
  const params = createUrlSearchParams(
    ['year', request.year.toString()],
    ['month', request.month.toString()]
  )
  const { data: json } = await client.request<JsonOf<StartingPlacementsRow[]>>({
    url: uri`/reports/starting-placements`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonStartingPlacementsRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.UnitsReportController.getUnitsReport
*/
export async function getUnitsReport(): Promise<UnitsReportRow[]> {
  const { data: json } = await client.request<JsonOf<UnitsReportRow[]>>({
    url: uri`/reports/units`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.VardaErrorReport.getVardaErrorsReport
*/
export async function getVardaErrorsReport(): Promise<VardaErrorReportRow[]> {
  const { data: json } = await client.request<JsonOf<VardaErrorReportRow[]>>({
    url: uri`/reports/varda-errors`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonVardaErrorReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.patu.PatuReportingController.sendPatuReport
*/
export async function sendPatuReport(
  request: {
    from: LocalDate,
    to: LocalDate
  }
): Promise<void> {
  const params = createUrlSearchParams(
    ['from', request.from.formatIso()],
    ['to', request.to.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/patu-report`.toString(),
    method: 'POST',
    params
  })
  return json
}
