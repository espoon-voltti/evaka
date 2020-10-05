// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { isValidCents } from '~utils/money'
import InputField from '~components/shared/atoms/form/InputField'

type Props = {
  className?: string
  value?: string
  onChange: (v: string) => void
  allowEmpty: boolean
  dataQa?: string
}

const EuroInput = React.memo(function EuroInput({
  value,
  onChange,
  allowEmpty,
  dataQa
}: Props) {
  return (
    <InputField
      value={value ?? ''}
      placeholder="0"
      onChange={(value) => onChange(value)}
      info={
        allowEmpty
          ? !!value && !isValidCents(value)
            ? { text: 'Virheellinen arvo', status: 'warning' }
            : undefined
          : !value || !isValidCents(value)
          ? { text: 'Virheellinen arvo', status: 'warning' }
          : undefined
      }
      data-qa={dataQa}
    />
  )
})

export default EuroInput
