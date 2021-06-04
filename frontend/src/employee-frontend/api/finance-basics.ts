// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import { JsonOf } from 'lib-common/json'
import { client } from './client'
import { FeeThresholdsWithId, FeeThresholds } from '../types/finance-basics'

export async function getFeeThresholds(): Promise<
  Result<FeeThresholdsWithId[]>
> {
  return client
    .get<JsonOf<FeeThresholdsWithId[]>>('/finance-basics/fee-thresholds')
    .then((res) =>
      Success.of(
        res.data.map((json) => ({
          ...json,
          thresholds: {
            ...json.thresholds,
            validDuring: DateRange.parseJson(json.thresholds.validDuring)
          }
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

export async function createFeeThresholds(
  thresholds: FeeThresholds
): Promise<Result<void>> {
  try {
    const response = await client.post(
      '/finance-basics/fee-thresholds',
      thresholds
    )
    return Success.of(response.data)
  } catch (e) {
    return Failure.fromError(e)
  }
}
