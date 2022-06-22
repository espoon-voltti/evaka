// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, { useCallback } from 'react'
import styled from 'styled-components'

import { faChevronDown } from 'lib-icons'

import { borderStyles, DropdownProps, Root } from './shared'

type SelectProps<T> = DropdownProps<T, HTMLSelectElement> &
  (T extends string | number
    ? { getItemValue?: never }
    : { getItemValue: (item: T) => string })

function Select<T>(props: SelectProps<T>) {
  const {
    id,
    items,
    selectedItem,
    onChange,
    disabled,
    getItemLabel = defaultGetItemLabel,
    getItemDataQa,
    name,
    onFocus,
    fullWidth,
    'data-qa': dataQa
  } = props

  const getItemValue =
    'getItemValue' in props && props.getItemValue
      ? props.getItemValue
      : defaultGetItemLabel

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
  color: ${(p) => p.theme.colors.grayscale.g100};
  background-color: ${(p) => p.theme.colors.grayscale.g0};
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
  font-size: 1rem;
  right: 12px;
  top: 12px;
`

export default React.memo(Select) as typeof Select
