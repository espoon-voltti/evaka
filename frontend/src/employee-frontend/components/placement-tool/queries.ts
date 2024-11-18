// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { getNextPreschoolTerm } from 'employee-frontend/generated/api-clients/application'
import { createQueryKeys } from 'employee-frontend/query'
import { query } from 'lib-common/query'

const queryKeys = createQueryKeys('placementTool', {
  nextPreschoolTerm: () => ['nextPreschoolTerm']
})

export const nextPreschoolTermQuery = query({
  api: getNextPreschoolTerm,
  queryKey: () => queryKeys.nextPreschoolTerm()
})
