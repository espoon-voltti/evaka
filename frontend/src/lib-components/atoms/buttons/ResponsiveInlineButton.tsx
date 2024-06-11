// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { tabletMin } from '../../breakpoints'

import LegacyInlineButton, { InlineButtonProps } from './LegacyInlineButton'

interface ResponsiveInlineButtonProps extends InlineButtonProps {
  breakpoint?: string
}

const ResponsiveText = styled.span<{ breakpoint: string }>`
  @media (max-width: ${(p) => p.breakpoint}) {
    display: none;
  }
`

export default React.memo(function ResponsiveInlineButton({
  breakpoint = tabletMin,
  text,
  ...props
}: ResponsiveInlineButtonProps) {
  return (
    <LegacyInlineButton
      text={<ResponsiveText breakpoint={breakpoint}>{text}</ResponsiveText>}
      {...props}
    />
  )
})
