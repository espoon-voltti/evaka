import { Failure, Result, Success } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import { JsonOf } from 'lib-common/json'
import { client } from './client'
import { FeeThresholds } from '../types/finance-basics'

export async function getFeeThresholds(): Promise<Result<FeeThresholds[]>> {
  return client
    .get<JsonOf<FeeThresholds[]>>('/finance-basics/fee-thresholds')
    .then((res) =>
      Success.of(
        res.data.map((json) => ({
          ...json,
          validDuring: DateRange.parseJson(json.validDuring)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}
