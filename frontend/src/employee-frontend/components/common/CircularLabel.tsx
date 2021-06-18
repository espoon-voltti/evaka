// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

const RoundLabel = styled.div<{ background: string; color: string }>`
  border-radius: 15px;
  height: 30px;
  padding: 0 10px;
  text-align: center;
  line-height: 30px;
  font-weight: 600;
  white-space: nowrap;

  background: ${({ background }) => background};
  color: ${({ color }) => color};
`

interface Props {
  text: string
  background: string
  color: string
  'data-qa'?: string
}

function CircularLabel({ text, background, color, 'data-qa': dataQa }: Props) {
  return (
    <RoundLabel
      background={background}
      color={color}
      className={`info-label`}
      data-qa={dataQa}
    >
      {text}
    </RoundLabel>
  )
}

export default CircularLabel
