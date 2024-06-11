// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useCallback } from 'react'

import { Failure, Result, Success } from 'lib-common/api'
import {
  cancelMutation,
  invalidateDependencies,
  MutationDescription
} from 'lib-common/query'

import { AsyncButtonBehaviorProps } from './async-button-behavior'

/**
 * Behavioral props for a mutate button.
 */
export interface MutateButtonBehaviorProps<Arg, Data>
  extends Omit<
    AsyncButtonBehaviorProps<{ value: Data; arg: Arg }>,
    'onClick' | 'onSuccess'
  > {
  mutation: MutationDescription<Arg, Data>
  onClick: () => Arg | typeof cancelMutation
  onSuccess?: (value: Data) => void
}

/**
 * A hook that provides the behavior for a mutate button.
 *
 * The returned raw handlers should be passed to an async button's onClick and onSuccess props.
 */
export function useMutateButtonBehavior<Arg, Data>({
  mutation,
  onClick,
  onSuccess
}: MutateButtonBehaviorProps<Arg, Data>) {
  const { api } = mutation
  const queryClient = useQueryClient()

  const { mutateAsync } = useMutation({ mutationFn: api })

  const handleClick = useCallback(():
    | Promise<Result<{ value: Data; arg: Arg }>>
    | undefined => {
    const arg = onClick()
    if (arg === cancelMutation) {
      return undefined
    }
    return mutateAsync(arg)
      .then((value) => Success.of({ value, arg }))
      .catch((error) => Failure.fromError(error))
  }, [mutateAsync, onClick])

  const handleSuccess = useCallback(
    ({ value, arg }: { value: Data; arg: Arg }) => {
      // Don't wait for the dependencies to load before running onSuccess
      void invalidateDependencies(queryClient, mutation, arg)
      onSuccess?.(value)
    },
    [mutation, onSuccess, queryClient]
  )

  return { handleClick, handleSuccess }
}
