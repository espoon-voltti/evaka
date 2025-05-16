// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { FocusEventHandler } from 'react'
import styled, { css } from 'styled-components'

export interface DropdownProps<T, E extends HTMLElement> {
  id?: string
  items: readonly T[]
  selectedItem: T | null
  onChange: (item: T | null) => void
  disabled?: boolean
  placeholder?: string
  getItemLabel?: (item: T) => string
  getItemDataQa?: (item: T) => string | undefined
  name?: string
  onFocus?: FocusEventHandler<E>
  fullWidth?: boolean
  'data-qa'?: string
}

export const borderRadius = '2px'

export const Root = styled.div`
  max-width: 500px;
  border-radius: ${borderRadius};
  border: 2px solid transparent;

  &.active {
    border-color: ${(p) => p.theme.colors.main.m2Active};
  }
  &.full-width {
    width: 100%;
    max-width: initial;
  }
`

export const borderStyles = css`
  border: 1px solid ${(p) => p.theme.colors.grayscale.g70};
  border-radius: ${borderRadius};
`
