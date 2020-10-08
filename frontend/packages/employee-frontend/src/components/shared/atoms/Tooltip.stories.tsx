// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { storiesOf } from '@storybook/react'
import Tooltip from '~components/shared/atoms/Tooltip'
import styled from 'styled-components'
import { FixedSpaceColumn } from '~components/shared/layout/flex-helpers'

const Elem = styled.div`
  height: 200px;
  width: 300px;
  background-color: red;
`

storiesOf('evaka/atoms/Tooltip', module).add('default', () => (
  <div>
    <FixedSpaceColumn>
      <Tooltip
        tooltipText={
          'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.'
        }
      >
        <Elem>Elementti 1</Elem>
      </Tooltip>

      <Tooltip tooltipText={'Tämä on tooltip!'}>
        <Elem>Elementti 2</Elem>
      </Tooltip>
    </FixedSpaceColumn>
  </div>
))
