// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { Greyscale } from '~components/shared/Colors'
import { DefaultMargins } from '~components/shared/layout/white-space'

const TooltipWrapper = styled.div`
  position: relative;
  display: inline-block;

  &:not(:hover) {
    .tooltip {
      display: none;
    }
  }
`
const TooltipPositioner = styled.div`
  position: absolute;
  top: calc(100% + ${DefaultMargins.xs});
  left: 0;
  right: 0;
  z-index: 99999;
  display: flex;
  justify-content: center;
  align-items: center;
`

const TooltipDiv = styled.div`
  background-color: ${Greyscale.dark}bb;
  color: ${Greyscale.white};
  padding: ${DefaultMargins.s};
  border-radius: 4px;
  text-align: center;
`

interface TooltipProps {
  children: React.ReactNode
  tooltipText: string
}

export default function Tooltip({ children, tooltipText }: TooltipProps) {
  return (
    <TooltipWrapper>
      {children}
      <TooltipPositioner className={'tooltip'}>
        <TooltipDiv>{tooltipText}</TooltipDiv>
      </TooltipPositioner>
    </TooltipWrapper>
  )
}
