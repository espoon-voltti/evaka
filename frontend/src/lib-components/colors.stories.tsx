// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled, { useTheme } from 'styled-components'
import { storiesOf } from '@storybook/react'
import { H2, H3, Label } from './typography'
import { Gap } from './white-space'
import { FixedSpaceColumn, FixedSpaceRow } from './layout/flex-helpers'

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
  return <FixedSpaceRow spacing={'m'}>{children}</FixedSpaceRow>
}

function Col({ children }: { children: React.ReactNode }) {
  return <FixedSpaceColumn alignItems="center">{children}</FixedSpaceColumn>
}

storiesOf('evaka/Colors', module).add('default', () =>
  React.createElement(() => {
    const theme = useTheme()
    return (
      <div>
        <H2>Brand Colors</H2>
        <H3>Espoo</H3>
        <Row>
          <Col>
            <ColorCircle color={theme.colors.brand.primary} />
            <Label>primary</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.brand.secondary} />
            <Label>secondary</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.brand.secondaryLight} />
            <Label>secondary light</Label>
          </Col>
        </Row>

        <Gap />

        <H2>Evaka Colors</H2>

        <H3>Main</H3>
        <Row>
          <Col>
            <ColorCircle color={theme.colors.main.dark} />
            <Label>dark</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.main.medium} />
            <Label>medium</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.main.primary} />
            <Label>primary</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.main.light} />
            <Label>light</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.main.lighter} />
            <Label>lighter</Label>
          </Col>
        </Row>

        <Gap />

        <H3>Greyscale</H3>
        <Row>
          <Col>
            <ColorCircle color={theme.colors.greyscale.darkest} />
            <Label>darkest</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.greyscale.dark} />
            <Label>dark</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.greyscale.medium} />
            <Label>medium</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.greyscale.lighter} />
            <Label>lighter</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.greyscale.lightest} />
            <Label>lightest</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.greyscale.white} bordered />
            <Label>white</Label>
          </Col>
        </Row>

        <Gap />

        <H3>Accents</H3>
        <Row>
          <Col>
            <ColorCircle color={theme.colors.accents.orange} />
            <Label>orange</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.accents.orangeDark} />
            <Label>orange dark</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.accents.green} />
            <Label>green</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.accents.greenDark} />
            <Label>green dark</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.accents.water} />
            <Label>water</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.accents.yellow} />
            <Label>yellow</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.accents.red} />
            <Label>red</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.accents.petrol} />
            <Label>petrol</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.accents.emerald} />
            <Label>emerald</Label>
          </Col>
          <Col>
            <ColorCircle color={theme.colors.accents.violet} />
            <Label>violet</Label>
          </Col>
        </Row>
      </div>
    )
  })
)
