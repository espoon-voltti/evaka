// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import {
  getFamilyContactsReport,
  getFuturePreschoolersReport,
  getMissingHeadOfFamilyReport,
  getOccupanciesReport,
  getPlacementGuaranteeReport,
  getPreschoolGroupsReport,
  getUnitsReport,
  getVardaErrorsReport,
  getVoucherServiceProvidersReport,
  MissingHeadOfFamilyReportFilters,
  OccupancyReportFilters,
  PlacementGuaranteeReportFilters,
  startVardaUpdate,
  VoucherServiceProvidersFilters
} from '../../api/reports'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('reports', {
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

export const familyContactsReportQuery = query({
  api: getFamilyContactsReport,
  queryKey: queryKeys.familyContacts
})

export const missingHeadOfFamilyReportQuery = query({
  api: getMissingHeadOfFamilyReport,
  queryKey: queryKeys.missingHeadOfFamily
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
  invalidateQueryKeys: () => []
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
