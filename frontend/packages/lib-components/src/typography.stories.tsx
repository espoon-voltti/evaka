// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { storiesOf } from '@storybook/react'
import { LoremParagraph } from './story-utils'
import { H1, H2, H3, H4, Label } from './typography'
import { Gap } from './white-space'

const LabelDiv = styled.div`
  display: flex;

  label {
    width: 150px;
  }
`

storiesOf('evaka/Typography', module).add('default', () => (
  <div>
    <H1>This is heading level 1</H1>
    <LoremParagraph />
    <Gap />

    <H2>This is heading level 2</H2>
    <LoremParagraph />
    <Gap />

    <H3>This is heading level 3</H3>
    <LoremParagraph />
    <Gap />

    <H4>This is heading level 4</H4>
    <LoremParagraph />
    <Gap />

    <H4>This is heading level 5</H4>
    <LoremParagraph />
    <Gap />

    <LabelDiv>
      <Label>This is a label</Label>
      <span>Lorem ipsum dolor sit amet</span>
    </LabelDiv>
    <LabelDiv>
      <Label>Another label</Label>
      <span>Lorem ipsum dolor sit amet</span>
    </LabelDiv>
  </div>
))
