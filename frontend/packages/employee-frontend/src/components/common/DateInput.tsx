// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { isValid, parse } from 'date-fns'
import LocalDate from '@evaka/lib-common/src/local-date'
import { InputField } from '~components/shared/alpha'
import { autoComplete } from '../../utils/date'
import './DateInput.scss'

function parseFi(str: string) {
  return parse(str, 'dd.MM.yyyy', new Date())
}

// matches all strings of format dd.MM.yyyy
// unfortunately date-fns parse treats M and MM formats similarly
// so we have to use a regex
const dateRegex = /^\d{2}\.\d{2}\.\d{4}$/

function isValidDate(str: string) {
  return dateRegex.test(str) && isValid(parseFi(str))
}

function autoCompleteDate(previous: string, next: string) {
  // do auto complete only if next value is previous with a single character appended to it
  return next.length - 1 === previous.length &&
    next.substr(0, previous.length) === previous
    ? autoComplete(next)
    : next
}

interface Props {
  initial?: LocalDate
  onChange: (date?: LocalDate | 'invalid') => void
  errorMessage?: string
  dataQa?: string
}

function DateInput({ initial, onChange, errorMessage, dataQa }: Props) {
  const [inputValue, setInputValue] = useState(initial ? initial.format() : '')

  return (
    <div className="date-input-container">
      <InputField
        value={inputValue}
        onChange={({ target }) => {
          const value = autoCompleteDate(inputValue, target.value)

          setInputValue(value)

          if (value && !isValidDate(value)) {
            onChange('invalid')
          } else {
            onChange(
              value ? LocalDate.fromSystemTzDate(parseFi(value)) : undefined
            )
          }
        }}
        state={errorMessage ? 'error' : undefined}
        message={errorMessage}
        placeholder={'01.01.2000'}
        dataQa={dataQa}
      />
    </div>
  )
}

export default DateInput
