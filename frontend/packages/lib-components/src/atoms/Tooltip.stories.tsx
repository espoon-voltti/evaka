// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { storiesOf } from '@storybook/react'
import { FixedSpaceColumn } from '../layout/flex-helpers'
import Tooltip from './Tooltip'

const Elem = styled.div`
  height: 200px;
  width: 300px;
  background-color: red;
`

storiesOf('evaka/atoms/Tooltip', module).add('default', () => (
  <div>
    <FixedSpaceColumn>
      <Tooltip
        tooltip={
          <div>
            Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
            eiusmod tempor incididunt ut labore et dolore magna aliqua. <br />{' '}
            Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris
            nisi ut aliquip ex ea commodo consequat.
          </div>
        }
      >
        <Elem>Elementti 1</Elem>
      </Tooltip>

      <Tooltip tooltip={<span>Tämä on tooltip!</span>}>
        <Elem>Elementti 2</Elem>
      </Tooltip>
    </FixedSpaceColumn>
  </div>
))
