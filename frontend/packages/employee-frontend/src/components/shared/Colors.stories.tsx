// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { storiesOf } from '@storybook/react'
import styled from 'styled-components'
import colors from '@evaka/lib-components/src/colors'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'components/shared/layout/flex-helpers'
import { H2, H3, Label } from '@evaka/lib-components/src/typography'
import { Gap } from '@evaka/lib-components/src/white-space'

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
        <ColorCircle color={colors.brandEspoo.espooBlue} />
        <Label>blue</Label>
      </Col>
      <Col>
        <ColorCircle color={colors.brandEspoo.espooTurquoise} />
        <Label>turquoise</Label>
      </Col>
    </Row>

    <Gap />

    <H2>Evaka Colors</H2>

    <H3>Blues</H3>
    <Row>
      <Col>
        <ColorCircle color={colors.blues.dark} />
        <Label>dark</Label>
      </Col>
      <Col>
        <ColorCircle color={colors.blues.medium} />
        <Label>medium</Label>
      </Col>
      <Col>
        <ColorCircle color={colors.blues.primary} />
        <Label>primary</Label>
      </Col>
      <Col>
        <ColorCircle color={colors.blues.light} />
        <Label>light</Label>
      </Col>
    </Row>

    <Gap />

    <H3>Greyscale</H3>
    <Row>
      <Col>
        <ColorCircle color={colors.greyscale.darkest} />
        <Label>darkest</Label>
      </Col>
      <Col>
        <ColorCircle color={colors.greyscale.dark} />
        <Label>dark</Label>
      </Col>
      <Col>
        <ColorCircle color={colors.greyscale.medium} />
        <Label>medium</Label>
      </Col>
      <Col>
        <ColorCircle color={colors.greyscale.lighter} />
        <Label>lighter</Label>
      </Col>
      <Col>
        <ColorCircle color={colors.greyscale.lightest} />
        <Label>lightest</Label>
      </Col>
      <Col>
        <ColorCircle color={colors.greyscale.white} bordered />
        <Label>white</Label>
      </Col>
    </Row>

    <Gap />

    <H3>Accents</H3>
    <Row>
      <Col>
        <ColorCircle color={colors.accents.orange} />
        <Label>orange</Label>
      </Col>
      <Col>
        <ColorCircle color={colors.accents.green} />
        <Label>green</Label>
      </Col>
      <Col>
        <ColorCircle color={colors.accents.water} />
        <Label>water</Label>
      </Col>
      <Col>
        <ColorCircle color={colors.accents.yellow} />
        <Label>yellow</Label>
      </Col>
      <Col>
        <ColorCircle color={colors.accents.red} />
        <Label>red</Label>
      </Col>
      <Col>
        <ColorCircle color={colors.accents.petrol} />
        <Label>petrol</Label>
      </Col>
      <Col>
        <ColorCircle color={colors.accents.emerald} />
        <Label>emerald</Label>
      </Col>
      <Col>
        <ColorCircle color={colors.accents.violet} />
        <Label>violet</Label>
      </Col>
    </Row>
  </div>
))
