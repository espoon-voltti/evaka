// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { defaultMargins } from '../white-space'
import Container from './Container'

type Props = {
  children: React.ReactNode
}

export default React.memo(function StickyFooter({ children }: Props) {
  return (
    <Footer>
      <ContentContainer>{children}</ContentContainer>
    </Footer>
  )
})

const Footer = styled.footer`
  position: sticky;
  bottom: 0;
  background-color: ${({ theme: { colors } }) => colors.greyscale.white};
  box-shadow: 0 -2px 4px 0 rgba(0, 0, 0, 0.1);

  @media print {
    display: none;
  }
`

const ContentContainer = styled(Container)`
  padding: ${defaultMargins.xs} 0;
`
