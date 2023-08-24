// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import {
  getFamilyContactsReport,
  getMissingHeadOfFamilyReport,
  getOccupanciesReport, getVardaErrorsReport,
  getVoucherServiceProvidersReport,
  MissingHeadOfFamilyReportFilters,
  OccupancyReportFilters,
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
  voucherServiceProviders: (filters: VoucherServiceProvidersFilters) => [
    'voucherServiceProviders',
    filters
  ],
  vardaErrors: () => ['vardaErrors']
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

export const voucherServiceProvidersReportQuery = query({
  api: getVoucherServiceProvidersReport,
  queryKey: queryKeys.voucherServiceProviders
})

export const vardaErrorsQuery = query({
  api: () => getVardaErrorsReport(),
  queryKey: queryKeys.vardaErrors
})
