// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { storiesOf } from '@storybook/react'
import styled from 'styled-components'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'components/shared/layout/flex-helpers'
import Colors from 'components/shared/Colors'
import { H2, H3, Label } from 'components/shared/Typography'
import { Gap } from 'components/shared/layout/white-space'

interface ColorCircleProps {
  color: string
  bordered?: boolean
}
export const ColorCircle = styled.div<ColorCircleProps>`
  width: 100px;
  height: 100px;
  border-radius: 100%;
  background: ${(p) => p.color};
  ${(p) => (p.bordered ? `border: lightgrey solid 1px;` : '')}
`

function Row({ children }: { children: React.ReactNode }) {
  return (
    <FixedSpaceRow spacing={'m'} wrap>
      {children}
    </FixedSpaceRow>
  )
}

function Col({ children }: { children: React.ReactNode }) {
  return <FixedSpaceColumn alignItems="center">{children}</FixedSpaceColumn>
}

storiesOf('evaka/Colors', module).add('default', () => (
  <div>
    <H2>Brand Colors</H2>
    <H3>Espoo</H3>
    <Row>
      <Col>
        <ColorCircle color={Colors.brandEspoo.espooBlue} />
        <Label>blue</Label>
      </Col>
      <Col>
        <ColorCircle color={Colors.brandEspoo.espooTurquoise} />
        <Label>turquoise</Label>
      </Col>
    </Row>

    <Gap />

    <H2>Evaka Colors</H2>

    <H3>Blues</H3>
    <Row>
      <Col>
        <ColorCircle color={Colors.blues.dark} />
        <Label>dark</Label>
      </Col>
      <Col>
        <ColorCircle color={Colors.blues.medium} />
        <Label>medium</Label>
      </Col>
      <Col>
        <ColorCircle color={Colors.blues.primary} />
        <Label>primary</Label>
      </Col>
      <Col>
        <ColorCircle color={Colors.blues.light} />
        <Label>light</Label>
      </Col>
    </Row>

    <Gap />

    <H3>Greyscale</H3>
    <Row>
      <Col>
        <ColorCircle color={Colors.greyscale.darkest} />
        <Label>darkest</Label>
      </Col>
      <Col>
        <ColorCircle color={Colors.greyscale.dark} />
        <Label>dark</Label>
      </Col>
      <Col>
        <ColorCircle color={Colors.greyscale.medium} />
        <Label>medium</Label>
      </Col>
      <Col>
        <ColorCircle color={Colors.greyscale.lighter} />
        <Label>lighter</Label>
      </Col>
      <Col>
        <ColorCircle color={Colors.greyscale.lightest} />
        <Label>lightest</Label>
      </Col>
      <Col>
        <ColorCircle color={Colors.greyscale.white} bordered />
        <Label>white</Label>
      </Col>
    </Row>

    <Gap />

    <H3>Accents</H3>
    <Row>
      <Col>
        <ColorCircle color={Colors.accents.orange} />
        <Label>orange</Label>
      </Col>
      <Col>
        <ColorCircle color={Colors.accents.green} />
        <Label>green</Label>
      </Col>
      <Col>
        <ColorCircle color={Colors.accents.water} />
        <Label>water</Label>
      </Col>
      <Col>
        <ColorCircle color={Colors.accents.yellow} />
        <Label>yellow</Label>
      </Col>
      <Col>
        <ColorCircle color={Colors.accents.red} />
        <Label>red</Label>
      </Col>
      <Col>
        <ColorCircle color={Colors.accents.petrol} />
        <Label>petrol</Label>
      </Col>
      <Col>
        <ColorCircle color={Colors.accents.emerald} />
        <Label>emerald</Label>
      </Col>
      <Col>
        <ColorCircle color={Colors.accents.violet} />
        <Label>violet</Label>
      </Col>
    </Row>
  </div>
))
