// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faChevronDown } from 'icon-set'
import Colors from '~components/shared/Colors'

type CommonProps = {
  className?: string
  id?: string
  options: Array<{
    label: string
    value: string
  }>
  onChange: (e: React.ChangeEvent<HTMLSelectElement>) => void
}

type NonNullableValue = {
  value: string
}

type NullableValue = {
  value?: string
  placeholder: string
}

type Props = CommonProps & (NonNullableValue | NullableValue)

export default React.memo(function Select({
  className,
  id,
  value,
  options,
  onChange,
  ...props
}: Props) {
  return (
    <Wrapper className={className}>
      <StyledSelect id={id} value={value} onChange={onChange}>
        {'placeholder' in props && (
          <option value="">{props.placeholder}</option>
        )}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </StyledSelect>
      <Icon size="sm" icon={faChevronDown} />
    </Wrapper>
  )
})

const Wrapper = styled.div`
  position: relative;
`

const StyledSelect = styled.select`
  appearance: none;
  background-color: transparent;
  display: block;
  font-size: 1rem;
  height: 2.25em;
  width: 100%;
  padding: 6px 26px 6px 8px;
  border: 1px solid ${Colors.greyscale.dark};
  border-width: 0 0 1px 0;
  box-shadow: none;
`

const Icon = styled(FontAwesomeIcon)`
  position: absolute;
  right: 8px;
  top: 12px;
`
