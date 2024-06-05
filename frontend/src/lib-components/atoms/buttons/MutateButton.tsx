// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { cancelMutation, MutationDescription } from 'lib-common/query'

import AsyncButton, { AsyncButtonProps } from './AsyncButton'
import { useMutateButtonBehavior } from './mutate-button-behavior'

export { cancelMutation }

export interface MutateButtonProps<Arg, Data>
  extends Omit<AsyncButtonProps<unknown>, 'onClick' | 'onSuccess'> {
  mutation: MutationDescription<Arg, Data>
  onClick: () => Arg | typeof cancelMutation
  onSuccess?: (value: Data) => void
}

function MutateButton<Arg, Data>({
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

export default React.memo(MutateButton) as typeof MutateButton

export const InlineMutateButton = styled(MutateButton)`
  padding: 0;
  min-width: 0;
  min-height: 0;
  border: none;
` as typeof MutateButton
