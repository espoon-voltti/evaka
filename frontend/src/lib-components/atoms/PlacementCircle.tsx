{
  /*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React from 'react'
import styled from 'styled-components'
import colors from '@evaka/lib-components/colors'
import Tooltip from './Tooltip'

const Circle = styled.div`
  width: 34px;
  height: 34px;
  min-width: 34px;
  min-height: 34px;
  max-width: 34px;
  max-height: 34px;
  background-color: ${colors.accents.green};
  border-radius: 100%;
`

const HalfCircle = styled.div`
  width: 17px;
  height: 34px;
  min-width: 17px;
  min-height: 34px;
  max-width: 17px;
  max-height: 34px;
  background-color: ${colors.accents.green};
  border-top-left-radius: 17px;
  border-bottom-left-radius: 17px;
`

type Props = {
  type: 'half' | 'full'
  label: string
  tooltipUp?: boolean
}

export default React.memo(function PlacementCircle({
  type,
  label,
  tooltipUp
}: Props) {
  return (
    <Tooltip tooltip={<span>{label}</span>} up={tooltipUp}>
      {type === 'half' ? <HalfCircle /> : <Circle />}
    </Tooltip>
  )
})
