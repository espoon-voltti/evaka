// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { transparentize } from 'polished'
import React from 'react'
import styled from 'styled-components'
import { spinnerOverlayZIndex } from '../../layout/z-helpers'
import { defaultMargins, SpacingSize } from '../../white-space'

const spinnerSize = '50px'

export const Spinner = styled.div<{ zIndex?: number }>`
  border-radius: 50%;
  width: ${spinnerSize};
  height: ${spinnerSize};
  ${({ zIndex }) => (zIndex ? `z-index: ${zIndex};` : '')}

  border: 5px solid ${(p) => transparentize(0.8, p.theme.colors.main.m2)};
  border-left-color: ${(p) => p.theme.colors.main.m2};
  animation: spin 1.1s infinite linear;

  &:after {
    border-radius: 50%;
    width: ${spinnerSize};
    height: ${spinnerSize};
  }

  @keyframes spin {
    0% {
      transform: rotate(0deg);
    }
    100% {
      transform: rotate(360deg);
    }
  }
`

interface SpinnerWrapperProps {
  size: string
}

const SpinnerWrapper = styled.div<SpinnerWrapperProps>`
  margin: ${(p) => p.size} 0;
  display: flex;
  justify-content: center;
  align-items: center;
`

interface SpinnerSegmentProps {
  size?: SpacingSize
}

export const SpinnerSegment = React.memo(function SpinnerSegment({
  size = 'm'
}: SpinnerSegmentProps) {
  return (
    <SpinnerWrapper size={defaultMargins[size]}>
      <Spinner />
    </SpinnerWrapper>
  )
})

const SpinnerOverlayRoot = styled.div`
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  bottom: 0;
  min-height: 80px;
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  opacity: 0.8;
  z-index: ${spinnerOverlayZIndex};
  display: flex;
  align-items: center;
  justify-content: center;
`

export const SpinnerOverlay = React.memo(function SpinnerOverlay() {
  return (
    <SpinnerOverlayRoot>
      <Spinner />
    </SpinnerOverlayRoot>
  )
})

const Relative = styled.div`
  position: relative;
`

export const LoadableContent = React.memo(function LoadableContent({
  loading,
  children
}: {
  loading: boolean
  children: React.ReactNode
}) {
  return (
    <Relative>
      {loading && <SpinnerOverlay />}
      {children}
    </Relative>
  )
})

export default Spinner
