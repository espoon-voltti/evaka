import { Failure, Paged, Result, Success } from '../../lib-common/api'
import { JsonOf } from '../../lib-common/json'
import { Employee } from '../types/users'
import { client } from './client'

export async function getUsers(
  page: number,
  pageSize: number
): Promise<Result<Paged<Employee>>> {
  return client
    .get<JsonOf<Paged<Employee>>>('/employee/admin', {
      params: { page, pageSize }
    })
    .then(({ data }) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}
