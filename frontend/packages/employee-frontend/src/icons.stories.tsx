// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { storiesOf } from '@storybook/react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import * as icons from '@evaka/icons'
import { DefaultMargins } from 'components/shared/layout/white-space'
import { IconDefinition } from '@fortawesome/fontawesome-svg-core'

const Grid = styled.div`
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  row-gap: ${DefaultMargins.m};
  column-gap: ${DefaultMargins.m};
`

const ColItem = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  > * {
    margin-bottom: ${DefaultMargins.s};
  }
`

storiesOf('@evaka/icons', module).add('all', () => (
  <Grid>
    {Object.keys(icons).map((name) => {
      const icon = icons[name] as IconDefinition
      return (
        <ColItem key={name}>
          <FontAwesomeIcon icon={icon} size="lg" />
          <span>{name}</span>
        </ColItem>
      )
    })}
  </Grid>
))
