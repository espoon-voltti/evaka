// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Property } from 'csstype'
import type { ReactNode } from 'react'
import React, { Fragment } from 'react'
import styled, { css } from 'styled-components'

import { LabelLike } from 'lib-components/typography'

type Spacing = 'small' | 'large'
type LabelWidth = '25%' | 'fit-content(40%)'

type Content = {
  label?: ReactNode
  value: ReactNode
  valueWidth?: string
  dataQa?: string
  onlyValue?: boolean
}
type Props = {
  spacing: Spacing
  horizontalSpacing?: Spacing
  labelWidth?: LabelWidth
  contents: (Content | false)[]
  alignItems?: Property.AlignItems
}

const LabelValueList = React.memo(function LabelValueList({
  spacing,
  horizontalSpacing,
  contents,
  alignItems,
  labelWidth = '25%'
}: Props) {
  return (
    <GridContainer
      spacing={spacing}
      horizontalSpacing={horizontalSpacing}
      alignItems={alignItems}
      labelWidth={labelWidth}
      size={contents.length}
    >
      {contents
        .filter((content): content is Content => !!content)
        .map(({ label, value, valueWidth, dataQa, onlyValue }, index) =>
          onlyValue ? (
            <OnlyValue index={index + 1} width={valueWidth} key={index}>
              {value}
            </OnlyValue>
          ) : (
            <Fragment key={index}>
              <GridLabel index={index + 1}>{label}</GridLabel>
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
  horizontalSpacing?: Spacing
  size: number
  labelWidth: LabelWidth
  alignItems?: Property.AlignItems
}>`
  display: grid;
  grid-template-columns: ${(p) => p.labelWidth} auto;
  grid-template-rows: repeat(${({ size }) => size}, auto);
  ${({ spacing, horizontalSpacing }) => css`
    grid-gap: ${spacing === 'small' ? '0.5em' : '1em'}
      ${horizontalSpacing === 'small' ? '1em' : '4em'};
  `};
  justify-items: start;
  align-items: ${(p) => p.alignItems ?? 'baseline'};
`

const GridLabel = styled(LabelLike)<{ index: number }>`
  grid-column: 1;
  grid-row: ${({ index }) => index};
`

const Value = styled.div<{ index: number; width?: string }>`
  grid-column: 2;
  grid-row: ${({ index }) => index};
  ${({ width }) => width && `width: ${width};`};
`

const OnlyValue = styled.div<{ index: number; width?: string }>`
  grid-column: 1 / -1;
  grid-row: ${({ index }) => index};
  ${({ width }) => width && `width: ${width};`};
`

export default LabelValueList
