// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, ReactNode } from 'react'
import styled from 'styled-components'

type Spacing = 'small' | 'large'
type LabelWidth = '25%' | 'fit-content(40%)' | string

type Props = {
  spacing: Spacing
  labelWidth?: LabelWidth
  contents: {
    label?: ReactNode
    value: ReactNode
    valueWidth?: string
    dataQa?: string
    onlyValue?: boolean
  }[]
}

const LabelValueList = React.memo(function LabelValueList({
  spacing,
  contents,
  labelWidth = '25%'
}: Props) {
  return (
    <GridContainer
      spacing={spacing}
      labelWidth={labelWidth}
      size={contents.length}
    >
      {contents.map(({ label, value, valueWidth, dataQa, onlyValue }, index) =>
        onlyValue ? (
          { value }
        ) : (
          <Fragment key={index}>
            <Label index={index + 1}>{label}</Label>
            <Value index={index + 1} width={valueWidth} data-qa={dataQa}>
              {value}
            </Value>
          </Fragment>
        )
      )}
    </GridContainer>
  )
})

const GridContainer = styled.div<{
  spacing: Spacing
  size: number
  labelWidth: LabelWidth
}>`
  display: grid;
  grid-template-columns: [label] ${(p) => p.labelWidth} [value] auto;
  grid-template-rows: repeat(${({ size }) => size}, auto);
  grid-gap: ${({ spacing }) => (spacing === 'small' ? '0.5em' : '1em')} 4em;
  justify-items: start;
  align-items: baseline;
`

const Label = styled.div<{ index: number }>`
  grid-column: label;
  grid-row: ${({ index }) => index};
  font-weight: 600;
`

const Value = styled.div<{ index: number; width?: string }>`
  grid-column: value;
  grid-row: ${({ index }) => index};
  ${({ width }) => width && `width: ${width};`};
`

export default LabelValueList
