// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { BaseProps } from '../utils'
import { defaultMargins } from '../white-space'

import Container from './Container'

type Props = {
  children: React.ReactNode
} & BaseProps

export default React.memo(function StickyFooter({
  children,
  className,
  'data-qa': dataQa
}: Props) {
  return (
    <Footer className={className} data-qa={dataQa}>
      <ContentContainer>{children}</ContentContainer>
    </Footer>
  )
})

const Footer = styled.footer`
  position: sticky;
  bottom: 0;
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  box-shadow: 0 -2px 4px 0 rgba(0, 0, 0, 0.1);

  @media print {
    display: none;
  }
`

const ContentContainer = styled(Container)`
  padding: ${defaultMargins.xs} 0;
`

export const StickyFooterContainer = styled.div`
  padding: ${defaultMargins.xs};
`
