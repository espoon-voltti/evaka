// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import classNames from 'classnames'
import React from 'react'
import ReactSelect, { Props } from 'react-select'
import styled, { useTheme } from 'styled-components'

import { scrollIntoViewSoftKeyboard } from 'lib-common/utils/scrolling'
import { tabletMin } from 'lib-components/breakpoints'

import { FixedSpaceColumn, FixedSpaceRow } from '../../layout/flex-helpers'
import { fontWeights } from '../../typography'
import { defaultMargins } from '../../white-space'

import { StaticCheckBox } from './Checkbox'

interface MultiSelectProps<T> {
  value: readonly T[]
  options: readonly T[]
  getOptionId: (value: T) => string
  getOptionLabel: (value: T) => string
  getOptionSecondaryText?: (value: T) => string
  onChange: (selected: T[]) => void
  maxSelected?: number
  placeholder: string
  noOptionsMessage?: string
  showValuesInInput?: boolean
  closeMenuOnSelect?: Props<never>['closeMenuOnSelect']
  isClearable?: Props<never>['isClearable']
  inputId?: Props<never>['inputId']
  'data-qa'?: string
  autoFocus?: boolean
}

function MultiSelect<T>({
  value,
  options,
  getOptionId,
  getOptionLabel,
  getOptionSecondaryText,
  onChange,
  closeMenuOnSelect,
  noOptionsMessage,
  maxSelected,
  showValuesInInput: showSelectedValues,
  autoFocus,
  ...props
}: MultiSelectProps<T>) {
  const { colors } = useTheme()

  // If MsEdge's translation feature is active and react-select uses aria live
  // messaging, the React app crashes when the dropdown menu is closed. Because
  // of this we disable aria live messaging when MsEdge's translation feature is
  // active.
  // https://github.com/JedWatson/react-select/issues/5816
  const msEdgeTranslationActive = !!document.querySelector('*[_msttexthash]')
  const ariaLiveMessages = msEdgeTranslationActive
    ? {
        guidance: () => '',
        onChange: () => '',
        onFilter: () => '',
        onFocus: () => ''
      }
    : undefined

  return (
    <div data-qa={props['data-qa']} className="multi-select">
      <ReactSelect
        ariaLiveMessages={ariaLiveMessages}
        styles={{
          menu: (base) => ({
            ...base,
            zIndex: 15
          }),
          clearIndicator: (base) => ({
            ...base,
            [`@media (max-width: ${tabletMin})`]: {
              display: 'none'
            }
          }),
          group: (base) => ({
            ...base,
            position: 'relative',
            ':not(:last-child):after': {
              position: 'absolute',
              content: '""',
              bottom: '-2px',
              left: defaultMargins.s,
              right: defaultMargins.s,
              borderBottom: `1px solid ${colors.grayscale.g15}`
            }
          })
        }}
        autoFocus={autoFocus}
        isMulti
        isSearchable={true}
        hideSelectedOptions={false}
        backspaceRemovesValue={false}
        closeMenuOnSelect={closeMenuOnSelect ?? false}
        noOptionsMessage={() => (
          <span data-qa="no-options">{noOptionsMessage ?? 'Ei tuloksia'}</span>
        )}
        getOptionLabel={getOptionLabel}
        getOptionValue={getOptionId}
        value={value}
        tabSelectsValue={false}
        onFocus={(ev) => {
          const parentContainer = ev.target.closest('.multi-select')

          if (parentContainer) {
            scrollIntoViewSoftKeyboard(parentContainer, 'start')
          }
        }}
        options={[
          {
            options: value
          },
          {
            options:
              maxSelected === undefined || value.length < maxSelected
                ? options.filter(
                    (o) =>
                      !value
                        .map((o2) => getOptionId(o2))
                        .includes(getOptionId(o))
                  )
                : []
          }
        ]}
        onChange={(selected) => {
          const selectionsArray =
            selected && 'length' in selected ? (selected as T[]) : []
          onChange(selectionsArray)
        }}
        filterOption={({ data }, q) =>
          getOptionLabel(data).toLowerCase().includes(q.toLowerCase())
        }
        controlShouldRenderValue={showSelectedValues ?? true}
        components={{
          Option: function Option({ innerRef, innerProps, ...props }) {
            const data = props.data

            return (
              <OptionWrapper
                {...innerProps}
                data-qa="option"
                data-id={getOptionId(data)}
                ref={innerRef}
                key={getOptionId(data)}
                className={classNames({ focused: props.isFocused })}
              >
                <OptionContents
                  label={getOptionLabel(data)}
                  secondaryText={
                    getOptionSecondaryText && getOptionSecondaryText(data)
                  }
                  selected={props.isSelected}
                />
              </OptionWrapper>
            )
          }
        }}
        {...props}
      />
    </div>
  )
}

export default React.memo(MultiSelect) as typeof MultiSelect

const OptionWrapper = styled.div`
  cursor: pointer;

  &:hover,
  &.focused {
    background-color: ${(p) => p.theme.colors.main.m4};
  }

  padding: ${defaultMargins.xxs} ${defaultMargins.s};
`

const OptionContents = React.memo(function Option({
  label,
  secondaryText,
  selected
}: {
  label: string
  secondaryText?: string
  selected: boolean
}) {
  return (
    <FixedSpaceRow alignItems="center">
      <StaticCheckBox checked={selected} />
      <FixedSpaceColumn spacing="zero">
        <span>{label}</span>
        {!!secondaryText && <SecondaryText>{secondaryText}</SecondaryText>}
      </FixedSpaceColumn>
    </FixedSpaceRow>
  )
})

const SecondaryText = styled.span`
  font-size: 14px;
  line-height: 21px;
  font-weight: ${fontWeights.semibold};
  color: ${(p) => p.theme.colors.grayscale.g70};
`
