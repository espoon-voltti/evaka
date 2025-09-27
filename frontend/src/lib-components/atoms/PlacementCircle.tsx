// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ReactNode } from 'react'
import React from 'react'
import styled from 'styled-components'

import Tooltip from './Tooltip'

const CircleContainer = styled.div<{ $size: number }>`
  width: ${(p) => p.$size}px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
`

const Circle = styled.div<{ $size: number }>`
  width: ${(p) => p.$size}px;
  height: ${(p) => p.$size}px;
  min-width: ${(p) => p.$size}px;
  min-height: ${(p) => p.$size}px;
  max-width: ${(p) => p.$size}px;
  max-height: ${(p) => p.$size}px;
  background-color: ${(p) => p.theme.colors.main.m1};
  border-radius: 100%;
`

const HalfCircle = styled.div<{ $size: number }>`
  width: ${(p) => p.$size / 2}px;
  height: ${(p) => p.$size}px;
  min-width: ${(p) => p.$size / 2}px;
  min-height: ${(p) => p.$size}px;
  max-width: ${(p) => p.$size / 2}px;
  max-height: ${(p) => p.$size}px;
  background-color: ${(p) => p.theme.colors.main.m1};
  border-top-left-radius: ${(p) => p.$size / 2}px;
  border-bottom-left-radius: ${(p) => p.$size / 2}px;
`

type Props = {
  type: 'half' | 'full'
  label: ReactNode
  tooltipUp?: boolean
  size?: number
}

export default React.memo(function PlacementCircle({
  type,
  label,
  tooltipUp,
  size = 34
}: Props) {
  return (
    <Tooltip
      tooltip={<span>{label}</span>}
      position={tooltipUp ? 'top' : 'bottom'}
    >
      <CircleContainer $size={size}>
        {type === 'half' ? (
          <HalfCircle $size={size} />
        ) : (
          <Circle $size={size} />
        )}
      </CircleContainer>
    </Tooltip>
  )
})
