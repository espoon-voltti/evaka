// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  createFeeThresholds,
  createVoucherValue,
  getFeeThresholds,
  getVoucherValues,
  updateFeeThresholds,
  updateVoucherValue
} from '../../generated/api-clients/invoicing'
import { deleteVoucherValue } from '../../generated/api-clients/invoicing'

const q = new Queries()

export const voucherValuesQuery = q.query(getVoucherValues)

export const createVoucherValueMutation = q.mutation(createVoucherValue, [
  voucherValuesQuery
])

export const updateVoucherValueMutation = q.mutation(updateVoucherValue, [
  voucherValuesQuery
])

export const deleteVoucherValueMutation = q.mutation(deleteVoucherValue, [
  voucherValuesQuery
])

export const feeThresholdsQuery = q.query(getFeeThresholds)

export const createFeeThresholdsMutation = q.mutation(createFeeThresholds, [
  feeThresholdsQuery.prefix
])

export const updateFeeThresholdsMutation = q.mutation(updateFeeThresholds, [
  feeThresholdsQuery.prefix
])
