// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import Combobox, {
  ComboboxProps
} from 'lib-components/atoms/dropdowns/Combobox'
import { BaseProps } from 'lib-components/utils'

export interface SelectOption<T extends string> {
  value: T
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

const getItemLabel = ({ label }: SelectOption<string>) => label

function Select_<T extends string>({
  'data-qa': dataQa,
  className,
  fullWidth,
  ...rest
}: ComboboxProps<SelectOption<T>> & BaseProps) {
  return (
    <Container fullWidth={fullWidth} data-qa={dataQa} className={className}>
      <Combobox getItemLabel={getItemLabel} fullWidth={fullWidth} {...rest} />
    </Container>
  )
}

export default React.memo(Select_) as typeof Select_
