// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { Container } from 'lib-components/layout/Container'
import colors from 'lib-customizations/common'

type Props = {
  align: 'center' | 'left' | 'right'
  'data-qa'?: string
}

export default React.memo(function StickyActionBar({
  align,
  ['data-qa']: dataQa,
  children
}: Props & { children: React.ReactNode | readonly React.ReactNode[] }) {
  return (
    <Bar data-qa={dataQa}>
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
  background: ${colors.grayscale.g0};
  padding: 16px 0;
  margin: 0 -9999rem;
  margin-top: 50px;
  box-shadow: 0px -8px 8px -6px ${colors.grayscale.g4};
`

const Content = styled(Container)<Props>`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: ${({ align }) => alignValues[align]};
`
