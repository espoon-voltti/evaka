// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { Arg0, UUID } from 'lib-common/types'

import {
  getExceededServiceNeedReportRows,
  getExceededServiceNeedReportUnits,
  getFamilyContactsReport,
  getFuturePreschoolersGroupsReport,
  getFuturePreschoolersReport,
  getMissingHeadOfFamilyReport,
  getNonSsnChildrenReportRows,
  getOccupancyGroupReport,
  getOccupancyUnitReport,
  getPlacementGuaranteeReport,
  getServiceVoucherReportForAllUnits,
  getUnitsReport,
  getVardaErrorsReport
} from '../../generated/api-clients/reports'
import {
  markChildForVardaReset,
  runFullVardaReset,
  runFullVardaUpdate
} from '../../generated/api-clients/varda'
import { createQueryKeys } from '../../query'

import { OccupancyReportFilters } from './Occupancies'

const queryKeys = createQueryKeys('reports', {
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
  vardaErrors: () => ['vardaErrors'],
  futurePreschoolers: () => ['futurePreschoolers'],
  futurePreschoolersGroups: () => ['futurePreschoolersGroups'],
  units: () => ['units']
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

export const vardaErrorsQuery = query({
  api: getVardaErrorsReport,
  queryKey: queryKeys.vardaErrors
})

export const startVardaUpdateMutation = mutation({
  api: runFullVardaUpdate,
  invalidateQueryKeys: () => [queryKeys.vardaErrors()]
})

export const startVardaResetMutation = mutation({
  api: runFullVardaReset,
  invalidateQueryKeys: () => [queryKeys.vardaErrors()]
})

export const resetVardaChildMutation = mutation({
  api: markChildForVardaReset,
  invalidateQueryKeys: () => [queryKeys.vardaErrors()]
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
