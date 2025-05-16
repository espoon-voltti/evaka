// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { tabletMin } from '../../breakpoints'

import type { AddButtonProps } from './AddButton'
import AddButton from './AddButton'

interface ResponsiveAddButtonProps extends AddButtonProps {
  breakpoint?: string
}

const ResponsiveText = styled.span<{ breakpoint: string }>`
  @media (max-width: ${(p) => p.breakpoint}) {
    position: absolute;
    left: -10000px;
    top: auto;
    width: 1px;
    height: 1px;
    overflow: hidden;
  }
`

export default React.memo(function ResponsiveAddButton({
  breakpoint = tabletMin,
  text,
  ...props
}: ResponsiveAddButtonProps) {
  return (
    <AddButton
      text={<ResponsiveText breakpoint={breakpoint}>{text}</ResponsiveText>}
      {...props}
    />
  )
})
