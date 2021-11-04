import { client } from '../api-client'
import { User } from './state'
import { Failure, Result, Success } from 'lib-common/api'

export type AuthStatus = { loggedIn: false } | { loggedIn: true; user: User }

export function getAuthStatus(): Promise<Result<AuthStatus>> {
  return client
    .get<AuthStatus>('/auth/status')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
