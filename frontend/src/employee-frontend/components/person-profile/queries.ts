// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { query } from 'lib-common/query'

import { getIncomeCoefficientMultipliers } from '../../api/income'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('personProfile', {
  incomeCoefficientMultipliers: () => ['incomeCoefficientMultipliers']
})

export const incomeCoefficientMultipliersQuery = query({
  api: getIncomeCoefficientMultipliers,
  queryKey: queryKeys.incomeCoefficientMultipliers
})
