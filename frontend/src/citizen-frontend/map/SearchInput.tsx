// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import classNames from 'classnames'
import { combine, Result, Success } from 'lib-common/api'
import { PublicUnit } from 'lib-common/generated/api-types/daycare'
import { useDebounce } from 'lib-common/utils/useDebounce'
import { useApiState } from 'lib-common/utils/useRestApi'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { fasMapMarkerAlt } from 'lib-icons'
import React, { FocusEventHandler, useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'
import { useTranslation } from '../localization'
import { queryAutocomplete } from './api'
import { MapAddress } from './MapView'

type Props = {
  allUnits: Result<PublicUnit[]>
  selectedAddress: MapAddress | null
  setSelectedAddress: (address: MapAddress | null) => void
  setSelectedUnit: (u: PublicUnit | null) => void
}

export default React.memo(function SearchInput({
  allUnits,
  selectedAddress,
  setSelectedAddress,
  setSelectedUnit
}: Props) {
  const t = useTranslation()
  const [inputString, setInputString] = useState('')
  const debouncedInputString = useDebounce(inputString, 500)

  const [addressOptions] = useApiState(
    () => fetchAddressOptions(debouncedInputString),
    [debouncedInputString]
  )

  const unitOptions = useMemo(() => {
    return allUnits
      .map((units) =>
        units.map((u) => ({
          unit: {
            id: u.id,
            name: u.name
          },
          streetAddress: u.streetAddress,
          postalCode: u.postalCode,
          postOffice: u.postOffice,
          coordinates: u.location ?? { lat: 0, lon: 0 }
        }))
      )
      .getOrElse([])
  }, [allUnits])

  const filteredUnitOptions = useMemo(() => {
    if (debouncedInputString.length < 3) return []

    return unitOptions.filter(({ unit }) =>
      unit?.name.toLowerCase().includes(debouncedInputString.toLowerCase())
    )
  }, [unitOptions, debouncedInputString])

  const options = useMemo(() => {
    return [...filteredUnitOptions, ...addressOptions.getOrElse([])]
  }, [filteredUnitOptions, addressOptions])

  const clearSelection = useCallback(() => {
    setInputString('')
    setSelectedAddress(null)
    setSelectedUnit(null)
  }, [setSelectedAddress, setSelectedUnit])

  const onInputChange = useCallback(
    (inputValue: string) => {
      if (inputValue.length === 0) {
        setSelectedAddress(null)
      }
      setInputString(inputValue)
    },
    [setSelectedAddress]
  )

  const onChange = useCallback(
    (item: MapAddress | null) => {
      if (!item) {
        return clearSelection()
      }

      if (item.unit) {
        setSelectedAddress(null)
        if (allUnits.isSuccess) {
          setSelectedUnit(
            allUnits.value.find((u) => u.id === item.unit?.id) ?? null
          )
        }
      } else {
        setSelectedUnit(null)
        setSelectedAddress(item)
        setInputString(`${item.streetAddress}, ${item.postOffice}`)
      }
    },
    [allUnits, clearSelection, setSelectedAddress, setSelectedUnit]
  )

  const menuEmptyLabel =
    debouncedInputString.length > 0
      ? t.map.noResults
      : `${t.map.keywordRequired}...`

  return (
    <div data-qa="map-search-input">
      <Combobox
        clearable
        isLoading={combine(addressOptions, allUnits).isLoading}
        onInputChange={onInputChange}
        items={options}
        getItemLabel={getItemLabel}
        selectedItem={selectedAddress}
        onChange={onChange}
        placeholder={t.map.searchPlaceholder}
        menuEmptyLabel={menuEmptyLabel}
        onFocus={onFocus}
      >
        {customComponents}
      </Combobox>
    </div>
  )
})

async function fetchAddressOptions(input: string) {
  if (input.length > 0) {
    return await queryAutocomplete(input)
  } else {
    return Success.of([])
  }
}

const onFocus: FocusEventHandler<HTMLInputElement> = (e) => {
  e.target.select()
}

const getItemLabel = (item: MapAddress) =>
  item.unit ? item.unit.name : `${item.streetAddress}, ${item.postOffice ?? ''}`

const customComponents = {
  menuItem: function MenuItem({
    item,
    highlighted
  }: {
    item: MapAddress
    highlighted: boolean
  }) {
    const addressLabel = [item.streetAddress, item.postOffice].join(', ')

    return (
      <OptionWrapper
        data-qa={
          item.unit ? `map-search-${item.unit.id}` : 'map-search-address'
        }
        data-address={item.streetAddress}
        key={item.unit?.id ?? addressLabel}
        className={classNames({ focused: highlighted })}
      >
        <OptionContents
          label={item.unit?.name ?? addressLabel}
          secondaryText={item.unit ? addressLabel : undefined}
          isUnit={!!item.unit}
        />
      </OptionWrapper>
    )
  }
}

const OptionWrapper = styled.div`
  cursor: pointer;

  &:hover,
  &.focused {
    background-color: ${colors.main.lighter};
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
  font-weight: ${fontWeights.semibold};
  color: ${colors.greyscale.dark};
`
