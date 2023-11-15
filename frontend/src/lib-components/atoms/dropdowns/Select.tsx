// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import React, {
  ChangeEvent,
  FocusEventHandler,
  useCallback,
  useMemo
} from 'react'
import styled from 'styled-components'

import { OneOf } from 'lib-common/form/form'
import { BoundForm } from 'lib-common/form/hooks'
import { faChevronDown } from 'lib-icons'

import UnderRowStatusIcon from '../StatusIcon'
import { InputFieldUnderRow } from '../form/InputField'

import { borderStyles, DropdownProps, Root } from './shared'

type SelectProps<T> = DropdownProps<T, HTMLSelectElement> &
  (T extends string | number
    ? { getItemValue?: never }
    : { getItemValue: (item: T) => string })

function Select<T>(props: SelectProps<T>) {
  const {
    items,
    selectedItem,
    onChange,
    getItemLabel = defaultGetItemLabel,
    getItemDataQa,
    ...rest
  } = props

  const getItemValue =
    'getItemValue' in props && props.getItemValue
      ? props.getItemValue
      : defaultGetItemLabel

  const onSelectedItemChange = useCallback(
    (newValue: string) => {
      const newSelectedItem =
        items.find((item) => getItemValue(item) === newValue) ?? null
      onChange(newSelectedItem)
    },
    [items, getItemValue, onChange]
  )

  const fooItems = useMemo(
    () =>
      items.map((item) => ({
        value: getItemValue(item),
        label: getItemLabel(item),
        dataQa: getItemDataQa?.(item)
      })),
    [getItemDataQa, getItemLabel, getItemValue, items]
  )

  return (
    <RawSelect
      options={fooItems}
      value={selectedItem ? getItemValue(selectedItem) : ''}
      onChange={onSelectedItemChange}
      {...rest}
    />
  )
}

interface RawProps {
  id?: string
  options: { value: string; label: string; dataQa: string | undefined }[]
  value: string
  onChange: (value: string) => void
  disabled?: boolean
  placeholder?: string
  name?: string
  onFocus?: FocusEventHandler<HTMLSelectElement>
  fullWidth?: boolean
  'data-qa'?: string
}

const RawSelect = React.memo(function RawSelect({
  id,
  options,
  value,
  onChange,
  disabled,
  placeholder,
  name,
  onFocus,
  fullWidth,
  'data-qa': dataQa
}: RawProps) {
  const handleChange = useCallback(
    (e: ChangeEvent<HTMLSelectElement>) => {
      onChange(e.target.value)
    },
    [onChange]
  )
  return (
    <Root data-qa={dataQa} className={classNames({ 'full-width': fullWidth })}>
      <Wrapper>
        <StyledSelect
          id={id}
          name={name}
          value={value}
          onChange={handleChange}
          disabled={disabled}
          onFocus={onFocus}
        >
          {placeholder !== undefined && <option value="">{placeholder}</option>}
          {options.map((item) => (
            <option key={item.value} value={item.value} data-qa={item.dataQa}>
              {item.label}
            </option>
          ))}
        </StyledSelect>
        <Icon size="sm" icon={faChevronDown} />
      </Wrapper>
    </Root>
  )
})

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

interface SelectFProps<T> {
  bind: BoundForm<OneOf<T>>
  id?: string
  disabled?: boolean
  placeholder?: string
  name?: string
  onFocus?: FocusEventHandler<HTMLSelectElement>
  fullWidth?: boolean
  'data-qa'?: string
}

function SelectFC<T>({
  bind: { state, update, inputInfo },
  ...props
}: SelectFProps<T>) {
  const { domValue, options } = state
  const rawOptions = useMemo(
    () =>
      options.map((opt) => ({
        value: opt.domValue,
        label: opt.label,
        dataQa: opt.dataQa
      })),
    [options]
  )
  const onChange = useCallback(
    (value: string) => {
      update((prev) => ({ ...prev, domValue: value }))
    },
    [update]
  )

  const info = inputInfo()
  const infoText = info?.text
  const infoStatus = info?.status

  return (
    <>
      <RawSelect
        {...props}
        options={rawOptions}
        value={domValue}
        onChange={onChange}
      />
      {!!infoText && (
        <InputFieldUnderRow className={classNames(infoStatus)}>
          <span
            data-qa={props['data-qa'] ? `${props['data-qa']}-info` : undefined}
          >
            {infoText}
          </span>
          <UnderRowStatusIcon status={info?.status} />
        </InputFieldUnderRow>
      )}
    </>
  )
}

export const SelectF = React.memo(SelectFC) as typeof SelectFC
