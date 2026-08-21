// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled, { css } from 'styled-components'

interface Props {
  toggled: boolean
  position: 'top' | 'bottom'
  children: React.ReactNode
  closerToText?: boolean
  'data-qa'?: string
}

export default React.memo(function AttentionIndicator({
  toggled,
  position,
  children,
  closerToText = false,
  'data-qa': dataQa
}: Props) {
  return (
    <Wrapper>
      {children}
      {toggled && (
        <Indicator
          data-qa={dataQa}
          $position={position}
          $closerToText={closerToText}
        />
      )}
    </Wrapper>
  )
})

const Wrapper = styled.div`
  position: relative;
`

const Indicator = styled.div<{
  $position: 'top' | 'bottom'
  $closerToText: boolean
}>`
  position: absolute;
  height: 12px;
  width: 12px;
  ${({ $position, $closerToText }) =>
    $position === 'top'
      ? css`
          top: ${$closerToText ? '6px' : '-2px'};
        `
      : css`
          bottom: ${$closerToText ? '10px' : '2px'};
        `}
  right: ${({ $closerToText }) => ($closerToText ? '4px' : '-6px')};
  border-radius: 6px;
  background: ${(p) => p.theme.colors.status.warning};
`
