// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { memo } from 'react'
import styled from 'styled-components'

import type { ComboboxProps } from 'lib-components/atoms/dropdowns/Combobox'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import type { BaseProps } from 'lib-components/utils'

export interface SelectOption {
  value: string
  label: string
}

const Container = styled.div<{
  fullWidth?: boolean
}>`
  position: relative;
  ${(p) => (p.fullWidth ? 'width: 100%;' : '')}
  min-width: 150px;

  select {
    padding-right: 20px;
  }
`

const getItemLabel = ({ label }: SelectOption) => label

const Select = memo(function Select({
  'data-qa': dataQa,
  className,
  fullWidth,
  ...rest
}: ComboboxProps<SelectOption> & BaseProps) {
  return (
    <Container fullWidth={fullWidth} data-qa={dataQa} className={className}>
      <Combobox getItemLabel={getItemLabel} fullWidth={fullWidth} {...rest} />
    </Container>
  )
})

export default Select
