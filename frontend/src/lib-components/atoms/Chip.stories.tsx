// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { storiesOf } from '@storybook/react'
import React from 'react'
import { useTheme } from 'styled-components'
import { FixedSpaceRow } from '../layout/flex-helpers'
import { H2, H3 } from '../typography'
import { Gap } from '../white-space'
import { StaticChip } from './Chip'

storiesOf('evaka/atoms/Chip', module).add('StaticChip', () =>
  React.createElement(() => {
    const theme = useTheme()

    const renderChips = (colors: Record<string, string>) =>
      Object.entries(colors).map(([key, value]) => (
        <StaticChip key={key} color={value}>
          {key}
        </StaticChip>
      ))

    return (
      <div>
        <H2>Brand Colors</H2>
        <H3>Espoo</H3>
        <FixedSpaceRow>{renderChips(theme.colors.brand)}</FixedSpaceRow>

        <Gap />

        <H2>Evaka Colors</H2>

        <H3>Main</H3>
        <FixedSpaceRow>{renderChips(theme.colors.main)}</FixedSpaceRow>

        <Gap />

        <H3>Greyscale</H3>
        <FixedSpaceRow>{renderChips(theme.colors.greyscale)}</FixedSpaceRow>

        <Gap />

        <H3>Accents</H3>
        <FixedSpaceRow>{renderChips(theme.colors.accents)}</FixedSpaceRow>
      </div>
    )
  })
)
