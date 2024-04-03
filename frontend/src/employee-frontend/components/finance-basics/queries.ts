// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { query } from 'lib-common/query'

import { getVoucherValues } from '../../generated/api-clients/invoicing'
import { getServiceNeedOptions } from '../../generated/api-clients/serviceneed'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('financeBasics', {
  serviceNeeds: () => ['serviceNeeds'],
  voucherValues: () => ['voucherValues']
})

export const serviceNeedsQuery = query({
  api: getServiceNeedOptions,
  queryKey: queryKeys.serviceNeeds
})

export const voucherValuesQuery = query({
  api: getVoucherValues,
  queryKey: queryKeys.voucherValues
})
