// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactNode } from 'react'
import styled from 'styled-components'
import { faTimes } from 'lib-icons'
import { defaultMargins, Gap } from 'lib-components/white-space'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { desktopMin, tabletMin } from 'lib-components/breakpoints'

interface Props {
  highlight: boolean
  close: () => void
  children: ReactNode | ReactNode[]
}

export default React.memo(function CalendarModal({
  highlight,
  close,
  children
}: Props) {
  return (
    <Modal>
      <TopBar>
        <CloseButton icon={faTimes} onClick={close} />
      </TopBar>
      <Content highlight={highlight}>{children}</Content>
      <Gap size="XXL" />
    </Modal>
  )
})

const Modal = styled.div`
  background: ${(p) => p.theme.colors.brand.secondaryLight};
  box-shadow: 0px 10px 24px 24px #0f0f0f14;
  z-index: 20;
  position: fixed;
  overflow-y: auto;
  bottom: 0;

  right: 100px;
  max-height: min(840px, 90vh);
  width: 500px;

  @media (max-width: ${desktopMin}) {
    right: ${defaultMargins.L};
  }

  @media (max-width: ${tabletMin}) {
    top: 0;

    right: 0;
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

const Content = styled.div<{ highlight: boolean }>`
  background: ${(p) => p.theme.colors.greyscale.white};
  box-shadow: 0px 4px 4px -4px #0f0f0f1a, 0px -4px 4px -4px #0f0f0f1a;

  padding: ${defaultMargins.L};
  padding-left: calc(${defaultMargins.L} - 4px);
  border-left: 4px solid
    ${(p) => (p.highlight ? p.theme.colors.brand.secondary : 'transparent')};

  @media (max-width: ${tabletMin}) {
    padding: ${defaultMargins.s};
    padding-left: calc(${defaultMargins.s} - 4px);
  }
`
