// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import {
  getFamilyContactsReport,
  getFuturePreschoolersReport,
  getMissingHeadOfFamilyReport,
  getNonSsnChildrenReport,
  getOccupanciesReport,
  getPlacementGuaranteeReport,
  getPreschoolGroupsReport,
  getUnitsReport,
  getVardaErrorsReport,
  getVoucherServiceProvidersReport,
  MissingHeadOfFamilyReportFilters,
  OccupancyReportFilters,
  PlacementGuaranteeReportFilters,
  resetVardaChild,
  startVardaReset,
  startVardaUpdate,
  VoucherServiceProvidersFilters
} from '../../api/reports'
import {
  getExceededServiceNeedReportRows,
  getExceededServiceNeedReportUnits
} from '../../generated/api-clients/reports'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('reports', {
  exceededServiceNeedsUnits: () => ['exceededServiceNeedsUnits'],
  exceededServiceNeedsRows: (params: {
    unitId: UUID
    year: number
    month: number
  }) => ['exceededServiceNeedsReportRows', params],
  familyContacts: (unitId: UUID) => ['familyContacts', unitId],
  missingHeadOfFamily: (filters: MissingHeadOfFamilyReportFilters) => [
    'missingHeadOfFamily',
    filters
  ],
  occupancies: (filters: OccupancyReportFilters) => ['occupancies', filters],
  placementGuarantee: (filters: PlacementGuaranteeReportFilters) => [
    'placementGuarantee',
    filters
  ],
  voucherServiceProviders: (filters: VoucherServiceProvidersFilters) => [
    'voucherServiceProviders',
    filters
  ],
  vardaErrors: () => ['vardaErrors'],
  futurePreschoolers: () => ['futurePreschoolers'],
  preschoolGroups: () => ['preschoolGroups'],
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
  api: getNonSsnChildrenReport,
  queryKey: () => []
})

export const occupanciesReportQuery = query({
  api: (filters: OccupancyReportFilters) =>
    filters.careAreaId !== null
      ? getOccupanciesReport(filters)
      : Promise.resolve([]),
  queryKey: queryKeys.occupancies
})

export const placementGuaranteeReportQuery = query({
  api: getPlacementGuaranteeReport,
  queryKey: queryKeys.placementGuarantee
})

export const voucherServiceProvidersReportQuery = query({
  api: getVoucherServiceProvidersReport,
  queryKey: queryKeys.voucherServiceProviders
})

export const vardaErrorsQuery = query({
  api: () => getVardaErrorsReport(),
  queryKey: queryKeys.vardaErrors
})

export const startVardaUpdateMutation = mutation({
  api: () => startVardaUpdate(),
  invalidateQueryKeys: () => [queryKeys.vardaErrors()]
})

export const startVardaResetMutation = mutation({
  api: () => startVardaReset(),
  invalidateQueryKeys: () => [queryKeys.vardaErrors()]
})

export const resetVardaChildMutation = mutation({
  api: (arg: { childId: UUID }) => resetVardaChild(arg.childId),
  invalidateQueryKeys: () => [queryKeys.vardaErrors()]
})

export const futurePreschoolersQuery = query({
  api: getFuturePreschoolersReport,
  queryKey: queryKeys.futurePreschoolers
})

export const preschoolGroupsQuery = query({
  api: (municipal: boolean) => getPreschoolGroupsReport(municipal),
  queryKey: queryKeys.preschoolGroups
})

export const unitsReportQuery = query({
  api: getUnitsReport,
  queryKey: queryKeys.units
})
