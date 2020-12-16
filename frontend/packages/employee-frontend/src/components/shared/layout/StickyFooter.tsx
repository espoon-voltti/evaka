// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'
import Container from 'components/shared/layout/Container'
import { DefaultMargins } from 'components/shared/layout/white-space'

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
  background-color: ${colors.greyscale.white};
  box-shadow: 0 -2px 4px 0 rgba(0, 0, 0, 0.1);

  @media print {
    display: none;
  }
`

const ContentContainer = styled(Container)`
  padding: ${DefaultMargins.xs} 0;
`
