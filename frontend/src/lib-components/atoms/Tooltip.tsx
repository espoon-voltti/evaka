// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, { useCallback, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import styled from 'styled-components'

import {
  fasCaretDown,
  fasCaretLeft,
  fasCaretRight,
  fasCaretUp
} from 'lib-icons'

import type { BaseProps } from '../utils'
import { defaultMargins } from '../white-space'

const TooltipWrapper = styled.div`
  position: relative;
  display: inline-block;
  &:not(:hover) {
    .tooltip {
      display: none;
    }
  }
`

const TooltipPositioner = styled.div<{
  position: Position
}>`
  position: fixed;
  z-index: 99999;
  display: flex;
`

const TooltipDiv = styled.div`
  position: static;
  color: ${(p) => p.theme.colors.grayscale.g0};
  font-size: 15px;
  line-height: 22px;

  background-color: ${(p) => p.theme.colors.grayscale.g70};
  padding: ${defaultMargins.s};
  border-radius: 2px;
  box-shadow: 0 4px 4px rgba(0, 0, 0, 0.25);

  p {
    margin: 0;
  }
  p:not(:last-child) {
    margin-bottom: 8px;
  }

  overflow-wrap: anywhere;
  hyphens: auto;
`

const beakPositions = (position: Position) => {
  const top = (() => {
    switch (position) {
      case 'top':
        return 'auto'
      case 'bottom':
        return '-24px'
      case 'left':
      case 'right':
        return '0'
    }
  })()

  const bottom = () => {
    switch (position) {
      case 'top':
        return '-24px'
      case 'bottom':
        return 'auto'
      case 'left':
      case 'right':
        return '0'
    }
  }

  const left = () => {
    switch (position) {
      case 'top':
      case 'bottom':
        return '0'
      case 'left':
        return 'auto'
      case 'right':
        return '-10px'
    }
  }

  const right = () => {
    switch (position) {
      case 'top':
      case 'bottom':
        return '0'
      case 'left':
        return '-10px'
      case 'right':
        return 'auto'
    }
  }

  return { top, bottom, left, right }
}

const Beak = styled.div<{ position: Position }>`
  pointer-events: none;
  position: absolute;
  display: flex;
  justify-content: center;
  align-items: center;
  color: ${(p) => p.theme.colors.grayscale.g70};

  top: ${(p) => beakPositions(p.position).top};
  bottom: ${(p) => beakPositions(p.position).bottom};
  left: ${(p) => beakPositions(p.position).left};
  right: ${(p) => beakPositions(p.position).right};
`

type Position = 'top' | 'bottom' | 'right' | 'left'
type Width = 'small' | 'large'

export type TooltipProps = BaseProps & {
  children?: React.ReactNode
  tooltip: React.ReactNode
  position?: Position
  width?: Width
  className?: string
}

const defaultPosition: Position = 'bottom'

export default React.memo(function Tooltip({
  children,
  className,
  ...props
}: TooltipProps) {
  const [isHovered, setIsHovered] = useState(false)
  const wrapperRef = useRef<HTMLDivElement>(null)
  const tooltipRef = useRef<HTMLDivElement>(null)

  const position = props.position ?? defaultPosition

  useFloatingPositioning({
    anchorRef: wrapperRef,
    floatingRef: tooltipRef,
    active: isHovered,
    position,
    width: props.width ?? 'small'
  })

  return (
    <TooltipWrapper
      ref={wrapperRef}
      className={className}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      {children}
      {isHovered &&
        createPortal(
          <TooltipWithoutAnchor ref={tooltipRef} {...props} />,
          document.getElementById('tooltip-container') ?? document.body
        )}
    </TooltipWrapper>
  )
})

export const TooltipWithoutAnchor = React.memo(
  React.forwardRef(function Tooltip(
    {
      tooltip,
      'data-qa': dataQa,
      className,
      ...props
    }: Omit<TooltipProps, 'children'>,
    ref: React.ForwardedRef<HTMLDivElement>
  ) {
    if (!tooltip) return null

    const position = props.position ?? defaultPosition
    const icon = (() => {
      switch (position) {
        case 'top':
          return fasCaretDown
        case 'bottom':
          return fasCaretUp
        case 'left':
          return fasCaretRight
        case 'right':
          return fasCaretLeft
      }
    })()

    return (
      <TooltipPositioner
        className={classNames('tooltip', className)}
        position={position}
        ref={ref}
      >
        <TooltipDiv data-qa={dataQa}>
          <Beak position={position}>
            <FontAwesomeIcon
              icon={icon}
              size={['top', 'bottom'].includes(position) ? '3x' : '2x'}
            />
          </Beak>
          <div>{tooltip}</div>
        </TooltipDiv>
      </TooltipPositioner>
    )
  })
)

type TooltipLocation = {
  top: string | null
  left: string | null
  right: string | null
  bottom: string | null
  maxWidth?: string
}

function useFloatingPositioning({
  anchorRef,
  floatingRef,
  active,
  position,
  width
}: {
  anchorRef: React.RefObject<HTMLElement | null>
  floatingRef: React.RefObject<HTMLElement | null>
  active: boolean
  position: Position
  width: Width
}) {
  const locate = useCallback(() => {
    const margin = 4 // px

    const viewportWidth = window.innerWidth
    const viewportHeight = window.innerHeight

    const anchor = anchorRef.current
    const floating = floatingRef.current
    if (!anchor || !floating) return

    const anchorRect = anchor.getBoundingClientRect()

    const baseTooltipMaxWidth = 128 // based on previous tooltip measures
    const calculatedMaxWidth =
      (['top', 'bottom'].includes(position)
        ? width === 'large'
          ? 400
          : baseTooltipMaxWidth
        : margin) + anchorRect.width

    Object.assign(floating.style, {
      maxWidth: Math.max(baseTooltipMaxWidth, calculatedMaxWidth) + 'px'
    })

    const location: TooltipLocation = {
      top: null,
      left: null,
      right: null,
      bottom: null
    }

    const verticalLeft =
      anchorRect.left - floating.offsetWidth / 2 + anchorRect.width / 2 + 'px'
    const horizontalTop =
      anchorRect.top - floating.offsetHeight / 2 + anchorRect.height / 2 + 'px'

    switch (position) {
      case 'top':
        location.bottom = viewportHeight - anchorRect.top + 3 * margin + 'px'
        location.left = verticalLeft
        break
      case 'bottom':
        location.top = anchorRect.bottom + 3 * margin + 'px'
        location.left = verticalLeft
        break
      case 'left':
        location.top = horizontalTop
        location.right = viewportWidth - anchorRect.left + 4 * margin + 'px'
        break
      case 'right':
        location.top = horizontalTop
        location.left = anchorRect.right + 4 * margin + 'px'
        break
    }

    Object.assign(floating.style, location)
  }, [anchorRef, floatingRef, position, width])

  useLayoutEffect(() => {
    if (!active) return

    locate() // Initial positioning

    const handleUpdate = () => locate()

    window.addEventListener('scroll', handleUpdate, true)
    window.addEventListener('resize', handleUpdate)

    return () => {
      window.removeEventListener('scroll', handleUpdate, true)
      window.removeEventListener('resize', handleUpdate)
    }
  }, [active, locate])
}
