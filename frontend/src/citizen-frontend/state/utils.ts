// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { createSelector } from '@reduxjs/toolkit'
import { SkipToken } from '@reduxjs/toolkit/dist/query/core/buildSelectors'
import {
  MutationDefinition,
  QueryArgFrom,
  ResultTypeFrom
} from '@reduxjs/toolkit/dist/query/endpointDefinitions'
import { BaseQueryFn } from '@reduxjs/toolkit/dist/query/react'
import {
  UseMutation,
  UseQuery,
  UseQueryHookResult
} from '@reduxjs/toolkit/dist/query/react/buildHooks'
import { QueryDefinition } from '@reduxjs/toolkit/query'
import { useCallback } from 'react'

import { Failure, Loading, Result, Success } from 'lib-common/api'

export function queryResultHook<
  QueryArg,
  BaseQuery extends BaseQueryFn,
  TagTypes extends string,
  ResultType,
  Deserialized
>(
  useQuery: UseQuery<
    QueryDefinition<QueryArg, BaseQuery, TagTypes, ResultType>
  >,
  deserialize: (data: ResultType) => Deserialized
): (
  arg:
    | QueryArgFrom<QueryDefinition<QueryArg, BaseQuery, TagTypes, ResultType>>
    | SkipToken
) => Result<Deserialized>
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
  Deserialized
>(
  useQuery: UseQuery<
    QueryDefinition<QueryArg, BaseQuery, TagTypes, ResultType>
  >,
  deserialize?: (data: ResultType) => Deserialized
): (
  arg:
    | QueryArgFrom<QueryDefinition<QueryArg, BaseQuery, TagTypes, ResultType>>
    | SkipToken
) => Result<Deserialized> {
  type QueryResult = UseQueryHookResult<
    QueryDefinition<QueryArg, BaseQuery, TagTypes, ResultType>
  >
  const deserializeSelector = createSelector(
    (queryResult: QueryResult) => queryResult.data,
    (data) => (data && deserialize ? deserialize(data) : data)
  )
  const resultSelector = createSelector(
    deserializeSelector,
    (queryResult: QueryResult) => queryResult.error,
    (queryResult: QueryResult) => queryResult.isLoading,
    (queryResult: QueryResult) => queryResult.isFetching,
    (data, error, isLoading, isFetching): Result<Deserialized> => {
      if (data) {
        const success = Success.of(data as Deserialized)
        if (isFetching) return success.reloading()
        return success
      }
      if (error) {
        return Failure.fromError(error)
      }
      return Loading.of()
    }
  )

  return (arg) => {
    return resultSelector(useQuery(arg))
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
      async (
        arg: QueryArgFrom<
          MutationDefinition<QueryArg, BaseQuery, TagTypes, ResultType>
        >
      ): Promise<
        Result<
          ResultTypeFrom<
            MutationDefinition<QueryArg, BaseQuery, TagTypes, ResultType>
          >
        >
      > => {
        const result = await mutate(arg)
        if ('error' in result) {
          return Failure.fromError(result.error)
        }
        return Success.of(result.data)
      },
      [mutate]
    )
    return [mutateWithResult]
  }
}
