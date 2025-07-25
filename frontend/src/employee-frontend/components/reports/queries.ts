// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { sendJamixOrders } from '../../generated/api-clients/jamix'
import { mergePeople, safeDeletePerson } from '../../generated/api-clients/pis'
import {
  clearTitaniaErrors,
  getApplicationsReport,
  getAssistanceNeedDecisionsReport,
  getAssistanceNeedsAndActionsReport,
  getAssistanceNeedsAndActionsReportByChild,
  getAttendanceReservationReportByChild,
  getChildAgeLanguageReport,
  getChildAttendanceReport,
  getChildDocumentDecisionsReport,
  getChildDocumentsReport,
  getChildDocumentsReportTemplateOptions,
  getChildrenInDifferentAddressReport,
  getCitizenDocumentResponseReport,
  getCitizenDocumentResponseReportGroupOptions,
  getCitizenDocumentResponseTemplateOptions,
  getCustomerFeesReport,
  getDecisionsReport,
  getDuplicatePeopleReport,
  getEndedPlacementsReport,
  getExceededServiceNeedReportRows,
  getExceededServiceNeedReportUnits,
  getFamilyConflictsReport,
  getFamilyContactsReport,
  getFamilyDaycareMealReport,
  getFuturePreschoolersReport,
  getFuturePreschoolersSourceUnitsReport,
  getFuturePreschoolersUnitsReport,
  getHolidayPeriodAttendanceReport,
  getHolidayQuestionnaireReport,
  getIncompleteIncomeReport,
  getInvoiceReport,
  getMealReportByUnit,
  getMissingHeadOfFamilyReport,
  getMissingServiceNeedReport,
  getNonSsnChildrenReportRows,
  getOccupancyGroupReport,
  getOccupancyUnitReport,
  getPartnersInDifferentAddressReport,
  getPermittedReports,
  getPlacementCountReport,
  getPlacementGuaranteeReport,
  getPlacementSketchingReport,
  getPreschoolAbsenceReport,
  getPreschoolApplicationReport,
  getPresenceReport,
  getRawReport,
  getServiceNeedReport,
  getServiceVoucherReportForAllUnits,
  getServiceVoucherReportForUnit,
  getSextetReport,
  getStartingPlacementsReport,
  getTampereRegionalSurveyAgeStatistics,
  getTampereRegionalSurveyMonthlyStatistics,
  getTampereRegionalSurveyMunicipalVoucherDistribution,
  getTampereRegionalSurveyYearlyStatistics,
  getTitaniaErrorsReport,
  getUnitsReport,
  getVardaChildErrorsReport,
  getVardaUnitErrorsReport,
  sendPatuReport
} from '../../generated/api-clients/reports'
import { markChildForVardaReset } from '../../generated/api-clients/varda'

const q = new Queries()

export const permittedReportsQuery = q.query(getPermittedReports)

export const assistanceNeedsAndActionsReportQuery = q.query(
  getAssistanceNeedsAndActionsReport
)

export const assistanceNeedsAndActionsReportByChildQuery = q.query(
  getAssistanceNeedsAndActionsReportByChild
)

export const attendanceReservationReportByChildQuery = q.query(
  getAttendanceReservationReportByChild
)

export const childAttendanceReportQuery = q.query(getChildAttendanceReport)

export const customerFeesReportQuery = q.query(getCustomerFeesReport)

export const childDocumentsReportQuery = q.query(getChildDocumentsReport)

export const childDocumentsReportTemplateOptionsQuery = q.query(
  getChildDocumentsReportTemplateOptions
)

export const getCitizenDocumentResponseTemplateOptionsQuery = q.query(
  getCitizenDocumentResponseTemplateOptions
)

export const getCitizenDocumentResponseReportGroupOptionsQuery = q.query(
  getCitizenDocumentResponseReportGroupOptions
)

export const getCitizenDocumentResponseReportQuery = q.query(
  getCitizenDocumentResponseReport
)

export const exceededServiceNeedReportUnitsQuery = q.query(
  getExceededServiceNeedReportUnits
)

export const exceededServiceNeedsReportRowsQuery = q.query(
  getExceededServiceNeedReportRows
)

export const familyContactsReportQuery = q.query(getFamilyContactsReport)

export const invoicesReportQuery = q.query(getInvoiceReport)

export const missingHeadOfFamilyReportQuery = q.query(
  getMissingHeadOfFamilyReport
)

export const nonSsnChildrenReportQuery = q.query(getNonSsnChildrenReportRows)

export const occupancyUnitReportQuery = q.query(getOccupancyUnitReport)
export const occupancyGroupReportQuery = q.query(getOccupancyGroupReport)

export const placementGuaranteeReportQuery = q.query(
  getPlacementGuaranteeReport
)

export const placementSketchingQuery = q.query(getPlacementSketchingReport)

export const voucherServiceProvidersReportQuery = q.query(
  getServiceVoucherReportForAllUnits
)

export const vardaChildErrorsQuery = q.query(getVardaChildErrorsReport)

export const resetVardaChildMutation = q.mutation(markChildForVardaReset, [
  vardaChildErrorsQuery
])

export const vardaUnitErrorsQuery = q.query(getVardaUnitErrorsReport)

export const futurePreschoolersQuery = q.query(getFuturePreschoolersReport)

export const preschoolUnitsQuery = q.query(getFuturePreschoolersUnitsReport)

export const preschoolSourceUnitsQuery = q.query(
  getFuturePreschoolersSourceUnitsReport
)

export const unitsReportQuery = q.query(getUnitsReport)

export const mealReportByUnitQuery = q.query(getMealReportByUnit)

export const preschoolAbsenceReportQuery = q.query(getPreschoolAbsenceReport)

export const preschoolApplicationReportQuery = q.query(
  getPreschoolApplicationReport
)

export const holidayPeriodAttendanceReportQuery = q.query(
  getHolidayPeriodAttendanceReport
)

export const holidayQuestionnaireReportQuery = q.query(
  getHolidayQuestionnaireReport
)

export const sendJamixOrdersMutation = q.mutation(sendJamixOrders)

export const titaniaErrorsReportQuery = q.query(getTitaniaErrorsReport)

export const clearTitaniaErrorMutation = q.mutation(clearTitaniaErrors, [
  titaniaErrorsReportQuery
])

export const incompleteIncomeReportQuery = q.query(getIncompleteIncomeReport)

export const startingPlacementsReportQuery = q.query(
  getStartingPlacementsReport
)

export const sextetReport = q.query(getSextetReport)

export const tampereRegionalSurveyMonthlyReport = q.query(
  getTampereRegionalSurveyMonthlyStatistics
)

export const tampereRegionalSurveyAgeReport = q.query(
  getTampereRegionalSurveyAgeStatistics
)

export const tampereRegionalSurveyYearlyReport = q.query(
  getTampereRegionalSurveyYearlyStatistics
)

export const tampereRegionalSurveyMunicipalVoucherReport = q.query(
  getTampereRegionalSurveyMunicipalVoucherDistribution
)

export const missingServiceNeedReportQuery = q.query(
  getMissingServiceNeedReport
)

export const familyDaycareMealReportQuery = q.query(getFamilyDaycareMealReport)

export const childDocumentDecisionsReportQuery = q.query(
  getChildDocumentDecisionsReport
)

export const decisionReportQuery = q.query(getDecisionsReport)

export const assistanceNeedDecisionsReportQuery = q.query(
  getAssistanceNeedDecisionsReport
)

export const presenceReportQuery = q.query(getPresenceReport)

export const rawReportQuery = q.query(getRawReport)
export const sendPatuReportMutation = q.mutation(sendPatuReport)

export const serviceVoucherReportForUnitQuery = q.query(
  getServiceVoucherReportForUnit
)

export const serviceNeedReportQuery = q.query(getServiceNeedReport)

export const partnersInDifferentAddressReportQuery = q.query(
  getPartnersInDifferentAddressReport
)

export const placementCountReportQuery = q.query(getPlacementCountReport)

export const familyConflictsReportQuery = q.query(getFamilyConflictsReport)

export const endedPlacementsReportQuery = q.query(getEndedPlacementsReport)

export const duplicatePeopleReportQuery = q.query(getDuplicatePeopleReport)

export const mergePeopleMutation = q.mutation(mergePeople, [
  duplicatePeopleReportQuery.prefix
])

export const safeDeletePersonMutation = q.mutation(safeDeletePerson, [
  duplicatePeopleReportQuery.prefix
])

export const childrenInDifferentAddressReportQuery = q.query(
  getChildrenInDifferentAddressReport
)

export const childAgeLanguageReportQuery = q.query(getChildAgeLanguageReport)

export const applicationsReportQuery = q.query(getApplicationsReport)
