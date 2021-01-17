import React from 'react'
import styled from 'styled-components'
import { readableColor } from 'polished'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCheck } from '@evaka/lib-icons'
import colors from '../colors'
import { FixedSpaceRow } from '../layout/flex-helpers'
import { defaultMargins } from '../white-space'

type SelectionChipProps = {
  text: string
  selected: boolean
}

export const SelectionChip = React.memo(function SelectionChip({
  text,
  selected
}: SelectionChipProps) {
  return (
    <SelectionChipWrapper selected={selected} color={colors.primary}>
      <FixedSpaceRow alignItems="center">
        {selected && <FontAwesomeIcon icon={faCheck} />}
        <span>{text}</span>
      </FixedSpaceRow>
    </SelectionChipWrapper>
  )
})

export const StaticChip = styled.div<{ color: string }>`
  border: 1px solid ${(p) => p.color};
  border-radius: 1000px;
  background-color: ${(p) => p.color};
  color: ${(p) => readableColor(p.color, '#000', colors.greyscale.white)};
  padding: ${defaultMargins.xxs}
    calc(${defaultMargins.xs} + ${defaultMargins.xxs});
`

const SelectionChipWrapper = styled(StaticChip)<{ selected: boolean }>`
  ${(p) =>
    p.selected
      ? ''
      : `
      background-color: ${colors.greyscale.white};
      color: ${p.color}
  `}
`
