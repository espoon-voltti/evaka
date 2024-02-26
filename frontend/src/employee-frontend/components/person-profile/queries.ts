// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { query } from 'lib-common/query'

import { getIncomeMultipliers } from '../../generated/api-clients/invoicing'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('personProfile', {
  incomeCoefficientMultipliers: () => ['incomeCoefficientMultipliers']
})

export const incomeCoefficientMultipliersQuery = query({
  api: getIncomeMultipliers,
  queryKey: queryKeys.incomeCoefficientMultipliers
})
