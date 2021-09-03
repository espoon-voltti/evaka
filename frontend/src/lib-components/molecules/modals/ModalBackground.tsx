import React, { ReactNode } from 'react'
import styled from 'styled-components'
import FocusLock from 'react-focus-lock'
import { modalZIndex } from '../../layout/z-helpers'

interface Props extends ZIndexProp {
  children: ReactNode | ReactNode[]
}

interface ZIndexProp {
  zIndex?: number
}

export default React.memo(function ModalBackground({
  zIndex,
  children
}: Props) {
  return (
    <FocusLock>
      <BackgroundOverlay zIndex={zIndex} />
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
  z-index: ${(p) => (p.zIndex ? p.zIndex - 1 : modalZIndex - 1)};
`
