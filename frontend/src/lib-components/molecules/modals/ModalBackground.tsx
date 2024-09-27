// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactNode } from 'react'
import { FocusOn } from 'react-focus-on'
import styled from 'styled-components'

import { modalZIndex } from '../../layout/z-helpers'

interface Props extends ZIndexProp {
  children: ReactNode | ReactNode[]
}

interface ZIndexProp {
  onClick?: () => void
  zIndex?: number
  onEscapeKey?: () => void
}

export default React.memo(function ModalBackground({
  onClick,
  zIndex,
  onEscapeKey,
  children
}: Props) {
  return (
    <FocusOn onEscapeKey={onEscapeKey}>
      <BackgroundOverlay zIndex={zIndex} onClick={onClick} />
      <FormModalLifter zIndex={zIndex}>{children}</FormModalLifter>
    </FocusOn>
  )
})

const BackgroundOverlay = styled.div<ZIndexProp>`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: ${(p) => (p.zIndex ? p.zIndex - 2 : modalZIndex - 2)};
`

const FormModalLifter = styled.div<ZIndexProp>`
  position: relative;
  z-index: ${(p) => (p.zIndex ? p.zIndex - 1 : modalZIndex - 1)};
`
