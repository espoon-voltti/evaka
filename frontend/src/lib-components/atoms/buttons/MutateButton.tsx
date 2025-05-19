// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { cancelMutation } from 'lib-common/query'

import { AsyncButton } from './AsyncButton'
import type { BaseButtonVisualProps } from './button-visuals'
import type { MutateButtonBehaviorProps } from './mutate-button-behavior'
import { useMutateButtonBehavior } from './mutate-button-behavior'

export { cancelMutation }

export type MutateButtonProps<Arg, Data> = BaseButtonVisualProps &
  MutateButtonBehaviorProps<Arg, Data> & {
    textInProgress?: string
    textDone?: string
    hideSuccess?: boolean
  }

const MutateButton_ = function MutateButton<Arg, Data>({
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
    <AsyncButton onClick={handleClick} onSuccess={handleSuccess} {...props} />
  )
}

/**
 * An HTML button that triggers a mutation when clicked.
 *
 * Loading/success/failure states are indicated with a spinner or a checkmark/cross icon.
 */
export const MutateButton = React.memo(MutateButton_) as typeof MutateButton_
