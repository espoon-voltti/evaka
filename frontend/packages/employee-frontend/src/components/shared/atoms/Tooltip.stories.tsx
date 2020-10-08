// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { storiesOf } from '@storybook/react'
import Tooltip from '~components/shared/atoms/Tooltip'
import styled from 'styled-components'

const Elem = styled.div`
  height: 200px;
  width: 300px;
  background-color: red;
`

storiesOf('evaka/atoms/Tooltip', module).add('default', () => (
  <div>
    <Tooltip
      tooltipText={
        'hello world hello world hello world hello world hello world hello world hello world hello world hello world hello world hello world hello world hello world hello world hello world hello world hello world hello world hello world hello world hello world '
      }
    >
      <Elem>foobar</Elem>
    </Tooltip>
    <br />
    <br />
    <Tooltip tooltipText={'hello mars'}>
      <Elem>foobar</Elem>
    </Tooltip>
  </div>
))
