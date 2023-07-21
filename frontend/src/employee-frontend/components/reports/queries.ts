// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import {
  getFamilyContactsReport,
  getMissingHeadOfFamilyReport,
  MissingHeadOfFamilyReportFilters
} from '../../api/reports'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('reports', {
  familyContacts: (unitId: UUID) => ['familyContacts', unitId],
  missingHeadOfFamily: (filters: MissingHeadOfFamilyReportFilters) => [
    'missingHeadOfFamily',
    filters
  ]
})

export const familyContactsReportQuery = query({
  api: getFamilyContactsReport,
  queryKey: queryKeys.familyContacts
})

export const missingHeadOfFamilyReportQuery = query({
  api: getMissingHeadOfFamilyReport,
  queryKey: queryKeys.missingHeadOfFamily
})
