// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { StaticChip } from 'lib-components/atoms/Chip'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

type Props = { age: number }

const SmallChip = styled(StaticChip)`
  padding: ${defaultMargins.xxs};
  font-size: 14px;
`

export const AgeIndicatorChip = React.memo(function AgeIndicatorChip({
  age
}: Props) {
  return (
    <SmallChip color={age < 3 ? colors.accents.a6turquoise : colors.main.m1}>
      {age}v
    </SmallChip>
  )
})
