// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SkipToken } from '@reduxjs/toolkit/dist/query/core/buildSelectors'
import {
  MutationDefinition,
  QueryArgFrom,
  ResultTypeFrom
} from '@reduxjs/toolkit/dist/query/endpointDefinitions'
import {
  UseMutation,
  UseQuery
} from '@reduxjs/toolkit/dist/query/react/buildHooks'
import { QueryDefinition } from '@reduxjs/toolkit/query'
import { BaseQueryFn, createApi } from '@reduxjs/toolkit/query/react'
import { AxiosError, AxiosRequestConfig } from 'axios'
import { useCallback, useMemo } from 'react'

import { Failure, Loading, Result, Success } from 'lib-common/api'

import { client } from '../api-client'

const axiosBaseQuery =
  (): BaseQueryFn<
    | string
    | {
        url: string
        method: AxiosRequestConfig['method']
        body?: AxiosRequestConfig['data']
        params?: AxiosRequestConfig['params']
      },
    unknown,
    unknown
  > =>
  async (urlOrOptions) => {
    const options =
      typeof urlOrOptions === 'string'
        ? { url: urlOrOptions, method: 'GET' }
        : urlOrOptions
    // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
    const { url, method, body, params } = options
    try {
      // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
      const result = await client({ url, method, data: body, params })
      return { data: result.data }
    } catch (axiosError) {
      const err = axiosError as AxiosError
      return {
        error: {
          status: err.response?.status,
          data: err.response?.data || err.message
        }
      }
    }
  }

export const api = createApi({
  reducerPath: 'api',
  baseQuery: axiosBaseQuery(),
  tagTypes: [
    'ChildUnreadPedagogicalDocumentsCount',
    'ChildPedagogicalDocument',
    'ChildUnreadAssistanceNeedDecisionsCount',
    'ChildUnreadVasuDocumentsCount',
    'ChildConsents',
    'ChildConsentNotifications'
  ],
  endpoints: () => ({})
})

export function queryResultHook<
  QueryArg,
  BaseQuery extends BaseQueryFn,
  TagTypes extends string,
  ResultType
>(
  useQuery: UseQuery<QueryDefinition<QueryArg, BaseQuery, TagTypes, ResultType>>
): (
  arg:
    | QueryArgFrom<QueryDefinition<QueryArg, BaseQuery, TagTypes, ResultType>>
    | SkipToken
) => Result<ResultType>
export function queryResultHook<
  QueryArg,
  BaseQuery extends BaseQueryFn,
  TagTypes extends string,
  ResultType,
  TransformedResultType
>(
  useQuery: UseQuery<
    QueryDefinition<QueryArg, BaseQuery, TagTypes, ResultType>
  >,
  transformResult: (data: ResultType) => TransformedResultType
): (
  arg:
    | QueryArgFrom<QueryDefinition<QueryArg, BaseQuery, TagTypes, ResultType>>
    | SkipToken
) => Result<TransformedResultType>
export function queryResultHook<
  QueryArg,
  BaseQuery extends BaseQueryFn,
  TagTypes extends string,
  ResultType,
  TransformedResultType
>(
  useQuery: UseQuery<
    QueryDefinition<QueryArg, BaseQuery, TagTypes, ResultType>
  >,
  transformResult?: (data: ResultType) => TransformedResultType
): (
  arg:
    | QueryArgFrom<QueryDefinition<QueryArg, BaseQuery, TagTypes, ResultType>>
    | SkipToken
) => Result<TransformedResultType> {
  return (arg) => {
    const { data, error, isLoading, isFetching } = useQuery(arg)
    return useMemo(() => {
      if (isLoading) {
        return Loading.of()
      }
      if (error) {
        return Failure.fromError(error)
      }
      if (data) {
        const transformedData = (
          transformResult ? transformResult(data) : data
        ) as TransformedResultType
        const success = Success.of(transformedData)
        if (isFetching) return success.reloading()
        return success
      }
      return Loading.of()
    }, [data, error, isFetching, isLoading])
  }
}

export function mutationResultHook<
  QueryArg,
  BaseQuery extends BaseQueryFn,
  TagTypes extends string,
  ResultType
>(
  useMutation: UseMutation<
    MutationDefinition<QueryArg, BaseQuery, TagTypes, ResultType>
  >
): () => [
  (
    arg: QueryArgFrom<
      MutationDefinition<QueryArg, BaseQuery, TagTypes, ResultType>
    >
  ) => Promise<
    Result<
      ResultTypeFrom<
        MutationDefinition<QueryArg, BaseQuery, TagTypes, ResultType>
      >
    >
  >
] {
  return () => {
    const [mutate] = useMutation()
    const mutateWithResult = useCallback(
      (
        arg: QueryArgFrom<
          MutationDefinition<QueryArg, BaseQuery, TagTypes, ResultType>
        >
      ) =>
        mutate(arg).then(
          (
            result
          ): Result<
            ResultTypeFrom<
              MutationDefinition<QueryArg, BaseQuery, TagTypes, ResultType>
            >
          > => {
            if ('error' in result) {
              return Failure.fromError(result.error)
            }
            return Success.of(result.data)
          }
        ),
      [mutate]
    )
    return [mutateWithResult]
  }
}
