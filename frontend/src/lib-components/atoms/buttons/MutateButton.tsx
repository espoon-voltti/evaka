// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { QueryKey, useMutation, useQueryClient } from '@tanstack/react-query'
import React, { useCallback } from 'react'

import { Failure, Result, Success } from 'lib-common/api'
import { invalidateDependencies, MutationDescription } from 'lib-common/query'

import AsyncButton, { AsyncButtonProps } from './AsyncButton'

export const cancelMutation: unique symbol = Symbol('cancelMutation')

export interface MutateButtonProps<Arg, Data, Key extends QueryKey>
  extends Omit<AsyncButtonProps<unknown>, 'onClick' | 'onSuccess'> {
  mutation: MutationDescription<Arg, Data, Key>
  onClick: () => Arg | typeof cancelMutation
  onSuccess?: (value: Data) => void
}

function MutateButton<Arg, Data, Key extends QueryKey>({
  mutation,
  onClick,
  onSuccess,
  ...props
}: MutateButtonProps<Arg, Data, Key>) {
  const { api } = mutation
  const queryClient = useQueryClient()

  const { mutateAsync } = useMutation(api)

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

  return (
    <AsyncButton {...props} onClick={handleClick} onSuccess={handleSuccess} />
  )
}

export default React.memo(MutateButton) as typeof MutateButton
