// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { Arg0, UUID } from 'lib-common/types'

import {
  getCustomerFeesReport,
  getExceededServiceNeedReportRows,
  getExceededServiceNeedReportUnits,
  getFamilyContactsReport,
  getFuturePreschoolersGroupsReport,
  getFuturePreschoolersReport,
  getMealReportByUnit,
  getMissingHeadOfFamilyReport,
  getNonSsnChildrenReportRows,
  getOccupancyGroupReport,
  getOccupancyUnitReport,
  getPlacementGuaranteeReport,
  getServiceVoucherReportForAllUnits,
  getUnitsReport,
  getVardaChildErrorsReport,
  getVardaUnitErrorsReport
} from '../../generated/api-clients/reports'
import {
  markChildForVardaReset,
  runFullVardaReset,
  runFullVardaUpdate
} from '../../generated/api-clients/varda'
import { createQueryKeys } from '../../query'

import { OccupancyReportFilters } from './Occupancies'

const queryKeys = createQueryKeys('reports', {
  customerFees: (filters: Arg0<typeof getCustomerFeesReport>) => [
    'customerFees',
    filters
  ],
  exceededServiceNeedsUnits: () => ['exceededServiceNeedsUnits'],
  exceededServiceNeedsRows: (params: {
    unitId: UUID
    year: number
    month: number
  }) => ['exceededServiceNeedsReportRows', params],
  familyContacts: (filters: Arg0<typeof getFamilyContactsReport>) => [
    'familyContacts',
    filters
  ],
  missingHeadOfFamily: (filters: Arg0<typeof getMissingHeadOfFamilyReport>) => [
    'missingHeadOfFamily',
    filters
  ],
  occupancies: (filters: OccupancyReportFilters) => ['occupancies', filters],
  placementGuarantee: (filters: Arg0<typeof getPlacementGuaranteeReport>) => [
    'placementGuarantee',
    filters
  ],
  voucherServiceProviders: (
    filters: Arg0<typeof getServiceVoucherReportForAllUnits>
  ) => ['voucherServiceProviders', filters],
  vardaChildErrors: () => ['vardaChildErrors'],
  vardaUnitErrors: () => ['vardaUnitErrors'],
  futurePreschoolers: () => ['futurePreschoolers'],
  futurePreschoolersGroups: () => ['futurePreschoolersGroups'],
  units: () => ['units'],
  mealReportByUnit: (filters: Arg0<typeof getMealReportByUnit>) => [
    'mealReportByUnit',
    filters
  ]
})

export const customerFeesReportQuery = query({
  api: getCustomerFeesReport,
  queryKey: queryKeys.customerFees
})

export const exceededServiceNeedReportUnitsQuery = query({
  api: getExceededServiceNeedReportUnits,
  queryKey: queryKeys.exceededServiceNeedsUnits
})

export const exceededServiceNeedsReportRowsQuery = query({
  api: getExceededServiceNeedReportRows,
  queryKey: queryKeys.exceededServiceNeedsRows
})

export const familyContactsReportQuery = query({
  api: getFamilyContactsReport,
  queryKey: queryKeys.familyContacts
})

export const missingHeadOfFamilyReportQuery = query({
  api: getMissingHeadOfFamilyReport,
  queryKey: queryKeys.missingHeadOfFamily
})

export const nonSsnChildrenReportQuery = query({
  api: getNonSsnChildrenReportRows,
  queryKey: () => []
})

export const occupanciesReportQuery = query({
  api: (filters: OccupancyReportFilters) =>
    filters.careAreaId === null
      ? Promise.resolve([])
      : filters.display === 'UNITS'
        ? getOccupancyUnitReport(filters)
        : getOccupancyGroupReport(filters),
  queryKey: queryKeys.occupancies
})

export const placementGuaranteeReportQuery = query({
  api: getPlacementGuaranteeReport,
  queryKey: queryKeys.placementGuarantee
})

export const voucherServiceProvidersReportQuery = query({
  api: getServiceVoucherReportForAllUnits,
  queryKey: queryKeys.voucherServiceProviders
})

export const vardaChildErrorsQuery = query({
  api: getVardaChildErrorsReport,
  queryKey: queryKeys.vardaChildErrors
})

export const startVardaUpdateMutation = mutation({
  api: runFullVardaUpdate,
  invalidateQueryKeys: () => [queryKeys.vardaChildErrors()]
})

export const startVardaResetMutation = mutation({
  api: runFullVardaReset,
  invalidateQueryKeys: () => [queryKeys.vardaChildErrors()]
})

export const resetVardaChildMutation = mutation({
  api: markChildForVardaReset,
  invalidateQueryKeys: () => [queryKeys.vardaChildErrors()]
})

export const vardaUnitErrorsQuery = query({
  api: getVardaUnitErrorsReport,
  queryKey: queryKeys.vardaUnitErrors
})

export const futurePreschoolersQuery = query({
  api: getFuturePreschoolersReport,
  queryKey: queryKeys.futurePreschoolers
})

export const preschoolGroupsQuery = query({
  api: getFuturePreschoolersGroupsReport,
  queryKey: queryKeys.futurePreschoolersGroups
})

export const unitsReportQuery = query({
  api: getUnitsReport,
  queryKey: queryKeys.units
})

export const mealReportByUnitQuery = query({
  api: getMealReportByUnit,
  queryKey: queryKeys.mealReportByUnit
})
