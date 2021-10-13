// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { range } from 'lodash'
import React, { useMemo, useRef } from 'react'
import styled from 'styled-components'
import { StyledInput } from '../atoms/form/InputField'
import { fontWeights } from '../typography'
import { defaultMargins } from '../white-space'

const SingleNumberInput = styled(StyledInput)<{ invalid?: boolean }>`
  border: 1px solid
    ${({ theme, invalid }) =>
      invalid ? theme.colors.accents.red : theme.colors.main.primary};
  text-align: center;

  font-family: 'Montserrat', sans-serif;
  font-size: 2rem;
  font-weight: ${fontWeights.semibold};
  color: ${(p) => p.theme.colors.main.dark};
`

const PinContainer = styled.div`
  display: flex;
  justify-content: center;

  ${SingleNumberInput} + ${SingleNumberInput} {
    margin-left: ${defaultMargins.s};
  }
`

export const EMPTY_PIN = ['', '', '', '']

const isValidCharacter = (char: string) =>
  char === '' || !isNaN(parseInt(char, 10))

interface Props {
  pin: string[]
  onPinChange: (code: string[]) => void
  invalid?: boolean
}

export function PinInput({ pin, onPinChange, invalid = false }: Props) {
  const input1 = useRef<HTMLInputElement>(null)
  const input2 = useRef<HTMLInputElement>(null)
  const input3 = useRef<HTMLInputElement>(null)
  const input4 = useRef<HTMLInputElement>(null)
  const refs = useMemo(() => [input1, input2, input3, input4], [])

  if (pin.length !== 4) throw new Error('Invalid PIN length')

  const moveFocusRight = (currentIndex: number) =>
    refs[currentIndex + 1]?.current?.focus()
  const moveFocusLeft = (currentIndex: number) =>
    refs[currentIndex - 1]?.current?.focus()

  const onChange = (i: number, char: string) => {
    if (isValidCharacter(char)) {
      onPinChange([...pin.slice(0, i), char, ...pin.slice(i + 1)])
      if (char.length > 0) {
        moveFocusRight(i)
      }
    }
  }

  const moveFocusLeftOnBackspace = (i: number, key: string) => {
    if (i > 0 && key === 'Backspace' && pin[i] === '') {
      moveFocusLeft(i)
    }
  }

  return (
    <PinContainer data-qa="pin-input">
      {range(0, 4).map((i) => (
        <SingleNumberInput
          key={i}
          type="password"
          width="xs"
          inputMode="numeric"
          maxLength={1}
          invalid={invalid}
          clearable={false}
          autoFocus={i === 0}
          value={pin[i]}
          ref={refs[i]}
          onChange={(e) => onChange(i, e.target.value)}
          onKeyUp={(e) => moveFocusLeftOnBackspace(i, e.key)}
        />
      ))}
    </PinContainer>
  )
}
