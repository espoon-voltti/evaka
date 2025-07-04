// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  QueryKey,
  UseMutationOptions,
  UseMutationResult,
  UseQueryOptions,
  UseQueryResult,
  UseInfiniteQueryOptions,
  QueryClient,
  InfiniteData
} from '@tanstack/react-query'
import {
  useMutation as useMutationOriginal,
  useQuery as useQueryOriginal,
  useInfiniteQuery as useInfiniteQueryOriginal,
  useQueryClient
} from '@tanstack/react-query'
import uniqBy from 'lodash/uniqBy'
import { useCallback, useMemo } from 'react'

import type { Result } from 'lib-common/api'
import { Failure, Loading, Success } from 'lib-common/api'

import { useStableCallback } from './utils/useStableCallback'

const isQuery: unique symbol = Symbol('isQuery')

export interface QueriesQuery<Args extends unknown[], Data> {
  (...args: Args): UseQueryOptions<Data, unknown>

  prefix: QueryKey
  [isQuery]: true
}

export interface PrefixedQueriesQuery<QueryArg, Args extends unknown[], Data> {
  (...args: Args): UseQueryOptions<Data, unknown>

  prefix: (arg: QueryArg) => { queryKey: QueryKey }
  [isQuery]: true
}

export interface PagedInfiniteQueriesQuery<Args extends unknown[], Data, Id> {
  (...args: Args): PagedInfiniteQueryDescription<Data, Id>

  prefix: QueryKey
  [isQuery]: true
}

export type MutationOf<T> =
  T extends MutationDescription<infer Arg, unknown> ? Arg : never

export type MutateAsyncFn<M> = ReturnType<
  typeof useMutationResult<MutationOf<M>, void>
>['mutateAsync']

export class Queries {
  commonPrefix: string
  queryNames: Set<string>

  constructor() {
    this.commonPrefix = uniqueString('queries-')
    this.queryNames = new Set()
  }

  /** Query key: `[commonPrefix, queryName, ...args]` */
  query<Args extends unknown[], Data>(
    api: (...args: Args) => Promise<Data>,
    options?: QueryOptions
  ): QueriesQuery<Args, Data> {
    const queryName = this.getQueryName(api.name)
    return Object.assign(
      (...args: Args): UseQueryOptions<Data, unknown> => ({
        queryFn: () => api(...args),
        queryKey: [this.commonPrefix, queryName, ...args],
        ...options
      }),
      { prefix: [this.commonPrefix, queryName], [isQuery]: true as const }
    )
  }

  /** Query key: `[commonPrefix, queryName, prefix, ...args]`. `prefix` is computed from args */
  prefixedQuery<Args extends unknown[], Data, QueryPrefix>(
    api: (...args: Args) => Promise<Data>,
    prefix: (...args: Args) => QueryPrefix,
    options?: QueryOptions
  ): PrefixedQueriesQuery<QueryPrefix, Args, Data> {
    const queryName = this.getQueryName(api.name)
    return Object.assign(
      (...args: Args): UseQueryOptions<Data, unknown> => ({
        queryFn: () => api(...args),
        queryKey: [this.commonPrefix, queryName, prefix(...args), ...args],
        ...options
      }),
      {
        prefix: (prefix: QueryPrefix) => ({
          queryKey: [this.commonPrefix, queryName, prefix]
        }),
        [isQuery]: true as const
      }
    )
  }

  /** Query key: `[commonPrefix, name, extraArg, ...args]`. `extraArg` is passed from the caller but not used in the api call */
  parametricQuery<ExtraArg>() {
    return <Args extends unknown[], Data>(
      api: (...args: Args) => Promise<Data>,
      options?: QueryOptions
    ): QueriesQuery<[ExtraArg, ...Args], Data> => {
      const queryName = this.getQueryName(api.name)
      return Object.assign(
        (
          extraArg: ExtraArg,
          ...args: Args
        ): UseQueryOptions<Data, unknown> => ({
          queryFn: () => api(...args),
          queryKey: [this.commonPrefix, queryName, extraArg, ...args],
          ...options
        }),
        { prefix: [this.commonPrefix, queryName], [isQuery]: true as const }
      )
    }
  }

  /** Like `query` but for paged infinite queries */
  pagedInfiniteQuery<Args extends unknown[], Data, Id>(
    api: (...args: Args) => (pageParam: number) => Promise<Paged<Data>>,
    id: (data: Data) => Id,
    options?: QueryOptions
  ): PagedInfiniteQueriesQuery<Args, Data, Id> {
    const queryName = this.getQueryName(api.name)
    return Object.assign(
      (...args: Args): PagedInfiniteQueryDescription<Data, Id> => ({
        queryFn: ({ pageParam }) => api(...args)(pageParam),
        queryKey: [this.commonPrefix, queryName, ...args],
        id,
        initialPageParam: 1,
        getNextPageParam: (lastPage, pages) => {
          const nextPage = pages.length + 1
          return nextPage <= lastPage.pages ? nextPage : undefined
        },
        ...options
      }),
      { prefix: [this.commonPrefix, queryName], [isQuery]: true as const }
    )
  }

  mutation<Arg, Data>(
    api: (arg: Arg) => Promise<Data>,
    invalidations?: Invalidations<Arg>
  ): MutationDescription<Arg, Data> {
    return { api, invalidateQueryKeys: invalidateQueryKeysFn(invalidations) }
  }

  /** Allows passing an additional argument to be used in invalidations but not in the api call (like `parametricQuery`) */
  parametricMutation<MutationArg>() {
    return <Arg, Data>(
      api: (arg: Arg) => Promise<Data>,
      invalidations?: Invalidations<Arg & MutationArg>
    ): MutationDescription<Arg & MutationArg, Data> => {
      return {
        api,
        invalidateQueryKeys: invalidateQueryKeysFn(invalidations)
      }
    }
  }

  private getQueryName(name: string): string {
    if (!name || name === 'anonymous') {
      name = uniqueString('query-')
    }
    if (this.queryNames.has(name)) {
      throw new Error(`Query name ${name} is already in use`)
    }
    this.queryNames.add(name)
    return name
  }
}

let counter = 0

function uniqueString(prefix: string): string {
  return `${prefix}${counter++}`
}

type Invalidations<Arg> = (
  | ((arg: Arg) => { queryKey: QueryKey })
  | QueriesQuery<[], unknown>
  | QueryKey
)[]

function invalidateQueryKeysFn<Arg>(
  invalidations: Invalidations<Arg> | undefined
): ((arg: Arg) => QueryKey[]) | undefined {
  if (invalidations === undefined) {
    return undefined
  }

  const invalidationFns = invalidations.map((invalidation) =>
    isQuery in invalidation
      ? // If the invalidation is a query, do not pass any arguments to it. Typings ensure that a query can be used as
        // an invalidation only if it has no arguments. The arguments are added to the query key, so passing the
        // *mutation's* argument would result in the wrong query key being computed.
        () => invalidation().queryKey
      : typeof invalidation === 'function'
        ? // If the invalidation is some other function, it will compute the query key based on the mutation's argument.
          (arg: Arg) => invalidation(arg).queryKey
        : // Otherwise, the invalidation is already query key and we can just return it.
          () => invalidation
  )
  return (arg: Arg) => invalidationFns.map((f) => f(arg))
}

export interface QueryOptions {
  enabled?: boolean
  refetchOnMount?: boolean | 'always'
  refetchOnWindowFocus?: boolean | 'always'
  staleTime?: number
  refetchInterval?: number
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type AnyUseQueryOptions = UseQueryOptions<any, unknown>

type DataOf<T extends AnyUseQueryOptions> =
  T extends UseQueryOptions<infer Data, unknown> ? Data : never

export function useQuery<T extends AnyUseQueryOptions>(
  query: T,
  options?: QueryOptions
): UseQueryResult<DataOf<T>, unknown> {
  return useQueryOriginal({ ...query, ...options })
}

function queryResult<T>(
  data: T | undefined,
  error: unknown,
  isFetching: boolean
): Result<T> {
  if (data !== undefined) {
    const result = Success.of(data)
    return isFetching ? result.reloading() : result
  }
  if (error) {
    return Failure.fromError(error)
  }
  return Loading.of()
}

export function useQueryResult<T extends AnyUseQueryOptions>(
  query: T,
  options?: QueryOptions
): Result<DataOf<T>> {
  const { data, error, isFetching } = useQuery(query, options)
  return useMemo(
    () => queryResult(data, error, isFetching),
    [data, error, isFetching]
  )
}

function foreverPending(): Promise<never> {
  return new Promise(() => undefined)
}

export function useChainedQuery<T extends AnyUseQueryOptions>(
  query: Result<T>,
  options?: QueryOptions
): Result<DataOf<T>> {
  const result = useQueryResult(
    // Use a promise that never resolves
    query.getOrElse({ queryFn: foreverPending }) as T,
    {
      ...options,
      enabled: (options?.enabled ?? true) && query.isSuccess
    }
  )
  return query.isFailure ? Failure.of(query) : result
}

type Paged<T> = {
  data: T[]
  pages: number
  total: number
}

type PagedInfiniteQueryDescription<Data, Id> = UseInfiniteQueryOptions<
  Paged<Data>,
  unknown,
  InfiniteData<Paged<Data>>,
  QueryKey,
  number
> & { id: (data: Data) => Id }

export interface PagedInfiniteQueryResult<Data> {
  data: Result<Data[]>
  hasNextPage: boolean
  fetchNextPage: () => void
  transform: (f: (data: Data) => Data) => void
}

export function usePagedInfiniteQueryResult<Data, Id>(
  queryDescription: PagedInfiniteQueryDescription<Data, Id>,
  options?: QueryOptions
): PagedInfiniteQueryResult<Data> {
  const { id, queryKey, ...queryOptions } = queryDescription
  const {
    data,
    error,
    isFetching,
    isFetchingNextPage,
    hasNextPage,
    fetchNextPage
  } = useInfiniteQueryOriginal({ queryKey, ...queryOptions, ...options })

  const isFetchingFirstPage = isFetching && !isFetchingNextPage
  const result = useMemo(
    () =>
      // Use .map() to only call uniqBy/flatMap when it's a Success
      queryResult(null, error, isFetchingFirstPage).map(() =>
        data
          ? uniqBy(
              data.pages.flatMap((p) => p.data),
              id
            )
          : []
      ),
    [data, error, isFetchingFirstPage, id]
  )

  const maybeFetchNextPage = useCallback(() => {
    if (!isFetchingNextPage && hasNextPage) {
      void fetchNextPage()
    }
  }, [isFetchingNextPage, hasNextPage, fetchNextPage])

  const queryClient = useQueryClient()
  const transform = useCallback(
    (f: (data: Data) => Data) => {
      queryClient.setQueryData<InfiniteData<Paged<Data>>>(
        queryKey,
        (currentData) =>
          currentData
            ? {
                ...currentData,
                pages: currentData.pages.map((page) => ({
                  ...page,
                  data: page.data.map(f)
                }))
              }
            : undefined
      )
    },
    [queryClient, queryKey]
  )
  return {
    data: result,
    hasNextPage,
    fetchNextPage: maybeFetchNextPage,
    transform
  }
}

export interface MutationDescription<Arg, Data> {
  api: (arg: Arg) => Promise<Data>
  invalidateQueryKeys?: ((arg: Arg) => QueryKey[]) | undefined
}

export async function invalidateDependencies<Arg>(
  queryClient: QueryClient,
  mutationDescription: MutationDescription<Arg, unknown>,
  arg: Arg
) {
  const { invalidateQueryKeys } = mutationDescription
  if (invalidateQueryKeys) {
    for (const key of invalidateQueryKeys(arg)) {
      await queryClient.invalidateQueries({ queryKey: key })
    }
  }
}

export function useMutation<Arg, Data>(
  mutationDescription: MutationDescription<Arg, Data>,
  options?: Omit<UseMutationOptions<Data, unknown, Arg>, 'mutationFn'>
): UseMutationResult<Data, unknown, Arg> {
  const { api } = mutationDescription
  const queryClient = useQueryClient()

  return useMutationOriginal({
    mutationFn: api,
    ...options,
    onSuccess: async (data, arg, context) => {
      await options?.onSuccess?.(data, arg, context)
      await invalidateDependencies(queryClient, mutationDescription, arg)
    }
  })
}

type Override<T, U> = Omit<T, keyof U> & U

export function useMutationResult<Arg, Data>(
  mutationDescription: MutationDescription<Arg, Data>,
  options?: Omit<UseMutationOptions<Data, unknown, Arg>, 'mutationFn'>
): Override<
  UseMutationResult<Data, unknown, Arg>,
  { mutateAsync: (arg: Arg) => Promise<Result<Data>> }
> {
  const { mutateAsync, ...rest } = useMutation(mutationDescription, options)
  const mutateAsyncResult = useCallback(
    (arg: Arg): Promise<Result<Data>> =>
      mutateAsync(arg)
        .then((res) => Success.of(res))
        .catch((e) => Failure.fromError(e)),
    [mutateAsync]
  )
  return { ...rest, mutateAsync: mutateAsyncResult }
}

/**
 * Immediately resolves with the given constant value
 */
export function constantQuery<R>(value: R): UseQueryOptions<R, unknown> {
  return {
    queryFn: () => Promise.resolve(value),
    queryKey: ['builtin', 'constant', value],
    initialData: value,
    staleTime: Infinity
  }
}

/**
 * Stays in the pending state forever
 */
export function pendingQuery<R>(): UseQueryOptions<R, unknown> {
  return {
    queryFn: foreverPending,
    queryKey: ['builtin', 'pending']
  }
}

export const cancelMutation: unique symbol = Symbol('cancelMutation')

export type Either<A, B> =
  | {
      tag: 'first'
      value: A
    }
  | {
      tag: 'second'
      value: B
    }

export function first<A>(value: A): Either<A, never>
export function first(): Either<void, never>
export function first<A>(value?: A): Either<A | void, never> {
  return { tag: 'first', value }
}

export function second<B>(value: B): Either<never, B>
export function second(): Either<never, void>
export function second<B>(value?: B): Either<never, B | void> {
  return { tag: 'second', value }
}

export function useSelectMutation<ArgA, DataA, SelectA, ArgB, DataB, SelectB>(
  select: () => Either<SelectA, SelectB> | typeof cancelMutation,
  firstMutation: [
    MutationDescription<ArgA, DataA>,
    (value: SelectA) => ArgA | typeof cancelMutation
  ],
  secondMutation: [
    MutationDescription<ArgB, DataB>,
    (value: SelectB) => ArgB | typeof cancelMutation
  ]
): [
  MutationDescription<Either<ArgA, ArgB>, Either<DataA, DataB>>,
  () => Either<ArgA, ArgB> | typeof cancelMutation
] {
  const [mutationA, onClickA] = firstMutation
  const [mutationB, onClickB] = secondMutation
  const mutation = useMemo(
    (): MutationDescription<Either<ArgA, ArgB>, Either<DataA, DataB>> => ({
      api: (arg: Either<ArgA, ArgB>): Promise<Either<DataA, DataB>> =>
        arg.tag === 'first'
          ? mutationA.api(arg.value).then((r) => first(r))
          : mutationB.api(arg.value).then((r) => second(r)),
      invalidateQueryKeys: (arg: Either<ArgA, ArgB>): QueryKey[] =>
        arg.tag === 'first'
          ? (mutationA.invalidateQueryKeys?.(arg.value) ?? [])
          : (mutationB.invalidateQueryKeys?.(arg.value) ?? [])
    }),
    [mutationA, mutationB]
  )

  const select_ = useStableCallback(select)
  const onClickA_ = useStableCallback(onClickA)
  const onClickB_ = useStableCallback(onClickB)

  const onClick = useCallback(():
    | Either<ArgA, ArgB>
    | typeof cancelMutation => {
    const selection = select_()
    if (selection === cancelMutation) return cancelMutation
    if (selection.tag === 'first') {
      const value = onClickA_(selection.value)
      return value === cancelMutation ? cancelMutation : first(value)
    } else {
      const value = onClickB_(selection.value)
      return value === cancelMutation ? cancelMutation : second(value)
    }
  }, [onClickA_, onClickB_, select_])

  return [mutation, onClick]
}
