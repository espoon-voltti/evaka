// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

interface Props {
  toggled: boolean
  children: React.ReactNode
  'data-qa'?: string
}

export default React.memo(function AttentionIndicator({
  toggled,
  children,
  'data-qa': dataQa
}: Props) {
  return (
    <Wrapper>
      {children}
      {toggled && <Indicator data-qa={dataQa} />}
    </Wrapper>
  )
})

const Wrapper = styled.div`
  position: relative;
`

const Indicator = styled.div`
  position: absolute;
  height: 12px;
  width: 12px;
  bottom: -2px;
  right: -6px;
  border-radius: 6px;
  background: ${({ theme }) => theme.colors.accents.orange};
`
