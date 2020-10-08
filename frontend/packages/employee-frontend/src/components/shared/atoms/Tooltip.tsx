// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { Greyscale } from '~components/shared/Colors'
import { DefaultMargins } from '~components/shared/layout/white-space'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { fasCaretUp } from '~icon-set'

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
  left: -70px;
  right: -70px;
  z-index: 99999;
  display: flex;
  justify-content: center;
  align-items: center;
`

const TooltipDiv = styled.div`
  color: ${Greyscale.white};
  font-size: 15px;
  line-height: 22px;

  background-color: ${Greyscale.dark};
  padding: ${DefaultMargins.s};
  border-radius: 2px;
  box-shadow: 0 4px 4px rgba(0, 0, 0, 0.25);

  p:not(:last-child) {
    margin-bottom: 8px;
  }
`

const Beak = styled.div`
  position: absolute;
  top: -24px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  color: ${Greyscale.dark};
`

interface TooltipProps {
  children: React.ReactNode
  tooltip: JSX.Element
}

export default function Tooltip({ children, tooltip }: TooltipProps) {
  return (
    <TooltipWrapper>
      <div>{children}</div>

      <TooltipPositioner className={'tooltip'}>
        <TooltipDiv>
          <Beak>
            <FontAwesomeIcon icon={fasCaretUp} size={'3x'} />
          </Beak>
          <div>{tooltip}</div>
        </TooltipDiv>
      </TooltipPositioner>
    </TooltipWrapper>
  )
}
