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
import { AttendanceReservationReportByChildBody } from 'lib-common/generated/api-types/reports'
import { AttendanceReservationReportByChildGroup } from 'lib-common/generated/api-types/reports'
import { AttendanceReservationReportRow } from 'lib-common/generated/api-types/reports'
import { CareType } from 'lib-common/generated/api-types/daycare'
import { ChildAgeLanguageReportRow } from 'lib-common/generated/api-types/reports'
import { ChildAttendanceReportRow } from 'lib-common/generated/api-types/reports'
import { ChildPreschoolAbsenceRow } from 'lib-common/generated/api-types/reports'
import { ChildrenInDifferentAddressReportRow } from 'lib-common/generated/api-types/reports'
import { CustomerFeesReportRow } from 'lib-common/generated/api-types/reports'
import { DecisionsReportRow } from 'lib-common/generated/api-types/reports'
import { DuplicatePeopleReportRow } from 'lib-common/generated/api-types/reports'
import { EndedPlacementsReportRow } from 'lib-common/generated/api-types/reports'
import { ExceededServiceNeedReportRow } from 'lib-common/generated/api-types/reports'
import { ExceededServiceNeedReportUnit } from 'lib-common/generated/api-types/reports'
import { FamilyConflictReportRow } from 'lib-common/generated/api-types/reports'
import { FamilyContactReportRow } from 'lib-common/generated/api-types/reports'
import { FamilyDaycareMealReportResult } from 'lib-common/generated/api-types/reports'
import { FinanceDecisionType } from 'lib-common/generated/api-types/invoicing'
import { FuturePreschoolersReportRow } from 'lib-common/generated/api-types/reports'
import { HolidayPeriodAttendanceReportRow } from 'lib-common/generated/api-types/reports'
import { InvoiceReport } from 'lib-common/generated/api-types/reports'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { ManualDuplicationReportRow } from 'lib-common/generated/api-types/reports'
import { ManualDuplicationReportViewMode } from 'lib-common/generated/api-types/reports'
import { MealReportData } from 'lib-common/generated/api-types/reports'
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
import { PreschoolApplicationReportRow } from 'lib-common/generated/api-types/reports'
import { PreschoolUnitsReportRow } from 'lib-common/generated/api-types/reports'
import { PresenceReportRow } from 'lib-common/generated/api-types/reports'
import { ProviderType } from 'lib-common/generated/api-types/daycare'
import { RawReportRow } from 'lib-common/generated/api-types/reports'
import { Report } from 'lib-common/generated/api-types/reports'
import { ServiceNeedReportRow } from 'lib-common/generated/api-types/reports'
import { ServiceVoucherReport } from 'lib-common/generated/api-types/reports'
import { ServiceVoucherUnitReport } from 'lib-common/generated/api-types/reports'
import { SextetReportRow } from 'lib-common/generated/api-types/reports'
import { SourceUnitsReportRow } from 'lib-common/generated/api-types/reports'
import { StartingPlacementsRow } from 'lib-common/generated/api-types/reports'
import { TitaniaErrorReportRow } from 'lib-common/generated/api-types/reports'
import { UUID } from 'lib-common/types'
import { UnitsReportRow } from 'lib-common/generated/api-types/reports'
import { VardaChildErrorReportRow } from 'lib-common/generated/api-types/reports'
import { VardaUnitErrorReportRow } from 'lib-common/generated/api-types/reports'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonAssistanceNeedDecisionsReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonAttendanceReservationReportByChildGroup } from 'lib-common/generated/api-types/reports'
import { deserializeJsonAttendanceReservationReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonChildAttendanceReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonDuplicatePeopleReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonEndedPlacementsReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonFuturePreschoolersReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonHolidayPeriodAttendanceReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonManualDuplicationReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonMealReportData } from 'lib-common/generated/api-types/reports'
import { deserializeJsonMissingHeadOfFamilyReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonNonSsnChildrenReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonPlacementGuaranteeReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonPlacementSketchingReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonPreschoolApplicationReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonPresenceReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonRawReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonServiceVoucherReport } from 'lib-common/generated/api-types/reports'
import { deserializeJsonServiceVoucherUnitReport } from 'lib-common/generated/api-types/reports'
import { deserializeJsonStartingPlacementsRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonTitaniaErrorReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonVardaChildErrorReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonVardaUnitErrorReportRow } from 'lib-common/generated/api-types/reports'
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
    url: uri`/employee/reports/applications`.toString(),
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
    url: uri`/employee/reports/assistance-need-decisions`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonAssistanceNeedDecisionsReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.AssistanceNeedDecisionsReport.getAssistanceNeedDecisionsReportUnreadCount
*/
export async function getAssistanceNeedDecisionsReportUnreadCount(): Promise<number> {
  const { data: json } = await client.request<JsonOf<number>>({
    url: uri`/employee/reports/assistance-need-decisions/unread-count`.toString(),
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
    url: uri`/employee/reports/assistance-needs-and-actions`.toString(),
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
    url: uri`/employee/reports/assistance-needs-and-actions/by-child`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.AttendanceReservationReportController.getAttendanceReservationReportByChild
*/
export async function getAttendanceReservationReportByChild(
  request: {
    body: AttendanceReservationReportByChildBody
  }
): Promise<AttendanceReservationReportByChildGroup[]> {
  const { data: json } = await client.request<JsonOf<AttendanceReservationReportByChildGroup[]>>({
    url: uri`/employee/reports/attendance-reservation/by-child`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<AttendanceReservationReportByChildBody>
  })
  return json.map(e => deserializeJsonAttendanceReservationReportByChildGroup(e))
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
    url: uri`/employee/reports/attendance-reservation/${request.unitId}`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonAttendanceReservationReportRow(e))
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
    url: uri`/employee/reports/child-age-language`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.ChildAttendanceReportController.getChildAttendanceReport
*/
export async function getChildAttendanceReport(
  request: {
    childId: UUID,
    from: LocalDate,
    to: LocalDate
  }
): Promise<ChildAttendanceReportRow[]> {
  const params = createUrlSearchParams(
    ['from', request.from.formatIso()],
    ['to', request.to.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<ChildAttendanceReportRow[]>>({
    url: uri`/employee/reports/child-attendance/${request.childId}`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonChildAttendanceReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.ChildrenInDifferentAddressReportController.getChildrenInDifferentAddressReport
*/
export async function getChildrenInDifferentAddressReport(): Promise<ChildrenInDifferentAddressReportRow[]> {
  const { data: json } = await client.request<JsonOf<ChildrenInDifferentAddressReportRow[]>>({
    url: uri`/employee/reports/children-in-different-address`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.CustomerFeesReport.getCustomerFeesReport
*/
export async function getCustomerFeesReport(
  request: {
    date: LocalDate,
    areaId?: UUID | null,
    unitId?: UUID | null,
    decisionType: FinanceDecisionType
  }
): Promise<CustomerFeesReportRow[]> {
  const params = createUrlSearchParams(
    ['date', request.date.formatIso()],
    ['areaId', request.areaId],
    ['unitId', request.unitId],
    ['decisionType', request.decisionType.toString()]
  )
  const { data: json } = await client.request<JsonOf<CustomerFeesReportRow[]>>({
    url: uri`/employee/reports/customer-fees`.toString(),
    method: 'GET',
    params
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
    url: uri`/employee/reports/decisions`.toString(),
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
    url: uri`/employee/reports/duplicate-people`.toString(),
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
    url: uri`/employee/reports/ended-placements`.toString(),
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
    url: uri`/employee/reports/exceeded-service-need/rows`.toString(),
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
    url: uri`/employee/reports/exceeded-service-need/units`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.FamilyConflictReportController.getFamilyConflictsReport
*/
export async function getFamilyConflictsReport(): Promise<FamilyConflictReportRow[]> {
  const { data: json } = await client.request<JsonOf<FamilyConflictReportRow[]>>({
    url: uri`/employee/reports/family-conflicts`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.FamilyContactReportController.getFamilyContactsReport
*/
export async function getFamilyContactsReport(
  request: {
    unitId: UUID,
    date: LocalDate
  }
): Promise<FamilyContactReportRow[]> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId],
    ['date', request.date.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<FamilyContactReportRow[]>>({
    url: uri`/employee/reports/family-contacts`.toString(),
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
    url: uri`/employee/reports/family-daycare-meal-count`.toString(),
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
    url: uri`/employee/reports/future-preschoolers`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonFuturePreschoolersReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.FuturePreschoolersReport.getFuturePreschoolersSourceUnitsReport
*/
export async function getFuturePreschoolersSourceUnitsReport(): Promise<SourceUnitsReportRow[]> {
  const { data: json } = await client.request<JsonOf<SourceUnitsReportRow[]>>({
    url: uri`/employee/reports/future-preschoolers/source-units`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.FuturePreschoolersReport.getFuturePreschoolersUnitsReport
*/
export async function getFuturePreschoolersUnitsReport(): Promise<PreschoolUnitsReportRow[]> {
  const { data: json } = await client.request<JsonOf<PreschoolUnitsReportRow[]>>({
    url: uri`/employee/reports/future-preschoolers/units`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.HolidayPeriodAttendanceReport.getHolidayPeriodAttendanceReport
*/
export async function getHolidayPeriodAttendanceReport(
  request: {
    unitId: UUID,
    periodId: UUID
  }
): Promise<HolidayPeriodAttendanceReportRow[]> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId],
    ['periodId', request.periodId]
  )
  const { data: json } = await client.request<JsonOf<HolidayPeriodAttendanceReportRow[]>>({
    url: uri`/employee/reports/holiday-period-attendance`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonHolidayPeriodAttendanceReportRow(e))
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
    url: uri`/employee/reports/invoices`.toString(),
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
    url: uri`/employee/reports/manual-duplication`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonManualDuplicationReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.MealReportController.getMealReportByUnit
*/
export async function getMealReportByUnit(
  request: {
    unitId: UUID,
    date: LocalDate
  }
): Promise<MealReportData> {
  const params = createUrlSearchParams(
    ['date', request.date.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<MealReportData>>({
    url: uri`/employee/reports/meal/${request.unitId}`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonMealReportData(json)
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
    url: uri`/employee/reports/missing-head-of-family`.toString(),
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
    url: uri`/employee/reports/missing-service-need`.toString(),
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
    url: uri`/employee/reports/non-ssn-children`.toString(),
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
    url: uri`/employee/reports/occupancy-by-group`.toString(),
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
    url: uri`/employee/reports/occupancy-by-unit`.toString(),
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
    url: uri`/employee/reports/partners-in-different-address`.toString(),
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
    url: uri`/employee/reports/placement-count`.toString(),
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
    url: uri`/employee/reports/placement-guarantee`.toString(),
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
    url: uri`/employee/reports/placement-sketching`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonPlacementSketchingReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.PreschoolAbsenceReport.getPreschoolAbsenceReport
*/
export async function getPreschoolAbsenceReport(
  request: {
    unitId: UUID,
    groupId?: UUID | null,
    termStart: LocalDate,
    termEnd: LocalDate
  }
): Promise<ChildPreschoolAbsenceRow[]> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId],
    ['groupId', request.groupId],
    ['termStart', request.termStart.formatIso()],
    ['termEnd', request.termEnd.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<ChildPreschoolAbsenceRow[]>>({
    url: uri`/employee/reports/preschool-absence`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.PreschoolApplicationReport.getPreschoolApplicationReport
*/
export async function getPreschoolApplicationReport(): Promise<PreschoolApplicationReportRow[]> {
  const { data: json } = await client.request<JsonOf<PreschoolApplicationReportRow[]>>({
    url: uri`/employee/reports/preschool-application`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonPreschoolApplicationReportRow(e))
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
    url: uri`/employee/reports/presences`.toString(),
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
    url: uri`/employee/reports/raw`.toString(),
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
    url: uri`/employee/reports`.toString(),
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
    url: uri`/employee/reports/service-need`.toString(),
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
    url: uri`/employee/reports/service-voucher-value/units`.toString(),
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
    url: uri`/employee/reports/service-voucher-value/units/${request.unitId}`.toString(),
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
    url: uri`/employee/reports/sextet`.toString(),
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
    url: uri`/employee/reports/starting-placements`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonStartingPlacementsRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.TitaniaErrorReport.getTitaniaErrorsReport
*/
export async function getTitaniaErrorsReport(): Promise<TitaniaErrorReportRow[]> {
  const { data: json } = await client.request<JsonOf<TitaniaErrorReportRow[]>>({
    url: uri`/employee/reports/titania-errors`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonTitaniaErrorReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.UnitsReportController.getUnitsReport
*/
export async function getUnitsReport(): Promise<UnitsReportRow[]> {
  const { data: json } = await client.request<JsonOf<UnitsReportRow[]>>({
    url: uri`/employee/reports/units`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.VardaErrorReport.getVardaChildErrorsReport
*/
export async function getVardaChildErrorsReport(): Promise<VardaChildErrorReportRow[]> {
  const { data: json } = await client.request<JsonOf<VardaChildErrorReportRow[]>>({
    url: uri`/employee/reports/varda-child-errors`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonVardaChildErrorReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.VardaErrorReport.getVardaUnitErrorsReport
*/
export async function getVardaUnitErrorsReport(): Promise<VardaUnitErrorReportRow[]> {
  const { data: json } = await client.request<JsonOf<VardaUnitErrorReportRow[]>>({
    url: uri`/employee/reports/varda-unit-errors`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonVardaUnitErrorReportRow(e))
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
    url: uri`/employee/patu-report`.toString(),
    method: 'POST',
    params
  })
  return json
}
