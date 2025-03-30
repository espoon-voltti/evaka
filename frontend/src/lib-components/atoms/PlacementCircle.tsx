// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactNode } from 'react'
import styled from 'styled-components'

import Tooltip from './Tooltip'

const Circle = styled.div<{ $color?: string }>`
  width: 34px;
  height: 34px;
  min-width: 34px;
  min-height: 34px;
  max-width: 34px;
  max-height: 34px;
  background-color: ${(p) => p.$color ?? p.theme.colors.main.m1};
  border-radius: 100%;
`

const HalfCircle = styled.div<{ $color?: string }>`
  width: 17px;
  height: 34px;
  min-width: 17px;
  min-height: 34px;
  max-width: 17px;
  max-height: 34px;
  background-color: ${(p) => p.$color ?? p.theme.colors.main.m1};
  border-top-left-radius: 17px;
  border-bottom-left-radius: 17px;
`

type Props = {
  type: 'half' | 'full'
  label: ReactNode
  tooltipPosition?: 'top' | 'bottom' | 'right' | 'left'
  color?: string
}

export default React.memo(function PlacementCircle({
  type,
  label,
  tooltipPosition,
  color
}: Props) {
  return (
    <Tooltip tooltip={<div>{label}</div>} position={tooltipPosition}>
      {type === 'half' ? (
        <HalfCircle $color={color} />
      ) : (
        <Circle $color={color} />
      )}
    </Tooltip>
  )
})
