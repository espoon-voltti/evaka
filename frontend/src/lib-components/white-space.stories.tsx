// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { storiesOf } from '@storybook/react'
import { H3 } from './typography'
import { Gap, SpacingSize } from './white-space'

const ExampleElem = styled.div`
  width: 200px;
  height: 50px;
  border: darkgrey solid 1px;
`

const FlexRow = styled.div`
  display: flex;
  flex-direction: row;
`

const FlexColumn = styled.div`
  display: flex;
  flex-direction: column;
`

const spacingSizes: SpacingSize[] = ['xxs', 'xs', 's', 'm', 'L', 'XL', 'XXL']

storiesOf('evaka/layout/white-space', module)
  .add('Gaps in FlexRow', () => (
    <div>
      {spacingSizes.map((size) => (
        <div key={size}>
          <H3>{size}</H3>
          <FlexRow>
            <ExampleElem />
            <Gap horizontal size={size} />
            <ExampleElem />
          </FlexRow>
          <Gap size={'XL'} />
        </div>
      ))}
    </div>
  ))
  .add('Gaps in FlexColumn', () => (
    <div>
      {spacingSizes.map((size) => (
        <div key={size}>
          <H3>{size}</H3>
          <FlexColumn>
            <ExampleElem />
            <Gap size={size} />
            <ExampleElem />
          </FlexColumn>
          <Gap size={'XL'} />
        </div>
      ))}
    </div>
  ))
