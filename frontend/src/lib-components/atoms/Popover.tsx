// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useDialog } from '@react-aria/dialog'
import { FocusScope } from '@react-aria/focus'
import { useOverlay, useModal, DismissButton } from '@react-aria/overlays'
import { mergeProps } from '@react-aria/utils'
import { AriaDialogProps } from '@react-types/dialog'
import React from 'react'
import styled from 'styled-components'

import { defaultMargins } from 'lib-components/white-space'

type PopoverProps = {
  isOpen: boolean
  onClose?: () => void
  children: React.ReactNode
  popoverRef?: React.RefObject<HTMLDivElement>
  openAbove?: boolean
} & AriaDialogProps

const OverlayBox = styled.div`
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  box-shadow: 0px 2px 4px rgba(0, 0, 0, 0.25);
  border-radius: 4px;
`

const OverlayContainer = styled.div<{ $openAbove?: boolean }>`
  padding: ${defaultMargins.s} 0;
  position: absolute;
  z-index: 40;
  ${(p) =>
    p.$openAbove
      ? `
    bottom: 100%;
    padding-bottom: 0;
  `
      : ''};
`

export default React.memo(function Popover({
  isOpen,
  onClose,
  children,
  openAbove,
  ...props
}: PopoverProps) {
  const fallbackRef = React.useRef<HTMLDivElement>(null)
  const ref = props.popoverRef ?? fallbackRef

  const { overlayProps } = useOverlay(
    {
      isOpen,
      onClose,
      isDismissable: true
    },
    ref
  )

  const { modalProps } = useModal()
  const { dialogProps } = useDialog(props, ref)

  return (
    <FocusScope contain restoreFocus>
      <OverlayContainer
        {...mergeProps(overlayProps, modalProps, dialogProps)}
        ref={ref}
        $openAbove={openAbove}
      >
        <OverlayBox>
          {children}
          <DismissButton onDismiss={onClose} />
        </OverlayBox>
      </OverlayContainer>
    </FocusScope>
  )
})
