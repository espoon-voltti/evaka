// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type { AsyncButtonProps } from './AsyncButton'
import AsyncButton from './AsyncButton'

type AsyncIconButtonProps<T> = Omit<AsyncButtonProps<T>, 'text'>

const Wrapper = styled.div`
  .async-icon-button {
    border: none;
    min-width: 0;
    min-height: 0;
    padding: 0;
    background-color: transparent;
  }
`

export default React.memo(function AsyncIconButton<T>({
  className,
  ...props
}: AsyncIconButtonProps<T>) {
  return (
    <Wrapper className={className}>
      <AsyncButton text="" {...props} className="async-icon-button" />
    </Wrapper>
  )
})
