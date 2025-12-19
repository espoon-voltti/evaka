// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import React from 'react'
import styled from 'styled-components'

import { faTimes } from 'lib-icons'

import RoundIcon from '../atoms/RoundIcon'
import { IconOnlyButton } from '../atoms/buttons/IconOnlyButton'
import { modalZIndex } from '../layout/z-helpers'
import { defaultMargins } from '../white-space'

const toastWidthPx = '360px'
const toastBreakpoint = `calc(${toastWidthPx} + ${defaultMargins.s} * 2 + 16px)`

export interface Props {
  icon: IconDefinition
  iconColor: string
  onClick?: () => void
  onClose?: () => void
  children?: React.ReactNode
  'data-qa'?: string
  closeLabel: string
}

export default React.memo(function Toast({
  icon,
  iconColor,
  onClick,
  onClose,
  children,
  'data-qa': dataQa,
  closeLabel
}: Props) {
  function handleOnClose(e: React.MouseEvent<HTMLButtonElement>) {
    onClose?.()
    e.stopPropagation()
  }

  return (
    <ToastRoot
      role="dialog"
      showPointer={!!onClick}
      onClick={onClick}
      data-qa={dataQa}
    >
      <ToastContainer>
        <RoundIcon content={icon} color={iconColor} size="L" />
        <div>{children}</div>
        {onClose && (
          <CloseButtonWrapper>
            <CloseButton
              data-qa="toast-close-button"
              icon={faTimes}
              onClick={handleOnClose}
              aria-label={closeLabel}
            />
          </CloseButtonWrapper>
        )}
      </ToastContainer>
    </ToastRoot>
  )
})

const ToastRoot = styled.div<{
  showPointer: boolean
}>`
  max-width: ${toastWidthPx};
  @media (max-width: ${toastBreakpoint}) {
    max-width: 100%;
  }
  padding: ${defaultMargins.s};
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  border-radius: 16px;
  outline: 1px solid ${(p) => p.theme.colors.main.m1};
  z-index: ${modalZIndex - 5};
  cursor: ${(p) => (p.showPointer ? 'pointer' : 'auto')};
  pointer-events: auto;
`

const CloseButtonWrapper = styled.div`
  align-self: flex-start;
  @media (max-width: ${toastBreakpoint}) {
    position: absolute;
    top: 0;
    right: 0;
  }
`

const CloseButton = styled(IconOnlyButton)`
  color: ${(p) => p.theme.colors.main.m2};
`

const ToastContainer = styled.div`
  display: flex;
  flex-direction: row;
  gap: ${defaultMargins.s};
  align-items: center;
  position: relative;
  @media (max-width: ${toastBreakpoint}) {
    flex-direction: column;
    align-items: stretch;
  }
`
