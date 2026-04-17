// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import type { Result } from 'lib-common/api'
import type {
  Language,
  ProviderType,
  PublicUnit
} from 'lib-common/generated/api-types/daycare'
import { SelectionChip } from 'lib-components/atoms/Chip'
import { Button } from 'lib-components/atoms/buttons/Button'
import { InlineInternalLinkButton } from 'lib-components/atoms/buttons/InlineLinkButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Radio from 'lib-components/atoms/form/Radio'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import LanguageFilterChips from 'lib-components/molecules/LanguageFilterChips'
import { fontWeights, H1, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { mapConfig } from 'lib-customizations/citizen'
import colors from 'lib-customizations/common'
import { faAngleDown, faAngleUp, faArrowLeft } from 'lib-icons'

import { useTranslation } from '../localization'

import type { CareTypeOption, MapAddress } from './MapView'
import SearchInput from './SearchInput'

interface Props {
  allUnits: Result<PublicUnit[]>
  availableLanguages: Set<Language>
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
  availableLanguages,
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
    <Wrapper $opaque>
      {!!navigateBack && (
        <InlineInternalLinkButton
          to={navigateBack}
          text={t.common.return}
          icon={faArrowLeft}
        />
      )}
      <Gap $size="s" />
      <H1 $noMargin>{t.map.title}</H1>
      <P data-qa="map-main-info">
        {t.map.mainInfo}
        <PrivateUnitInfo>{t.map.privateUnitInfo}</PrivateUnitInfo>
      </P>

      <FixedSpaceColumn $spacing="xs">
        <Label id="map-search-label" htmlFor="map-search-input">
          {t.map.searchLabel}
        </Label>
        {/* The downshift dropdown needs the input to have an id and aria-labelledby for accessibility */}
        <SearchInput
          id="map-search-input"
          aria-labelledby="map-search-label"
          allUnits={allUnits}
          selectedAddress={selectedAddress}
          setSelectedAddress={setSelectedAddress}
          setSelectedUnit={setSelectedUnit}
        />
      </FixedSpaceColumn>

      <Gap $size="m" />

      <FixedSpaceColumn
        $spacing="xs"
        role="group"
        aria-labelledby="map-care-type-label"
      >
        <Label id="map-care-type-label">{t.map.careType}</Label>
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

      <Gap $size="xs" />

      <LanguageFilterChips
        availableLanguages={availableLanguages}
        selected={languages}
        onChange={setLanguages}
        getLabel={(lang) => t.common.unit.languagesShort[lang]}
        label={t.map.language}
        labelId="map-language-label"
        dataQa="map-language-filter"
      />

      {showMoreFilters && (
        <>
          <Gap $size="m" />

          <FixedSpaceColumn
            $spacing="xs"
            role="group"
            aria-labelledby="map-provider-type-label"
          >
            <Label id="map-provider-type-label">{t.map.providerType}</Label>
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

          <Gap $size="m" />

          <FixedSpaceColumn $spacing="xs">
            <Label>{t.map.shiftCareTitle}</Label>
            <Checkbox
              label={t.map.shiftCareLabel}
              checked={shiftCare}
              onChange={(checked) => setShiftCare(checked)}
            />
          </FixedSpaceColumn>
        </>
      )}

      <Gap $size="m" />

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
