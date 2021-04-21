import React from 'react'
import styled from 'styled-components'
import { formatDecimal } from 'lib-common/utils/number'
import colors from 'lib-components/colors'

interface Props {
  value: number
  disabled?: boolean
  onPlus?: () => void
  onMinus?: () => void
}

export default function PlusMinus({ value, disabled, onPlus, onMinus }: Props) {
  const str = formatDecimal(value)
  return (
    <Root>
      <TextButton disabled={disabled} onClick={onMinus}>
        -
      </TextButton>
      <Value>{str}</Value>
      <TextButton disabled={disabled} onClick={onPlus}>
        +
      </TextButton>
    </Root>
  )
}

const Root = styled.div`
  color: ${colors.blues.dark};
  display: flex;
  justify-content: center;
`

const Value = styled.span`
  font-size: 70px;
  min-width: 150px;
  text-align: center;
`

const TextButton = styled.button`
  border: none;
  outline: none;
  background-color: transparent;
  font-size: 48px;
  color: ${colors.blues.dark};
`
