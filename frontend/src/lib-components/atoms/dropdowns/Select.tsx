// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'
import styled from 'styled-components'
import classNames from 'classnames'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faChevronDown } from 'lib-icons'
import { borderStyles, DropdownProps, Root } from './shared'

interface SelectProps<T> extends DropdownProps<T, HTMLSelectElement> {
  getItemValue?: T extends string | number ? never : (item: T) => string
}

export default function Select<T>(props: SelectProps<T>) {
  const {
    id,
    items,
    selectedItem,
    onChange,
    disabled,
    getItemLabel = defaultGetItemLabel,
    getItemValue = defaultGetItemLabel,
    getItemDataQa,
    name,
    onFocus,
    fullWidth,
    'data-qa': dataQa
  } = props

  const onSelectedItemChange = useCallback(
    (event: React.ChangeEvent<HTMLSelectElement>) => {
      const newSelectedItem =
        items.find((item) => getItemValue(item) === event.target.value) ?? null
      onChange(newSelectedItem)
    },
    [items, getItemValue, onChange]
  )

  return (
    <Root data-qa={dataQa} className={classNames({ 'full-width': fullWidth })}>
      <Wrapper>
        <StyledSelect
          id={id}
          name={name}
          value={selectedItem ? getItemValue(selectedItem) : ''}
          onChange={onSelectedItemChange}
          disabled={disabled}
          onFocus={onFocus}
          data-qa={dataQa}
        >
          {'placeholder' in props && (
            <option value="">{props.placeholder}</option>
          )}
          {items.map((item) => {
            const itemValue = getItemValue(item)

            return (
              <option
                key={itemValue}
                value={itemValue}
                data-qa={getItemDataQa?.(item)}
              >
                {getItemLabel(item)}
              </option>
            )
          })}
        </StyledSelect>
        <Icon size="sm" icon={faChevronDown} />
      </Wrapper>
    </Root>
  )
}

function defaultGetItemLabel<T>(item: T) {
  return String(item)
}

const Wrapper = styled.div`
  position: relative;
`

const StyledSelect = styled.select`
  appearance: none;
  color: ${({ theme }) => theme.colors.greyscale.darkest};
  background-color: ${({ theme }) => theme.colors.greyscale.white};
  display: block;
  font-size: 1rem;
  width: 100%;
  padding: 8px 30px 8px 12px;
  ${borderStyles}
  box-shadow: none;
`

const Icon = styled(FontAwesomeIcon)`
  pointer-events: none;
  position: absolute;
  right: 12px;
  top: 12px;
`
