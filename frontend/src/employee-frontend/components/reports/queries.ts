// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { sendJamixOrders } from '../../generated/api-clients/jamix'
import {
  clearTitaniaErrors,
  getAssistanceNeedDecisionsReport,
  getAssistanceNeedsAndActionsReport,
  getAssistanceNeedsAndActionsReportByChild,
  getAttendanceReservationReportByChild,
  getChildAttendanceReport,
  getChildDocumentsReport,
  getChildDocumentsReportTemplateOptions,
  getCustomerFeesReport,
  getDecisionsReport,
  getExceededServiceNeedReportRows,
  getExceededServiceNeedReportUnits,
  getFamilyContactsReport,
  getFamilyDaycareMealReport,
  getFuturePreschoolersReport,
  getFuturePreschoolersSourceUnitsReport,
  getFuturePreschoolersUnitsReport,
  getHolidayPeriodAttendanceReport,
  getIncompleteIncomeReport,
  getInvoiceReport,
  getManualDuplicationReport,
  getMealReportByUnit,
  getMissingHeadOfFamilyReport,
  getMissingServiceNeedReport,
  getNonSsnChildrenReportRows,
  getOccupancyGroupReport,
  getOccupancyUnitReport,
  getPermittedReports,
  getPlacementGuaranteeReport,
  getPlacementSketchingReport,
  getPreschoolAbsenceReport,
  getPreschoolApplicationReport,
  getPresenceReport,
  getServiceVoucherReportForAllUnits,
  getSextetReport,
  getStartingPlacementsReport,
  getTampereRegionalSurveyAgeStatistics,
  getTampereRegionalSurveyMonthlyStatistics,
  getTampereRegionalSurveyYearlyStatistics,
  getTitaniaErrorsReport,
  getUnitsReport,
  getVardaChildErrorsReport,
  getVardaUnitErrorsReport
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

export const missingServiceNeedReportQuery = q.query(
  getMissingServiceNeedReport
)

export const manualDuplicationReportQuery = q.query(getManualDuplicationReport)

export const familyDaycareMealReportQuery = q.query(getFamilyDaycareMealReport)

export const decisionReportQuery = q.query(getDecisionsReport)

export const assistanceNeedDecisionsReportQuery = q.query(
  getAssistanceNeedDecisionsReport
)

export const presenceReportQuery = q.query(getPresenceReport)
