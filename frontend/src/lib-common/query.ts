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
  UseInfiniteQueryResult,
  InfiniteData,
  QueryClient
} from '@tanstack/react-query'
import { useCallback, useMemo } from 'react'

import { Failure, Loading, Result, Success } from 'lib-common/api'

import { useStableCallback } from './utils/useStableCallback'

export interface QueryDescription<Data, Key extends QueryKey> {
  api: () => Promise<Data>
  queryKey: Key
  queryOptions: UseQueryOptions<Data, unknown, Data, Key> | undefined
}

export function query<
  Args extends unknown[],
  Data,
  Key extends QueryKey
>(opts: {
  api: (...arg: Args) => Promise<Data>
  queryKey: (...arg: Args) => Key
  options?: UseQueryOptions<Data, unknown, Data, Key>
}): Args extends []
  ? QueryDescription<Data, Key>
  : (...arg: Args) => QueryDescription<Data, Key> {
  /* eslint-disable */
  const { api, queryKey, options } = opts
  return (
    api.length === 0
      ? { api, queryKey: (queryKey as any)(), queryOptions: options }
      : (...args: Args) => ({
          api: () => api(...args),
          queryKey: queryKey(...args),
          queryOptions: options
        })
  ) as any
  /* eslint-enable */
}

export function useQuery<Data, Key extends QueryKey>(
  queryDescription: QueryDescription<Data, Key>,
  options?: Omit<
    UseQueryOptions<Data, unknown, Data, Key>,
    'queryKey' | 'queryFn'
  >
): UseQueryResult<Data> {
  const { api, queryKey, queryOptions } = queryDescription
  return useQueryOriginal({
    queryKey,
    queryFn: api,
    ...queryOptions,
    ...options
  })
}

export function queryResult<T>(
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

export function useQueryResult<Data, Key extends QueryKey>(
  queryDescription: QueryDescription<Data, Key>,
  options?: UseQueryOptions<Data, unknown, Data, Key>
): Result<Data> {
  const { data, error, isFetching } = useQuery(queryDescription, options)
  return useMemo(
    () => queryResult(data, error, isFetching),
    [data, error, isFetching]
  )
}

export interface InfiniteQueryDescription<PageArg, Data, Key extends QueryKey> {
  api: (pageArg: PageArg) => Promise<Data>
  queryKey: Key
  firstPageParam: PageArg
  getNextPageParam: (lastPage: Data, pages: Data[]) => PageArg | undefined
  queryOptions?: UseInfiniteQueryOptions<Data, unknown, Data, Data, Key>
}

export function infiniteQuery<
  Args extends unknown[],
  PageArg,
  Data,
  Key extends QueryKey
>(opts: {
  api: (...args: Args) => (pageArg: PageArg) => Promise<Data>
  queryKey: (...arg: Args) => Key
  firstPageParam: PageArg
  getNextPageParam: (lastPage: Data, pages: Data[]) => PageArg | undefined
  options?: UseInfiniteQueryOptions<Data, unknown, Data, Data, Key>
}): Args extends []
  ? InfiniteQueryDescription<PageArg, Data, Key>
  : (...arg: Args) => InfiniteQueryDescription<PageArg, Data, Key> {
  /* eslint-disable */
  const { api, queryKey, firstPageParam, getNextPageParam, options } = opts
  return (
    api.length === 0
      ? {
          api: (api as any)(),
          queryKey: (queryKey as any)(),
          firstPageParam,
          getNextPageParam,
          queryOptions: options
        }
      : (...args: Args) => ({
          api: api(...args),
          queryKey: queryKey(...args),
          firstPageParam,
          getNextPageParam,
          queryOptions: options
        })
  ) as any
  /* eslint-enable */
}

export type UseInfiniteQueryResultWithTransform<Data> =
  UseInfiniteQueryResult<Data> & {
    transformPages: (f: (page: Data, index: number) => Data) => void
  }

export function useInfiniteQuery<PageArg, Data, Key extends QueryKey>(
  queryDescription: InfiniteQueryDescription<PageArg, Data, Key>,
  options?: Omit<
    UseInfiniteQueryOptions<Data, unknown, Data, Data, Key>,
    'queryKey' | 'queryFn' | 'getNextPageParam'
  >
): UseInfiniteQueryResultWithTransform<Data> {
  const { api, queryKey, firstPageParam, getNextPageParam, queryOptions } =
    queryDescription
  const queryClient = useQueryClient()
  const result = useInfiniteQueryOriginal({
    queryKey,
    queryFn: (context: { pageParam?: PageArg }) =>
      api(context.pageParam ?? firstPageParam),
    getNextPageParam,
    ...queryOptions,
    ...options
  })
  const transformPages = useCallback(
    (fn: (page: Data, index: number) => Data) => {
      queryClient.setQueryData<InfiniteData<Data>>(queryKey, (currentData) =>
        currentData
          ? { ...currentData, pages: currentData.pages.map(fn) }
          : undefined
      )
    },
    [queryClient, queryKey]
  )
  return { ...result, transformPages }
}

export interface MutationDescription<Arg, Data, Key extends QueryKey> {
  api: (arg: Arg) => Promise<Data>
  invalidateQueryKeys?: ((arg: Arg) => Key[]) | undefined
}

export function mutation<Arg, Data, Key extends QueryKey>(
  description: MutationDescription<Arg, Data, Key>
): MutationDescription<Arg, Data, Key> {
  return description
}

export async function invalidateDependencies<Arg, Key extends QueryKey>(
  queryClient: QueryClient,
  mutationDescription: MutationDescription<Arg, unknown, Key>,
  arg: Arg
) {
  const { invalidateQueryKeys } = mutationDescription
  if (invalidateQueryKeys) {
    for (const key of invalidateQueryKeys(arg)) {
      await queryClient.invalidateQueries(key)
    }
  }
}

export function useMutation<Arg, Data, Key extends QueryKey>(
  mutationDescription: MutationDescription<Arg, Data, Key>,
  options?: Omit<UseMutationOptions<Data, unknown, Arg>, 'mutationFn'>
): UseMutationResult<Data, unknown, Arg> {
  const { api } = mutationDescription
  const queryClient = useQueryClient()

  return useMutationOriginal(api, {
    ...options,
    onSuccess: async (data, arg, context) => {
      await options?.onSuccess?.(data, arg, context)
      await invalidateDependencies(queryClient, mutationDescription, arg)
    }
  })
}

type Override<T, U> = Omit<T, keyof U> & U

export function useMutationResult<Arg, Data, Key extends QueryKey>(
  mutationDescription: MutationDescription<Arg, Data, Key>,
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

export const cancelMutation: unique symbol = Symbol('cancelMutation')

export type Either<A, B> =
  | { tag: 'first'; value: A }
  | { tag: 'second'; value: B }

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

export function useSelectMutation<
  ArgA,
  DataA,
  KeyA extends QueryKey,
  SelectA,
  ArgB,
  DataB,
  KeyB extends QueryKey,
  SelectB
>(
  select: () => Either<SelectA, SelectB> | typeof cancelMutation,
  firstMutation: [
    MutationDescription<ArgA, DataA, KeyA>,
    (value: SelectA) => ArgA | typeof cancelMutation
  ],
  secondMutation: [
    MutationDescription<ArgB, DataB, KeyB>,
    (value: SelectB) => ArgB | typeof cancelMutation
  ]
): [
  MutationDescription<Either<ArgA, ArgB>, Either<DataA, DataB>, QueryKey>,
  () => Either<ArgA, ArgB> | typeof cancelMutation
] {
  const [mutationA, onClickA] = firstMutation
  const [mutationB, onClickB] = secondMutation
  const mutation = useMemo(
    (): MutationDescription<
      Either<ArgA, ArgB>,
      Either<DataA, DataB>,
      QueryKey
    > => ({
      api: (arg: Either<ArgA, ArgB>): Promise<Either<DataA, DataB>> =>
        arg.tag === 'first'
          ? mutationA.api(arg.value).then((r) => first(r))
          : mutationB.api(arg.value).then((r) => second(r)),
      invalidateQueryKeys: (arg: Either<ArgA, ArgB>): KeyA[] | KeyB[] =>
        arg.tag === 'first'
          ? mutationA.invalidateQueryKeys?.(arg.value) ?? []
          : mutationB.invalidateQueryKeys?.(arg.value) ?? []
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
