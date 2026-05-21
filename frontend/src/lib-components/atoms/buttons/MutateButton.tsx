// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { shade } from 'polished'
import React from 'react'
import styled from 'styled-components'

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

/**
 * A MutateButton styled in the danger palette. Use as `primary` to indicate
 * an irreversible action (e.g. delete confirmation).
 */
export const DangerMutateButton = styled(MutateButton)`
  &.primary {
    background: ${(p) => p.theme.colors.status.danger};
    border-color: ${(p) => p.theme.colors.status.danger};

    &:hover {
      background: ${(p) => shade(0.1, p.theme.colors.status.danger)};
      border-color: ${(p) => shade(0.1, p.theme.colors.status.danger)};
    }

    &:active {
      background: ${(p) => shade(0.2, p.theme.colors.status.danger)};
      border-color: ${(p) => shade(0.2, p.theme.colors.status.danger)};
    }
  }
` as typeof MutateButton
