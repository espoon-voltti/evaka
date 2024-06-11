// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { AsyncIconOnlyButton } from './AsyncIconOnlyButton'
import { BaseIconOnlyButtonVisualProps } from './icon-only-button-visuals'
import {
  MutateButtonBehaviorProps,
  useMutateButtonBehavior
} from './mutate-button-behavior'

export type MutateIconOnlyButtonProps<Arg, Data> =
  BaseIconOnlyButtonVisualProps &
    MutateButtonBehaviorProps<Arg, Data> & {
      /**
       * If true, the success icon is hidden.
       */
      hideSuccess?: boolean
    }

const MutateIconOnlyButton_ = function MutateIconOnlyButton<Arg, Data>({
  mutation,
  onClick,
  onSuccess,
  ...props
}: MutateIconOnlyButtonProps<Arg, Data>) {
  const { handleClick, handleSuccess } = useMutateButtonBehavior({
    mutation,
    onClick,
    onSuccess
  })
  return (
    <AsyncIconOnlyButton
      onClick={handleClick}
      onSuccess={handleSuccess}
      {...props}
    />
  )
}

/**
 * An HTML button that looks like an icon and triggers a mutation when clicked.
 *
 * Loading/success/failure states are indicated by temporarily replacing the default icon with a spinner or a checkmark/cross icon.
 */
export const MutateIconOnlyButton = React.memo(
  MutateIconOnlyButton_
) as typeof MutateIconOnlyButton_
