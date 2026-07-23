// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { tabletMin } from '../../breakpoints'

import type { ButtonProps } from './Button'
import { Button } from './Button'

interface ResponsiveInlineButtonProps extends ButtonProps {
  breakpoint?: string
}

const ResponsiveTextButton = styled(Button)<{ $breakpoint: string }>`
  @media (max-width: ${(p) => p.$breakpoint}) {
    span {
      display: none;
    }
  }
`

export default React.memo(function ResponsiveInlineButton({
  breakpoint = tabletMin,
  ...props
}: ResponsiveInlineButtonProps) {
  return (
    <ResponsiveTextButton
      appearance="inline"
      $breakpoint={breakpoint}
      {...props}
    />
  )
})
