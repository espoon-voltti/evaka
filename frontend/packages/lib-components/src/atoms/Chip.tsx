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
  onClick: (selected: boolean) => void
}

export const SelectionChip = React.memo(function SelectionChip({
  text,
  selected,
  onClick
}: SelectionChipProps) {
  return (
    <SelectionChipWrapper
      selected={selected}
      color={colors.primary}
      onClick={() => onClick(!selected)}
    >
      <FixedSpaceRow alignItems="center" spacing="xs">
        {selected && <FontAwesomeIcon icon={faCheck} />}
        <span>{text}</span>
      </FixedSpaceRow>
    </SelectionChipWrapper>
  )
})

export const StaticChip = styled.div<{ color: string; textColor?: string }>`
  font-family: 'Open Sans', sans-serif;
  font-weight: 600;
  font-size: 16px;
  line-height: 20px;
  user-select: none;
  border: 1px solid ${(p) => p.color};
  border-radius: 1000px;
  background-color: ${(p) => p.color};
  color: ${(p) =>
    p.textColor ??
    readableColor(p.color, colors.greyscale.darkest, colors.greyscale.white)};
  padding: ${defaultMargins.xxs}
    calc(${defaultMargins.xs} + ${defaultMargins.xxs});
`

const SelectionChipWrapper = styled(StaticChip)<{ selected: boolean }>`
  cursor: pointer;
  font-size: 14px;
  line-height: 18px;

  ${(p) =>
    p.selected
      ? ''
      : `
      background-color: ${colors.greyscale.white};
      color: ${p.color}
  `}
`
