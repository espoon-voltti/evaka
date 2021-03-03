// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import colors from '@evaka/lib-components/colors'
import { Container } from '@evaka/lib-components/layout/Container'

type Props = {
  align: 'center' | 'left' | 'right'
}

export default React.memo(function StickyActionBar({
  align,
  children
}: Props & { children: React.ReactNode | React.ReactNodeArray }) {
  return (
    <Bar>
      <Content align={align}>{children}</Content>
    </Bar>
  )
})

const alignValues = {
  center: 'center',
  left: 'flex-start',
  right: 'flex-end'
} as const

const Bar = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  position: sticky;
  bottom: 0;
  background: white;
  padding: 16px 0;
  margin: 0 -9999rem;
  margin-top: 50px;
  box-shadow: 0px -8px 8px -6px ${colors.greyscale.lightest};
`

const Content = styled(Container)<Props>`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: ${({ align }) => alignValues[align]};
`
