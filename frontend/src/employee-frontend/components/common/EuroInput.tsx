// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { isValidCents } from 'lib-common/money'
import InputField from 'lib-components/atoms/form/InputField'

type Props = {
  className?: string
  value?: string
  onChange: (v: string) => void
  allowEmpty: boolean
  'data-qa'?: string
  invalidText?: string
}

const EuroInput = React.memo(function EuroInput({
  value,
  onChange,
  allowEmpty,
  'data-qa': dataQa,
  invalidText = 'Virheellinen arvo'
}: Props) {
  return (
    <InputField
      value={value ?? ''}
      placeholder="0"
      onChange={(value) => onChange(value)}
      info={
        allowEmpty
          ? !!value && !isValidCents(value)
            ? { text: invalidText, status: 'warning' }
            : undefined
          : !value || !isValidCents(value)
            ? { text: invalidText, status: 'warning' }
            : undefined
      }
      align="right"
      data-qa={dataQa}
    />
  )
})

export default EuroInput
