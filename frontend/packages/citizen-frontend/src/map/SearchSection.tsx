import React, { useState } from 'react'
import styled from 'styled-components'
import { Result } from '@evaka/lib-common/src/api'
import { PublicUnit } from '@evaka/lib-common/src/api-types/units/PublicUnit'
import { UnitLanguage } from '@evaka/lib-common/src/api-types/units/enums'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from '@evaka/lib-components/src/layout/flex-helpers'
import colors from '@evaka/lib-components/src/colors'
import { Gap } from '@evaka/lib-components/src/white-space'
import { SelectionChip } from '@evaka/lib-components/src/atoms/Chip'
import HorizontalLine from '@evaka/lib-components/src/atoms/HorizontalLine'
import InlineButton from '@evaka/lib-components/src/atoms/buttons/InlineButton'
import Radio from '@evaka/lib-components/src/atoms/form/Radio'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import { H1, Label, P } from '@evaka/lib-components/src/typography'
import {
  faAngleDown,
  faAngleUp,
  faList,
  fasMapMarkerAlt
} from '@evaka/lib-icons'
import { useTranslation } from '~localization'
import { mapViewBreakpoint, MobileMode } from '~map/const'
import SearchInput from '~map/SearchInput'
import { CareTypeOption, MapAddress, ProviderTypeOption } from '~map/MapView'

type Props = {
  allUnits: Result<PublicUnit[]>
  careType: CareTypeOption
  setCareType: (c: CareTypeOption) => void
  languages: UnitLanguage[]
  setLanguages: (val: UnitLanguage[]) => void
  providerTypes: ProviderTypeOption[]
  setProviderTypes: (val: ProviderTypeOption[]) => void
  shiftCare: boolean
  setShiftCare: (val: boolean) => void
  mobileMode: MobileMode
  setMobileMode: (mode: MobileMode) => void
  selectedAddress: MapAddress | null
  setSelectedAddress: (address: MapAddress | null) => void
  setSelectedUnit: (u: PublicUnit | null) => void
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
  mobileMode,
  setMobileMode,
  selectedAddress,
  setSelectedAddress,
  setSelectedUnit
}: Props) {
  const t = useTranslation()

  const [showMoreFilters, setShowMoreFilters] = useState<boolean>(false)

  return (
    <Wrapper opaque>
      <H1>{t.map.title}</H1>
      <P>{t.map.mainInfo}</P>

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
          <Radio
            dataQa="map-filter-daycare"
            checked={careType === 'DAYCARE'}
            label={t.map.careTypes.DAYCARE}
            onChange={() => setCareType('DAYCARE')}
          />
          <Radio
            dataQa="map-filter-preschool"
            checked={careType === 'PRESCHOOL'}
            label={t.map.careTypes.PRESCHOOL}
            onChange={() => setCareType('PRESCHOOL')}
          />
          <Radio
            dataQa="map-filter-club"
            checked={careType === 'CLUB'}
            label={t.map.careTypes.CLUB}
            onChange={() => setCareType('CLUB')}
          />
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
          <Gap size="s" />

          <FixedSpaceColumn spacing="xs">
            <Label>{t.map.providerType}</Label>

            <FixedSpaceRow spacing="s">
              {(['MUNICIPAL', 'PURCHASED'] as ProviderTypeOption[]).map(
                (type) => (
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
                )
              )}
            </FixedSpaceRow>

            <FixedSpaceRow spacing="s">
              {([
                'PRIVATE',
                'PRIVATE_SERVICE_VOUCHER'
              ] as ProviderTypeOption[]).map((type) => (
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
            </FixedSpaceRow>
          </FixedSpaceColumn>

          <Gap size="xs" />

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

      <Gap size="s" />
      <Centered>
        <InlineButton
          onClick={() => setShowMoreFilters(!showMoreFilters)}
          text={showMoreFilters ? t.map.showLessFilters : t.map.showMoreFilters}
          icon={showMoreFilters ? faAngleUp : faAngleDown}
        />
      </Centered>

      <div className="mobile-tabs">
        <HorizontalLine />
        <MobileTabs>
          <InlineButton
            onClick={() => setMobileMode('map')}
            text={t.map.mobileTabs.map}
            icon={fasMapMarkerAlt}
            className={mobileMode !== 'map' ? 'inactive' : undefined}
          />
          <InlineButton
            onClick={() => setMobileMode('list')}
            text={t.map.mobileTabs.list}
            icon={faList}
            className={mobileMode !== 'list' ? 'inactive' : undefined}
          />
        </MobileTabs>
      </div>
    </Wrapper>
  )
})

const Wrapper = styled(ContentArea)`
  box-sizing: border-box;
  width: 100%;
  padding-right: 20px;

  .mobile-tabs {
    display: none;
  }
  @media (max-width: ${mapViewBreakpoint}) {
    .mobile-tabs {
      display: block !important;
    }
  }
`

const MobileTabs = styled.div`
  display: flex;
  justify-content: space-evenly;

  .inactive {
    color: ${colors.greyscale.medium} !important;
  }
`

const Centered = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
`
