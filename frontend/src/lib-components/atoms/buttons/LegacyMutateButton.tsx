// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { cancelMutation, MutationDescription } from 'lib-common/query'

import AsyncButton, { AsyncButtonProps } from './LegacyAsyncButton'
import { useMutateButtonBehavior } from './mutate-button-behavior'

export { cancelMutation }

export interface MutateButtonProps<Arg, Data>
  extends Omit<AsyncButtonProps<unknown>, 'onClick' | 'onSuccess'> {
  mutation: MutationDescription<Arg, Data>
  onClick: () => Arg | typeof cancelMutation
  onSuccess?: (value: Data) => void
}

function LegacyMutateButton<Arg, Data>({
  mutation,
  onClick,
  onSuccess,
  ...props
}: MutateButtonProps<Arg, Data>) {
  const { handleClick, handleSuccess } = useMutateButtonBehavior({
    mutation,
    onClick,
    onSuccess
  })
  return (
    <AsyncButton {...props} onClick={handleClick} onSuccess={handleSuccess} />
  )
}

/**
 * @deprecated use MutateButton instead
 */
export default React.memo(LegacyMutateButton) as typeof LegacyMutateButton
