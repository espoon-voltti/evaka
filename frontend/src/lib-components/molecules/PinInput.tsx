// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import range from 'lodash/range'
import React, { RefObject, useMemo, useRef } from 'react'
import styled from 'styled-components'

import { BoundFormState } from 'lib-common/form/hooks'

import UnderRowStatusIcon from '../atoms/StatusIcon'
import InputField, {
  InputFieldF,
  InputFieldUnderRow,
  InputInfo,
  StyledInput,
  TextInputProps
} from '../atoms/form/InputField'
import { fontWeights } from '../typography'
import { defaultMargins } from '../white-space'

const SingleNumberInput = styled(StyledInput)<{ invalid?: boolean }>`
  border: 1px solid
    ${(p) =>
      p.invalid ? p.theme.colors.status.danger : p.theme.colors.main.m2};
  text-align: center;

  font-family: 'Montserrat', sans-serif;
  font-size: 2rem;
  font-weight: ${fontWeights.semibold};
  color: ${(p) => p.theme.colors.main.m1};
`

const Centered = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
`

const PinContainer = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;

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
  info?: InputInfo | undefined
  inputRef?: RefObject<HTMLInputElement>
}

export const PinInput = React.memo(function PinInput({
  pin,
  onPinChange,
  info,
  inputRef,
  invalid = false
}: Props) {
  const input1 = useRef<HTMLInputElement>(null)
  const input2 = useRef<HTMLInputElement>(null)
  const input3 = useRef<HTMLInputElement>(null)
  const input4 = useRef<HTMLInputElement>(null)
  const refs = useMemo(
    () => [inputRef ?? input1, input2, input3, input4],
    [inputRef]
  )

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
    <Centered>
      <PinContainer data-qa="pin-input">
        {range(0, 4).map((i) => (
          <SingleNumberInput
            key={i}
            type="password"
            width="xs"
            inputMode="numeric"
            maxLength={1}
            invalid={invalid}
            autoFocus={i === 0}
            value={pin[i]}
            ref={refs[i]}
            onChange={(e) => onChange(i, e.target.value)}
            onKeyUp={(e) => moveFocusLeftOnBackspace(i, e.key)}
          />
        ))}
      </PinContainer>
      {info && (
        <InputFieldUnderRow className={classNames(info.status)}>
          <span data-qa="pin-input-info">{info.text}</span>
          <UnderRowStatusIcon status={info?.status} />
        </InputFieldUnderRow>
      )}
    </Centered>
  )
})

interface PinInputF extends Omit<Props, 'pin' | 'onPinChange'> {
  bind: BoundFormState<string[]>
}

export const PinInputF = React.memo(function PinInputF({
  bind: { state, set },
  ...props
}: PinInputF) {
  return <PinInput {...props} pin={state} onPinChange={set} />
})

type PlainPinInputProps = Pick<
  TextInputProps,
  'id' | 'info' | 'onChange' | 'value'
>

export const PlainPinInput = React.memo(function PlainPinInput(
  props: PlainPinInputProps
) {
  return (
    <InputField
      autoComplete="off"
      data-qa="pin-input"
      type="password"
      inputMode="numeric"
      width="s"
      maxLength={4}
      {...props}
    />
  )
})

interface PlainPinInputF
  extends Omit<PlainPinInputProps, 'value' | 'onChange'> {
  bind: BoundFormState<string>
}

export const PlainPinInputF = React.memo(function PlainPinInputF({
  bind,
  ...props
}: PlainPinInputF) {
  return (
    <InputFieldF
      autoComplete="off"
      data-qa="pin-input"
      type="password"
      inputMode="numeric"
      width="s"
      maxLength={4}
      bind={bind}
      {...props}
    />
  )
})
