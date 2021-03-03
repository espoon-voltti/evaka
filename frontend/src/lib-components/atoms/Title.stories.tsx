// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { storiesOf } from '@storybook/react'
import Title from './Title'

storiesOf('evaka/atoms/Title', module).add('default', () => (
  <div>
    <div>
      <Title>Default sample title</Title>
    </div>
    <div>
      <Title size={2}>Sample title with size 2</Title>
    </div>
    <div>
      <Title size={3}>Sample title with size 3</Title>
    </div>
    <div>
      <Title size={4}>Sample title with size 4</Title>
    </div>
    <div>
      <Title centered>Sample centered title</Title>
    </div>
  </div>
))
