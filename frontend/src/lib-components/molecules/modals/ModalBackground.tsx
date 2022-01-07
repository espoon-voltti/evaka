// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactNode } from 'react'
import FocusLock from 'react-focus-lock'
import styled from 'styled-components'
import { modalZIndex } from '../../layout/z-helpers'

interface Props extends ZIndexProp {
  children: ReactNode | ReactNode[]
}

interface ZIndexProp {
  onClick?: () => void
  zIndex?: number
}

export default React.memo(function ModalBackground({
  onClick,
  zIndex,
  children
}: Props) {
  return (
    <FocusLock>
      <BackgroundOverlay zIndex={zIndex} onClick={onClick} />
      <FormModalLifter zIndex={zIndex}>{children}</FormModalLifter>
    </FocusLock>
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
