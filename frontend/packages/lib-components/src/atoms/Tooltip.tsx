// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { fasCaretDown, fasCaretUp } from '@evaka/lib-icons'
import { greyscale } from '../colors'
import { defaultMargins } from '../white-space'
import classNames from 'classnames'

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
  top: calc(100% + ${defaultMargins.xs});
  left: -70px;
  right: -70px;
  z-index: 99999;
  display: flex;
  justify-content: center;
  align-items: center;

  &.up {
    top: auto;
    bottom: calc(100% + ${defaultMargins.xs});
  }
`

const TooltipDiv = styled.div`
  color: ${greyscale.white};
  font-size: 15px;
  line-height: 22px;

  background-color: ${greyscale.dark};
  padding: ${defaultMargins.s};
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
  color: ${greyscale.dark};

  &.up {
    top: auto;
    bottom: -24px;
  }
`

type TooltipProps = {
  children: React.ReactNode
  tooltip: JSX.Element
  up?: boolean
}

export default function Tooltip({ children, tooltip, up }: TooltipProps) {
  return (
    <TooltipWrapper>
      <div>{children}</div>

      <TooltipPositioner className={classNames('tooltip', { up })}>
        <TooltipDiv>
          <Beak className={classNames({ up })}>
            <FontAwesomeIcon
              icon={up ? fasCaretDown : fasCaretUp}
              size={'3x'}
            />
          </Beak>
          <div>{tooltip}</div>
        </TooltipDiv>
      </TooltipPositioner>
    </TooltipWrapper>
  )
}
