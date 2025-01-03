// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  QueryKey,
  useMutation as useMutationOriginal,
  UseMutationOptions,
  UseMutationResult,
  useQuery as useQueryOriginal,
  useInfiniteQuery as useInfiniteQueryOriginal,
  useQueryClient,
  UseQueryOptions,
  UseQueryResult,
  UseInfiniteQueryOptions,
  QueryClient,
  InfiniteData
} from '@tanstack/react-query'
import uniqBy from 'lodash/uniqBy'
import { useCallback, useMemo } from 'react'

import { Failure, Loading, Result, Success } from 'lib-common/api'

import { useStableCallback } from './utils/useStableCallback'

export interface QueriesQuery<Args extends unknown[], Data> {
  (...args: Args): UseQueryOptions<Data, unknown>

  prefix: QueryKey
}

export interface QueriesCustomQuery<QueryArg, Args extends unknown[], Data> {
  (...args: Args): UseQueryOptions<Data, unknown>

  prefix: (arg: QueryArg) => { queryKey: QueryKey }
}

export class Queries {
  commonPrefix: string
  queryNames: Set<string>

  constructor() {
    this.commonPrefix = randomString('queries-')
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
      { prefix: [this.commonPrefix, queryName] }
    )
  }

  /** Query key: `[commonPrefix, queryName, prefix, ...args]`. `prefix` is computed from args */
  prefixedQuery<Args extends unknown[], Data, QueryPrefix>(
    api: (...args: Args) => Promise<Data>,
    prefix: (...args: Args) => QueryPrefix,
    options?: QueryOptions
  ): QueriesCustomQuery<QueryPrefix, Args, Data> {
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
        })
      }
    )
  }

  /** Query key: `[commonPrefix, name, extraArg, ...args]`. `extraArg` is passed from the caller but not used in the api call */
  parametricQuery<ExtraArg>() {
    return <Args extends unknown[], Data>(
      api: (...args: Args) => Promise<Data>,
      options?: QueryOptions
    ) => {
      const queryName = this.getQueryName(api.name)
      return (
        extraArg: ExtraArg,
        ...args: Args
      ): UseQueryOptions<Data, unknown> => ({
        queryFn: () => api(...args),
        queryKey: [this.commonPrefix, queryName, extraArg, ...args],
        ...options
      })
    }
  }

  /** Like `query` but for paged infinite queries */
  pagedInfiniteQuery<Args extends unknown[], Data, Id>(
    api: (...args: Args) => (pageParam: number) => Promise<Paged<Data>>,
    id: (data: Data) => Id,
    options?: QueryOptions
  ): (...args: Args) => PagedInfiniteQueryDescription<Data, Id> {
    const queryName = this.getQueryName(api.name)
    return (...args: Args): PagedInfiniteQueryDescription<Data, Id> => ({
      queryFn: ({ pageParam }) => api(...args)(pageParam),
      queryKey: [this.commonPrefix, queryName, ...args],
      id,
      initialPageParam: 1,
      getNextPageParam: (lastPage, pages) => {
        const nextPage = pages.length + 1
        return nextPage <= lastPage.pages ? nextPage : undefined
      },
      ...options
    })
  }

  mutation<Arg, Data>(
    api: (arg: Arg) => Promise<Data>,
    invalidations?: (((arg: Arg) => { queryKey: QueryKey }) | QueryKey)[]
  ): MutationDescription<Arg, Data> {
    if (invalidations === undefined) {
      return { api }
    } else {
      const invalidationFns = invalidations.map((invalidation) =>
        typeof invalidation === 'function'
          ? (arg: Arg) => invalidation(arg).queryKey
          : () => invalidation
      )
      return {
        api,
        invalidateQueryKeys: (arg: Arg) => invalidationFns.map((f) => f(arg))
      }
    }
  }

  /** Allows passing an additional argument to be used in invalidations but not in the api call (like `parametricQuery`) */
  parametricMutation<MutationArg>() {
    return <Arg, Data>(
      api: (arg: Arg) => Promise<Data>,
      invalidations?: (
        | ((arg: Arg & MutationArg) => { queryKey: QueryKey })
        | QueryKey
      )[]
    ): MutationDescription<Arg & MutationArg, Data> => {
      if (invalidations === undefined) {
        return { api }
      } else {
        const invalidationFns = invalidations.map((invalidation) =>
          typeof invalidation === 'function'
            ? (arg: Arg & MutationArg) => invalidation(arg).queryKey
            : () => invalidation
        )
        return {
          api,
          invalidateQueryKeys: (arg) => invalidationFns.map((fn) => fn(arg))
        }
      }
    }
  }

  private getQueryName(name: string): string {
    if (!name || name === 'anonymous') {
      name = randomString('query-')
    }
    if (this.queryNames.has(name)) {
      throw new Error(`Query name ${name} is already in use`)
    }
    this.queryNames.add(name)
    return name
  }
}

function randomString(prefix = '') {
  return prefix + Math.random().toString(36).substring(2)
}

export interface QueryOptions {
  enabled?: boolean
  refetchOnMount?: boolean | 'always'
  refetchOnWindowFocus?: boolean | 'always'
  staleTime?: number
}

export function query<Args extends unknown[], Data>(opts: {
  api: (...args: Args) => Promise<Data>
  queryKey: (...args: Args) => QueryKey
  options?: QueryOptions
}): (...args: Args) => UseQueryOptions<Data, unknown> {
  const { api, queryKey, options } = opts
  return (...args: Args): UseQueryOptions<Data, unknown> => ({
    queryFn: () => api(...args),
    queryKey: queryKey(...args),
    ...options
  })
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
  Paged<Data>,
  QueryKey,
  number
> & { id: (data: Data) => Id }

export function pagedInfiniteQuery<Args extends unknown[], Data, Id>(opts: {
  api: (...args: Args) => (pageParam: number) => Promise<Paged<Data>>
  queryKey: (...args: Args) => QueryKey
  id: (data: Data) => Id
  options?: QueryOptions
}): (...args: Args) => PagedInfiniteQueryDescription<Data, Id> {
  const { api, queryKey, id, options } = opts
  return (...args: Args): PagedInfiniteQueryDescription<Data, Id> => ({
    queryFn: ({ pageParam }) => api(...args)(pageParam),
    queryKey: queryKey(...args),
    id,
    initialPageParam: 1,
    getNextPageParam: (lastPage, pages) => {
      const nextPage = pages.length + 1
      return nextPage <= lastPage.pages ? nextPage : undefined
    },
    ...options
  })
}

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

export function mutation<Arg, Data>(
  description: MutationDescription<Arg, Data>
): MutationDescription<Arg, Data> {
  return description
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

type Parameters<F> = F extends (...args: infer Args) => unknown ? Args : never

export function queryKeysNamespace<QueryKeyPrefix extends string>() {
  /* eslint-disable */
  return <
    KeyFactories extends Record<string, (...args: any[]) => QueryKey | null>
  >(
    prefix: QueryKeyPrefix,
    obj: KeyFactories
  ): {
    [K in keyof KeyFactories]: (
      ...args: Parameters<KeyFactories[K]>
    ) => QueryKey
  } => {
    return Object.fromEntries(
      Object.entries(obj).map(([key, value]) => [
        key,
        (...args: any[]) => {
          const key = value(...args)
          return key === null ? [prefix] : [prefix, ...key]
        }
      ])
    ) as any
  }
  /* eslint-enable */
}

export function constantQuery<R>(result: R): UseQueryOptions<R, unknown> {
  return {
    queryFn: () => Promise.resolve(result),
    queryKey: ['builtin', 'constant', result],
    initialData: result,
    staleTime: Infinity
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
