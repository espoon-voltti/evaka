// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled, { css } from 'styled-components'

interface Props {
  toggled: boolean
  position: 'top' | 'bottom'
  children: React.ReactNode
  'data-qa'?: string
}

export default React.memo(function AttentionIndicator({
  toggled,
  position,
  children,
  'data-qa': dataQa
}: Props) {
  return (
    <Wrapper>
      {children}
      {toggled && <Indicator data-qa={dataQa} position={position} />}
    </Wrapper>
  )
})

const Wrapper = styled.div`
  position: relative;
`

const Indicator = styled.div<{ position: 'top' | 'bottom' }>`
  position: absolute;
  height: 12px;
  width: 12px;
  ${({ position }) =>
    position === 'top'
      ? css`
          top: -2px;
        `
      : css`
          bottom: 2px;
        `}
  right: -6px;
  border-radius: 6px;
  background: ${(p) => p.theme.colors.status.warning};
`
