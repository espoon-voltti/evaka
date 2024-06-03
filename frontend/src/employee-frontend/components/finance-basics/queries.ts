// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'

import {
  createVoucherValue,
  getVoucherValues,
  updateVoucherValue
} from '../../generated/api-clients/invoicing'
import { deleteVoucherValue } from '../../generated/api-clients/invoicing'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('financeBasics', {
  voucherValues: () => ['voucherValues']
})

export const voucherValuesQuery = query({
  api: getVoucherValues,
  queryKey: queryKeys.voucherValues
})

export const createVoucherValueMutation = mutation({
  api: createVoucherValue,
  invalidateQueryKeys: () => [queryKeys.voucherValues()]
})

export const updateVoucherValueMutation = mutation({
  api: updateVoucherValue,
  invalidateQueryKeys: () => [queryKeys.voucherValues()]
})

export const deleteVoucherValueMutation = mutation({
  api: deleteVoucherValue,
  invalidateQueryKeys: () => [queryKeys.voucherValues()]
})
