import { Failure, Result, Success } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import { JsonOf } from 'lib-common/json'
import { client } from './client'
import { Pricing } from '../types/finance-basics'

export async function getPricing(): Promise<Result<Pricing[]>> {
  return client
    .get<JsonOf<Pricing[]>>('/finance-basics/pricing')
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
