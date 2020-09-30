// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ChangeEvent } from 'react'
import styled from 'styled-components'
import classNames from 'classnames'
import { EspooColours } from '~utils/colours'
import { isValidCents } from '~utils/money'

type InputProps = { error: boolean }

const Input = styled.input<InputProps>`
  text-align: right;
  border-color: ${(props) =>
    props.error ? EspooColours.red : EspooColours.grey};

  &:focus {
    border-color: ${(props) =>
      props.error ? EspooColours.red : EspooColours.espooTurqoise};
  }
`

type Props = {
  className?: string
  value?: string
  onChange: (v: string) => void
  allowEmpty: boolean
  dataQa?: string
}

const EuroInput = React.memo(function EuroInput({
  className,
  value,
  onChange,
  allowEmpty,
  dataQa
}: Props) {
  return (
    <Input
      className={classNames('input', className)}
      placeholder="0"
      value={value}
      onChange={(e: ChangeEvent<HTMLInputElement>) => onChange(e.target.value)}
      error={
        allowEmpty
          ? !!value && !isValidCents(value)
          : !value || !isValidCents(value)
      }
      data-qa={dataQa}
    />
  )
})

export default EuroInput
