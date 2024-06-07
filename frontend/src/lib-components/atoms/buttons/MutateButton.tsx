// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { AsyncButton } from './AsyncButton'
import { BaseButtonVisualProps } from './button-visuals'
import {
  MutateButtonBehaviorProps,
  useMutateButtonBehavior
} from './mutate-button-behavior'

export type MutateButtonProps<Arg, Data> = BaseButtonVisualProps &
  MutateButtonBehaviorProps<Arg, Data> & {
    textInProgress?: string
    textDone?: string
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

export const MutateButton = React.memo(MutateButton_) as typeof MutateButton_
