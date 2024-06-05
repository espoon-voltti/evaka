// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { AsyncIconOnlyButton } from './AsyncIconOnlyButton'
import { IconOnlyButtonVisualProps } from './icon-only-button-visuals'
import {
  MutateButtonBehaviorProps,
  useMutateButtonBehavior
} from './mutate-button-behavior'

export type MutateIconOnlyButtonProps<Arg, Data> = IconOnlyButtonVisualProps &
  MutateButtonBehaviorProps<Arg, Data>

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

export const MutateIconOnlyButton = React.memo(
  MutateIconOnlyButton_
) as typeof MutateIconOnlyButton_
