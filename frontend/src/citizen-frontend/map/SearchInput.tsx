// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useState } from 'react'
import styled from 'styled-components'
import ReactSelect, {
  components,
  InputActionMeta,
  InputProps
} from 'react-select'

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import { Result, Success } from 'lib-common/api'
import { PublicUnit } from 'lib-common/api-types/units/PublicUnit'
import { useRestApi } from 'lib-common/utils/useRestApi'
import colors from 'lib-components/colors'
import { defaultMargins } from 'lib-components/white-space'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { fasMapMarkerAlt } from 'lib-icons'
import { queryAutocomplete } from '../map/api'
import { MapAddress } from '../map/MapView'
import { useTranslation } from '../localization'
import { useDebounce } from 'lib-common/utils/useDebounce'

type Props = {
  allUnits: Result<PublicUnit[]>
  selectedAddress: MapAddress | null
  setSelectedAddress: (address: MapAddress | null) => void
  setSelectedUnit: (u: PublicUnit | null) => void
}

const Input = (props: InputProps) => (
  <components.Input {...props} isHidden={false} />
)

export default React.memo(function SearchInput({
  allUnits,
  selectedAddress,
  setSelectedAddress,
  setSelectedUnit
}: Props) {
  const t = useTranslation()
  const [inputString, setInputString] = useState('')
  const debouncedInputString = useDebounce(inputString, 500)

  const [addressOptions, setAddressOptions] = useState<Result<MapAddress[]>>(
    Success.of([])
  )
  const loadOptions = useRestApi(queryAutocomplete, setAddressOptions)
  useEffect(() => {
    if (debouncedInputString.length > 0) {
      loadOptions(debouncedInputString)
    } else {
      setAddressOptions(Success.of([]))
    }
  }, [debouncedInputString, loadOptions])

  const getUnitOptions = useCallback(() => {
    if (debouncedInputString.length < 3 || !allUnits.isSuccess) return []

    return allUnits.value
      .filter((u) =>
        u.name.toLowerCase().includes(debouncedInputString.toLowerCase())
      )
      .slice(0, 5)
      .map<MapAddress>((u) => ({
        unit: {
          id: u.id,
          name: u.name
        },
        streetAddress: u.streetAddress,
        postalCode: u.postalCode,
        postOffice: u.postOffice,
        coordinates: u.location ?? { lat: 0, lon: 0 }
      }))
  }, [allUnits, debouncedInputString])

  const selectOption = (option: MapAddress) => {
    if (option.unit) {
      setSelectedAddress(null)
      if (allUnits.isSuccess) {
        setSelectedUnit(
          allUnits.value.find((u) => u.id === option.unit?.id) ?? null
        )
      }
    } else {
      setSelectedUnit(null)
      setSelectedAddress(option)
      setInputString(`${option.streetAddress}, ${option.postOffice}`)
    }
  }

  const clearSelection = () => {
    setSelectedAddress(null)
    setSelectedUnit(null)
  }

  const onFocus = (e: React.FocusEvent<HTMLElement>) => {
    ;(e as React.FocusEvent<HTMLInputElement>).target.select()
  }

  const onInputChange = (inputValue: string, { action }: InputActionMeta) => {
    if (action === 'input-blur') {
      setInputString(
        selectedAddress
          ? `${selectedAddress.streetAddress}, ${selectedAddress.postOffice}`
          : ''
      )
    } else if (action === 'input-change') {
      if (inputValue.length === 0) {
        setSelectedAddress(null)
      }
      setInputString(inputValue)
    }
  }

  return (
    <div data-qa="map-search-input">
      <ReactSelect
        isSearchable
        isClearable
        closeMenuOnSelect
        isLoading={addressOptions.isLoading}
        inputValue={inputString}
        onInputChange={onInputChange}
        options={[
          {
            options: getUnitOptions()
          },
          {
            options: addressOptions.isSuccess ? addressOptions.value : []
          }
        ]}
        getOptionLabel={(option) =>
          option.unit
            ? option.unit.name
            : `${option.streetAddress}, ${option.postOffice ?? ''}`
        }
        value={selectedAddress}
        onChange={(selected) => {
          if (!selected) {
            clearSelection()
          } else if ('length' in selected) {
            if (selected.length > 0) {
              selectOption(selected[0])
            } else clearSelection()
          } else {
            selectOption(selected)
          }
        }}
        placeholder={t.map.searchPlaceholder}
        noOptionsMessage={() => t.map.noResults}
        components={{
          Input,
          Option: function Option({ innerRef, innerProps, ...props }) {
            const option = props.data as MapAddress
            const addressLabel = [option.streetAddress, option.postOffice].join(
              ', '
            )

            return (
              <OptionWrapper
                data-qa={
                  option.unit
                    ? `map-search-${option.unit.id}`
                    : 'map-search-address'
                }
                data-address={option.streetAddress}
                ref={innerRef}
                {...innerProps}
                key={option.unit?.id ?? addressLabel}
                className={classNames({ focused: props.isFocused })}
              >
                <OptionContents
                  label={option.unit?.name ?? addressLabel}
                  secondaryText={option.unit ? addressLabel : undefined}
                  isUnit={!!option.unit}
                />
              </OptionWrapper>
            )
          }
        }}
        onFocus={onFocus}
      />
    </div>
  )
})

const OptionWrapper = styled.div`
  cursor: pointer;
  &:hover,
  &.focused {
    background-color: ${colors.blues.lighter};
  }
  padding: ${defaultMargins.xxs} ${defaultMargins.s};
  margin-bottom: ${defaultMargins.xs};
`

const OptionContents = React.memo(function Option({
  label,
  secondaryText,
  isUnit
}: {
  label: string
  secondaryText?: string
  isUnit: boolean
}) {
  return (
    <FixedSpaceRow alignItems={'center'}>
      {isUnit && (
        <FontAwesomeIcon
          icon={fasMapMarkerAlt}
          color={colors.primary}
          style={{ fontSize: '24px' }}
        />
      )}
      <FixedSpaceColumn spacing="zero">
        <span>{label}</span>
        {secondaryText && <SecondaryText>{secondaryText}</SecondaryText>}
      </FixedSpaceColumn>
    </FixedSpaceRow>
  )
})

const SecondaryText = styled.span`
  font-size: 14px;
  line-height: 21px;
  font-weight: 600;
  color: ${colors.greyscale.dark};
`
