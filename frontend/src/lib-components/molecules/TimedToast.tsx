// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useEffect, useRef } from 'react'
import { useLocation } from 'react-router-dom'
import styled, { useTheme } from 'styled-components'

import { useStableCallback } from 'lib-common/utils/useStableCallback'
import { faCheck, faTimes } from 'lib-icons'

import { IconOnlyButton } from '../atoms/buttons/IconOnlyButton'
import { tabletMin } from '../breakpoints'
import { FixedSpaceRow } from '../layout/flex-helpers'
import { modalZIndex } from '../layout/z-helpers'
import { defaultMargins } from '../white-space'

export interface Props {
  onClick?: () => void
  onClose: () => void
  children?: React.ReactNode
  'aria-label'?: string
  'data-qa'?: string
  closeLabel: string
}

const defaultDuration = 10000

export default React.memo(function TimedToast({
  onClick,
  onClose,
  children,
  'aria-label': ariaLabel,
  'data-qa': dataQa,
  closeLabel
}: Props) {
  const { colors } = useTheme()
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const location = useLocation()
  const initialUrlPath = useRef(location.pathname)
  const onClose_ = useStableCallback(onClose)

  const stopTimer = () => {
    if (timerRef.current) {
      clearTimeout(timerRef.current)
    }
  }

  useEffect(() => {
    timerRef.current = setTimeout(() => {
      onClose_()
    }, defaultDuration)
    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current)
      }
    }
  }, [onClose_])

  useEffect(() => {
    if (location.pathname !== initialUrlPath.current) {
      onClose_()
    }
  }, [location.pathname, onClose_])

  return (
    <ToastRoot
      role="dialog"
      aria-atomic="true"
      data-qa={dataQa}
      onClick={onClose}
    >
      <ToastContainer>
        <FixedSpaceRow alignItems="center">
          <FontAwesomeIcon icon={faCheck} color={colors.main.m2} />
          <ToastContent
            aria-label={ariaLabel || undefined}
            onClick={onClick}
            onMouseOver={() => {
              stopTimer()
            }}
          >
            {children}
          </ToastContent>
          <CloseButton
            data-qa="timed-toast-close-button"
            icon={faTimes}
            onClick={onClose}
            aria-label={closeLabel}
          />
        </FixedSpaceRow>
      </ToastContainer>
    </ToastRoot>
  )
})

const ToastRoot = styled.div`
  width: 100%;
  @media (min-width: ${tabletMin}) {
    right: 16px;
    width: 480px;
    max-width: 360px;
  }
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  border-radius: 16px;
  outline: 1px solid ${(p) => p.theme.colors.main.m1};
  z-index: ${modalZIndex - 5};
  cursor: pointer;
  pointer-events: auto;
  overflow: hidden;
`

const ToastContent = styled.div`
  flex-grow: 1;
`

const ToastContainer = styled.div`
  position: relative;
  padding: ${defaultMargins.s};
`

const CloseButton = styled(IconOnlyButton)`
  align-self: flex-start;
  color: ${(p) => p.theme.colors.main.m2};
`
