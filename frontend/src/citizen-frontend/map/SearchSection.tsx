// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import {
  Language,
  ProviderType,
  PublicUnit
} from 'lib-common/generated/api-types/daycare'
import { SelectionChip } from 'lib-components/atoms/Chip'
import { Button } from 'lib-components/atoms/buttons/Button'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Radio from 'lib-components/atoms/form/Radio'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { fontWeights, H1, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { mapConfig } from 'lib-customizations/citizen'
import colors from 'lib-customizations/common'
import { faAngleDown, faAngleUp, faArrowLeft } from 'lib-icons'

import { useTranslation } from '../localization'
import SearchInput from '../map/SearchInput'

import { CareTypeOption, MapAddress } from './MapView'

interface Props {
  allUnits: Result<PublicUnit[]>
  careType: CareTypeOption
  setCareType: (c: CareTypeOption) => void
  languages: Language[]
  setLanguages: (val: Language[]) => void
  providerTypes: ProviderType[]
  setProviderTypes: (val: ProviderType[]) => void
  shiftCare: boolean
  setShiftCare: (val: boolean) => void
  selectedAddress: MapAddress | null
  setSelectedAddress: (address: MapAddress | null) => void
  setSelectedUnit: (u: PublicUnit | null) => void
  navigateBack: string
}

export default React.memo(function SearchSection({
  allUnits,
  careType,
  setCareType,
  languages,
  setLanguages,
  providerTypes,
  setProviderTypes,
  shiftCare,
  setShiftCare,
  selectedAddress,
  setSelectedAddress,
  setSelectedUnit,
  navigateBack
}: Props) {
  const t = useTranslation()

  const [showMoreFilters, setShowMoreFilters] = useState<boolean>(false)

  return (
    <Wrapper opaque>
      {!!navigateBack && (
        <Link to={navigateBack}>
          <Button
            appearance="inline"
            text={t.common.return}
            icon={faArrowLeft}
            onClick={() => undefined}
          />
        </Link>
      )}
      <Gap size="s" />
      <H1 noMargin>{t.map.title}</H1>
      <P data-qa="map-main-info">
        {t.map.mainInfo}
        <PrivateUnitInfo>{t.map.privateUnitInfo}</PrivateUnitInfo>
      </P>

      <FixedSpaceColumn spacing="xs">
        <Label>{t.map.searchLabel}</Label>
        <SearchInput
          allUnits={allUnits}
          selectedAddress={selectedAddress}
          setSelectedAddress={setSelectedAddress}
          setSelectedUnit={setSelectedUnit}
        />
      </FixedSpaceColumn>

      <Gap size="m" />

      <FixedSpaceColumn spacing="xs">
        <Label>{t.map.careType}</Label>
        <FixedSpaceFlexWrap>
          {mapConfig.careTypeFilters.map((careTypeFilter) => (
            <Radio
              key={`map-filter-${careTypeFilter}`}
              data-qa={`map-filter-${careTypeFilter}`}
              checked={careType === careTypeFilter}
              label={t.map.careTypes[careTypeFilter]}
              onChange={() => setCareType(careTypeFilter)}
            />
          ))}
        </FixedSpaceFlexWrap>
      </FixedSpaceColumn>

      <Gap size="xs" />

      <FixedSpaceColumn spacing="xs">
        <Label>{t.map.language}</Label>
        <FixedSpaceRow>
          <SelectionChip
            data-qa="map-filter-fi"
            text={t.common.unit.languagesShort.fi}
            selected={languages.includes('fi')}
            onChange={(selected) => {
              const nextValue = languages.filter((l) => l !== 'fi')
              if (selected) nextValue.push('fi')
              setLanguages(nextValue)
            }}
          />
          <SelectionChip
            data-qa="map-filter-sv"
            text={t.common.unit.languagesShort.sv}
            selected={languages.includes('sv')}
            onChange={(selected) => {
              const nextValue = languages.filter((l) => l !== 'sv')
              if (selected) nextValue.push('sv')
              setLanguages(nextValue)
            }}
          />
        </FixedSpaceRow>
      </FixedSpaceColumn>

      {showMoreFilters && (
        <>
          <Gap size="m" />

          <FixedSpaceColumn spacing="xs">
            <Label>{t.map.providerType}</Label>
            <FixedSpaceRow>
              <FixedSpaceFlexWrap>
                {mapConfig.unitProviderTypeFilters.map((type) => (
                  <SelectionChip
                    key={type}
                    text={t.map.providerTypes[type]}
                    selected={providerTypes.includes(type)}
                    onChange={(selected) => {
                      const nextValue = providerTypes.filter((t) => t !== type)
                      if (selected) nextValue.push(type)
                      setProviderTypes(nextValue)
                    }}
                  />
                ))}
              </FixedSpaceFlexWrap>
            </FixedSpaceRow>
          </FixedSpaceColumn>

          <Gap size="m" />

          <FixedSpaceColumn spacing="xs">
            <Label>{t.map.shiftCareTitle}</Label>
            <Checkbox
              label={t.map.shiftCareLabel}
              checked={shiftCare}
              onChange={(checked) => setShiftCare(checked)}
            />
          </FixedSpaceColumn>
        </>
      )}

      <Gap size="m" />

      <Button
        appearance="inline"
        order="text-icon"
        onClick={() => setShowMoreFilters(!showMoreFilters)}
        text={showMoreFilters ? t.map.showLessFilters : t.map.showMoreFilters}
        icon={showMoreFilters ? faAngleUp : faAngleDown}
      />
    </Wrapper>
  )
})

const Wrapper = styled(ContentArea)`
  box-sizing: border-box;
  width: 100%;
`

const PrivateUnitInfo = styled.span`
  font-weight: ${fontWeights.semibold};
  font-size: 14px;
  line-height: 21px;
  color: ${colors.grayscale.g70};
  display: block;
`
