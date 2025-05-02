// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import YearMonth from 'lib-common/year-month'
import { ApplicationStatus } from 'lib-common/generated/api-types/application'
import { ApplicationsReportRow } from 'lib-common/generated/api-types/reports'
import { AreaId } from 'lib-common/generated/api-types/shared'
import { AssistanceNeedDecisionsReportRow } from 'lib-common/generated/api-types/reports'
import { AssistanceNeedsAndActionsReport } from 'lib-common/generated/api-types/reports'
import { AssistanceNeedsAndActionsReportByChild } from 'lib-common/generated/api-types/reports'
import { AttendanceReservationReportByChildBody } from 'lib-common/generated/api-types/reports'
import { AttendanceReservationReportByChildGroup } from 'lib-common/generated/api-types/reports'
import { AttendanceReservationReportRow } from 'lib-common/generated/api-types/reports'
import { CareType } from 'lib-common/generated/api-types/daycare'
import { ChildAgeLanguageReportRow } from 'lib-common/generated/api-types/reports'
import { ChildAttendanceReportRow } from 'lib-common/generated/api-types/reports'
import { ChildDocumentOrDecisionStatus } from 'lib-common/generated/api-types/document'
import { ChildDocumentSummary } from 'lib-common/generated/api-types/document'
import { ChildDocumentsReportTemplate } from 'lib-common/generated/api-types/reports'
import { ChildPreschoolAbsenceRow } from 'lib-common/generated/api-types/reports'
import { ChildrenInDifferentAddressReportRow } from 'lib-common/generated/api-types/reports'
import { CitizenDocumentResponseReportRow } from 'lib-common/generated/api-types/reports'
import { CitizenDocumentResponseReportTemplate } from 'lib-common/generated/api-types/reports'
import { CustomerFeesReportRow } from 'lib-common/generated/api-types/reports'
import { DaycareAssistanceLevel } from 'lib-common/generated/api-types/assistance'
import { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import { DaycareId } from 'lib-common/generated/api-types/shared'
import { DecisionsReportRow } from 'lib-common/generated/api-types/reports'
import { DocumentTemplateId } from 'lib-common/generated/api-types/shared'
import { DuplicatePeopleReportRow } from 'lib-common/generated/api-types/reports'
import { EndedPlacementsReportRow } from 'lib-common/generated/api-types/reports'
import { ExceededServiceNeedReportRow } from 'lib-common/generated/api-types/reports'
import { ExceededServiceNeedReportUnit } from 'lib-common/generated/api-types/reports'
import { FamilyConflictReportRow } from 'lib-common/generated/api-types/reports'
import { FamilyContactReportRow } from 'lib-common/generated/api-types/reports'
import { FamilyDaycareMealReportResult } from 'lib-common/generated/api-types/reports'
import { FinanceDecisionType } from 'lib-common/generated/api-types/invoicing'
import { FuturePreschoolersReportRow } from 'lib-common/generated/api-types/reports'
import { GroupId } from 'lib-common/generated/api-types/shared'
import { HolidayPeriodAttendanceReportRow } from 'lib-common/generated/api-types/reports'
import { HolidayPeriodId } from 'lib-common/generated/api-types/shared'
import { IncompleteIncomeDbRow } from 'lib-common/generated/api-types/reports'
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
import { OtherAssistanceMeasureType } from 'lib-common/generated/api-types/assistance'
import { PartnersInDifferentAddressReportRow } from 'lib-common/generated/api-types/reports'
import { PersonId } from 'lib-common/generated/api-types/shared'
import { PlacementCountReportResult } from 'lib-common/generated/api-types/reports'
import { PlacementGuaranteeReportRow } from 'lib-common/generated/api-types/reports'
import { PlacementSketchingReportRow } from 'lib-common/generated/api-types/reports'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import { PreschoolApplicationReportRow } from 'lib-common/generated/api-types/reports'
import { PreschoolAssistanceLevel } from 'lib-common/generated/api-types/assistance'
import { PreschoolUnitsReportRow } from 'lib-common/generated/api-types/reports'
import { PresenceReportRow } from 'lib-common/generated/api-types/reports'
import { ProviderType } from 'lib-common/generated/api-types/daycare'
import { RawReportRow } from 'lib-common/generated/api-types/reports'
import { RegionalSurveyReportAgeStatisticsResult } from 'lib-common/generated/api-types/reports'
import { RegionalSurveyReportResult } from 'lib-common/generated/api-types/reports'
import { RegionalSurveyReportYearlyStatisticsResult } from 'lib-common/generated/api-types/reports'
import { Report } from 'lib-common/generated/api-types/reports'
import { ReservationType } from 'lib-common/generated/api-types/reports'
import { ServiceNeedReportRow } from 'lib-common/generated/api-types/reports'
import { ServiceVoucherReport } from 'lib-common/generated/api-types/reports'
import { ServiceVoucherUnitReport } from 'lib-common/generated/api-types/reports'
import { SextetReportRow } from 'lib-common/generated/api-types/reports'
import { SourceUnitsReportRow } from 'lib-common/generated/api-types/reports'
import { StartingPlacementsRow } from 'lib-common/generated/api-types/reports'
import { TitaniaErrorReportRow } from 'lib-common/generated/api-types/reports'
import { TitaniaErrorsId } from 'lib-common/generated/api-types/shared'
import { UnitRow } from 'lib-common/generated/api-types/reports'
import { UnitsReportRow } from 'lib-common/generated/api-types/reports'
import { VardaChildErrorReportRow } from 'lib-common/generated/api-types/reports'
import { VardaUnitErrorReportRow } from 'lib-common/generated/api-types/reports'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonAssistanceNeedDecisionsReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonAttendanceReservationReportByChildGroup } from 'lib-common/generated/api-types/reports'
import { deserializeJsonAttendanceReservationReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonChildAttendanceReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonChildDocumentSummary } from 'lib-common/generated/api-types/document'
import { deserializeJsonCitizenDocumentResponseReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonDaycareGroup } from 'lib-common/generated/api-types/daycare'
import { deserializeJsonDuplicatePeopleReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonEndedPlacementsReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonFuturePreschoolersReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonHolidayPeriodAttendanceReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonIncompleteIncomeDbRow } from 'lib-common/generated/api-types/reports'
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
    date: LocalDate,
    daycareAssistanceLevels?: DaycareAssistanceLevel[] | null,
    preschoolAssistanceLevels?: PreschoolAssistanceLevel[] | null,
    otherAssistanceMeasureTypes?: OtherAssistanceMeasureType[] | null
  }
): Promise<AssistanceNeedsAndActionsReport> {
  const params = createUrlSearchParams(
    ['date', request.date.formatIso()],
    ...(request.daycareAssistanceLevels?.map((e): [string, string | null | undefined] => ['daycareAssistanceLevels', e.toString()]) ?? []),
    ...(request.preschoolAssistanceLevels?.map((e): [string, string | null | undefined] => ['preschoolAssistanceLevels', e.toString()]) ?? []),
    ...(request.otherAssistanceMeasureTypes?.map((e): [string, string | null | undefined] => ['otherAssistanceMeasureTypes', e.toString()]) ?? [])
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
    date: LocalDate,
    daycareAssistanceLevels?: DaycareAssistanceLevel[] | null,
    preschoolAssistanceLevels?: PreschoolAssistanceLevel[] | null,
    otherAssistanceMeasureTypes?: OtherAssistanceMeasureType[] | null
  }
): Promise<AssistanceNeedsAndActionsReportByChild> {
  const params = createUrlSearchParams(
    ['date', request.date.formatIso()],
    ...(request.daycareAssistanceLevels?.map((e): [string, string | null | undefined] => ['daycareAssistanceLevels', e.toString()]) ?? []),
    ...(request.preschoolAssistanceLevels?.map((e): [string, string | null | undefined] => ['preschoolAssistanceLevels', e.toString()]) ?? []),
    ...(request.otherAssistanceMeasureTypes?.map((e): [string, string | null | undefined] => ['otherAssistanceMeasureTypes', e.toString()]) ?? [])
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
    unitId: DaycareId,
    start: LocalDate,
    end: LocalDate,
    groupIds?: GroupId[] | null,
    reservationType: ReservationType
  }
): Promise<AttendanceReservationReportRow[]> {
  const params = createUrlSearchParams(
    ['start', request.start.formatIso()],
    ['end', request.end.formatIso()],
    ...(request.groupIds?.map((e): [string, string | null | undefined] => ['groupIds', e]) ?? []),
    ['reservationType', request.reservationType.toString()]
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
    childId: PersonId,
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
* Generated from fi.espoo.evaka.reports.ChildDocumentDecisionsReportController.getChildDocumentDecisionsReport
*/
export async function getChildDocumentDecisionsReport(
  request: {
    statuses?: ChildDocumentOrDecisionStatus[] | null
  }
): Promise<ChildDocumentSummary[]> {
  const params = createUrlSearchParams(
    ...(request.statuses?.map((e): [string, string | null | undefined] => ['statuses', e.toString()]) ?? [])
  )
  const { data: json } = await client.request<JsonOf<ChildDocumentSummary[]>>({
    url: uri`/employee/reports/child-document-decisions`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonChildDocumentSummary(e))
}


/**
* Generated from fi.espoo.evaka.reports.ChildDocumentDecisionsReportController.getChildDocumentDecisionsReportNotificationCount
*/
export async function getChildDocumentDecisionsReportNotificationCount(): Promise<number> {
  const { data: json } = await client.request<JsonOf<number>>({
    url: uri`/employee/reports/child-document-decisions/notification-count`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.ChildDocumentsReport.getChildDocumentsReport
*/
export async function getChildDocumentsReport(
  request: {
    templateIds?: DocumentTemplateId[] | null,
    unitIds?: DaycareId[] | null
  }
): Promise<UnitRow[]> {
  const params = createUrlSearchParams(
    ...(request.templateIds?.map((e): [string, string | null | undefined] => ['templateIds', e]) ?? []),
    ...(request.unitIds?.map((e): [string, string | null | undefined] => ['unitIds', e]) ?? [])
  )
  const { data: json } = await client.request<JsonOf<UnitRow[]>>({
    url: uri`/employee/reports/child-documents`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.ChildDocumentsReport.getChildDocumentsReportTemplateOptions
*/
export async function getChildDocumentsReportTemplateOptions(): Promise<ChildDocumentsReportTemplate[]> {
  const { data: json } = await client.request<JsonOf<ChildDocumentsReportTemplate[]>>({
    url: uri`/employee/reports/child-documents/template-options`.toString(),
    method: 'GET'
  })
  return json
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
* Generated from fi.espoo.evaka.reports.CitizenDocumentResponseReport.getCitizenDocumentResponseReport
*/
export async function getCitizenDocumentResponseReport(
  request: {
    unitId: DaycareId,
    groupId: GroupId,
    documentTemplateId: DocumentTemplateId
  }
): Promise<CitizenDocumentResponseReportRow[]> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId],
    ['groupId', request.groupId],
    ['documentTemplateId', request.documentTemplateId]
  )
  const { data: json } = await client.request<JsonOf<CitizenDocumentResponseReportRow[]>>({
    url: uri`/employee/reports/citizen-document-response-report`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonCitizenDocumentResponseReportRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.CitizenDocumentResponseReport.getCitizenDocumentResponseReportGroupOptions
*/
export async function getCitizenDocumentResponseReportGroupOptions(
  request: {
    unitId: DaycareId,
    from?: LocalDate | null,
    to?: LocalDate | null
  }
): Promise<DaycareGroup[]> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId],
    ['from', request.from?.formatIso()],
    ['to', request.to?.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<DaycareGroup[]>>({
    url: uri`/employee/reports/citizen-document-response-report/group-options`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonDaycareGroup(e))
}


/**
* Generated from fi.espoo.evaka.reports.CitizenDocumentResponseReport.getCitizenDocumentResponseTemplateOptions
*/
export async function getCitizenDocumentResponseTemplateOptions(): Promise<CitizenDocumentResponseReportTemplate[]> {
  const { data: json } = await client.request<JsonOf<CitizenDocumentResponseReportTemplate[]>>({
    url: uri`/employee/reports/citizen-document-response-report/template-options`.toString(),
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
    areaId?: AreaId | null,
    unitId?: DaycareId | null,
    decisionType: FinanceDecisionType,
    providerType?: ProviderType | null,
    placementType?: PlacementType | null
  }
): Promise<CustomerFeesReportRow[]> {
  const params = createUrlSearchParams(
    ['date', request.date.formatIso()],
    ['areaId', request.areaId],
    ['unitId', request.unitId],
    ['decisionType', request.decisionType.toString()],
    ['providerType', request.providerType?.toString()],
    ['placementType', request.placementType?.toString()]
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
    unitId: DaycareId,
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
    unitId: DaycareId,
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
    groupIds?: GroupId[] | null,
    unitId: DaycareId,
    periodId: HolidayPeriodId
  }
): Promise<HolidayPeriodAttendanceReportRow[]> {
  const params = createUrlSearchParams(
    ...(request.groupIds?.map((e): [string, string | null | undefined] => ['groupIds', e]) ?? []),
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
* Generated from fi.espoo.evaka.reports.IncompleteIncomeReport.getIncompleteIncomeReport
*/
export async function getIncompleteIncomeReport(): Promise<IncompleteIncomeDbRow[]> {
  const { data: json } = await client.request<JsonOf<IncompleteIncomeDbRow[]>>({
    url: uri`/employee/reports/incomplete-income`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonIncompleteIncomeDbRow(e))
}


/**
* Generated from fi.espoo.evaka.reports.InvoiceReportController.getInvoiceReport
*/
export async function getInvoiceReport(
  request: {
    yearMonth: YearMonth
  }
): Promise<InvoiceReport> {
  const params = createUrlSearchParams(
    ['yearMonth', request.yearMonth.formatIso()]
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
    unitId: DaycareId,
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
    careAreaId?: AreaId | null,
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
    careAreaId?: AreaId | null,
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
    unitId?: DaycareId | null
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
    unitId: DaycareId,
    groupId?: GroupId | null,
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
    date: LocalDate,
    areaId?: AreaId | null,
    providerType?: ProviderType | null,
    placementType?: PlacementType | null
  }
): Promise<ServiceNeedReportRow[]> {
  const params = createUrlSearchParams(
    ['date', request.date.formatIso()],
    ['areaId', request.areaId],
    ['providerType', request.providerType?.toString()],
    ['placementType', request.placementType?.toString()]
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
    areaId?: AreaId | null
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
    unitId: DaycareId,
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
    placementType: PlacementType,
    unitProviderTypes?: ProviderType[] | null
  }
): Promise<SextetReportRow[]> {
  const params = createUrlSearchParams(
    ['year', request.year.toString()],
    ['placementType', request.placementType.toString()],
    ...(request.unitProviderTypes?.map((e): [string, string | null | undefined] => ['unitProviderTypes', e.toString()]) ?? [])
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
* Generated from fi.espoo.evaka.reports.TampereRegionalSurvey.getTampereRegionalSurveyAgeStatistics
*/
export async function getTampereRegionalSurveyAgeStatistics(
  request: {
    year: number
  }
): Promise<RegionalSurveyReportAgeStatisticsResult> {
  const params = createUrlSearchParams(
    ['year', request.year.toString()]
  )
  const { data: json } = await client.request<JsonOf<RegionalSurveyReportAgeStatisticsResult>>({
    url: uri`/employee/reports/tampere-regional-survey/age-statistics`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.TampereRegionalSurvey.getTampereRegionalSurveyMonthlyStatistics
*/
export async function getTampereRegionalSurveyMonthlyStatistics(
  request: {
    year: number
  }
): Promise<RegionalSurveyReportResult> {
  const params = createUrlSearchParams(
    ['year', request.year.toString()]
  )
  const { data: json } = await client.request<JsonOf<RegionalSurveyReportResult>>({
    url: uri`/employee/reports/tampere-regional-survey/monthly-statistics`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.TampereRegionalSurvey.getTampereRegionalSurveyYearlyStatistics
*/
export async function getTampereRegionalSurveyYearlyStatistics(
  request: {
    year: number
  }
): Promise<RegionalSurveyReportYearlyStatisticsResult> {
  const params = createUrlSearchParams(
    ['year', request.year.toString()]
  )
  const { data: json } = await client.request<JsonOf<RegionalSurveyReportYearlyStatisticsResult>>({
    url: uri`/employee/reports/tampere-regional-survey/yearly-statistics`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.reports.TitaniaErrorReport.clearTitaniaErrors
*/
export async function clearTitaniaErrors(
  request: {
    conflictId: TitaniaErrorsId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/reports/titania-errors/${request.conflictId}`.toString(),
    method: 'DELETE'
  })
  return json
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
