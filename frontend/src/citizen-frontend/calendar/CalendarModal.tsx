// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactNode } from 'react'
import styled from 'styled-components'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { desktopMin, tabletMin } from 'lib-components/breakpoints'
import { defaultMargins } from 'lib-components/white-space'
import { faTimes } from 'lib-icons'

interface Props {
  close: () => void
  'data-qa'?: string
  children: ReactNode | ReactNode[]
}

export default React.memo(function CalendarModal({
  close,
  'data-qa': dataQa,
  children
}: Props) {
  return (
    <Modal data-qa={dataQa}>
      <TopBar>
        <CloseButton icon={faTimes} onClick={close} />
      </TopBar>
      {children}
    </Modal>
  )
})

const Modal = styled.div`
  background: ${(p) => p.theme.colors.main.lighter};
  box-shadow: 0px 10px 24px 24px #0f0f0f14;
  z-index: 20;
  position: fixed;
  overflow-y: auto;

  bottom: ${defaultMargins.s};
  right: 100px;
  max-height: min(840px, 90vh);
  width: 500px;

  @media (max-width: ${desktopMin}) {
    right: ${defaultMargins.L};
  }

  @media (max-width: ${tabletMin}) {
    top: 0;
    bottom: 0;
    right: 0;
    left: 0;
    max-height: 100%;
    width: 100%;
  }
`

const TopBar = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
  align-items: center;
  height: 60px;
`

const CloseButton = styled(IconButton)`
  padding: ${defaultMargins.s};
  margin: 0 ${defaultMargins.xs};
`
