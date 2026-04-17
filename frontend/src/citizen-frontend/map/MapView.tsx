// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import type { ReactNode } from 'react'
import React, { useMemo, useState } from 'react'
import styled from 'styled-components'

import type { ApplicationType } from 'lib-common/generated/api-types/application'
import type {
  Language,
  ProviderType,
  PublicUnit
} from 'lib-common/generated/api-types/daycare'
import type { Coordinate } from 'lib-common/generated/api-types/shared'
import {
  constantQuery,
  useChainedQuery,
  useQueryResult
} from 'lib-common/query'
import AdaptiveFlex from 'lib-components/layout/AdaptiveFlex'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { useUser } from '../auth/state'
import { useTranslation } from '../localization'
import { headerHeightDesktop } from '../navigation/const'
import useTitle from '../useTitle'

import MapBox from './MapBox'
import MobileTabs from './MobileTabs'
import SearchSection from './SearchSection'
import UnitDetailsPanel from './UnitDetailsPanel'
import UnitList from './UnitList'
import type { MobileMode } from './const'
import { mapViewBreakpoint } from './const'
import { unitsQuery, unitsWithDistancesQuery } from './queries'

export type MapAddress = {
  coordinates: Coordinate
  streetAddress: string
  postalCode: string
  postOffice: string
  unit?: {
    id: string
    name: string
  }
}

export type CareTypeOption = ApplicationType

interface MapContainerProps {
  loggedIn: boolean
  children: ReactNode
}

const MapFullscreenContainer = React.memo(function MapFullscreenContainer({
  loggedIn,
  children
}: MapContainerProps) {
  return (
    <FullScreen data-qa="map-view" $loggedIn={loggedIn}>
      {children}
    </FullScreen>
  )
})

export default React.memo(function MapView() {
  const t = useTranslation()
  const [mobileMode, setMobileMode] = useState<MobileMode>('map')
  const user = useUser()

  const [selectedUnit, setSelectedUnit] = useState<PublicUnit | null>(null)

  const [selectedAddress, setSelectedAddress] = useState<MapAddress | null>(
    null
  )
  const [careType, setCareType] = useState<CareTypeOption>('DAYCARE')
  const [languages, setLanguages] = useState<Language[]>([])
  const [providerTypes, setProviderTypes] = useState<ProviderType[]>([])
  const [shiftCare, setShiftCare] = useState<boolean>(false)

  const allUnits = useQueryResult(unitsQuery({ applicationType: careType }))

  const careTypeFilteredUnits = useMemo(
    () =>
      allUnits.map((units) =>
        units.filter((u) => matchesCareType(u, careType))
      ),
    [allUnits, careType]
  )

  const availableLanguages = useMemo(
    () => new Set(careTypeFilteredUnits.getOrElse([]).map((u) => u.language)),
    [careTypeFilteredUnits]
  )

  // Any selected language chip that's no longer available (e.g., after a
  // careType switch) is ignored so the user never ends up filtering against
  // a hidden chip with no way to clear it.
  const effectiveLanguages = useMemo(
    () => languages.filter((l) => availableLanguages.has(l)),
    [languages, availableLanguages]
  )

  const filteredUnits = useMemo(
    () =>
      careTypeFilteredUnits.map((units) =>
        filterAndSortUnits(units, effectiveLanguages, providerTypes, shiftCare)
      ),
    [careTypeFilteredUnits, effectiveLanguages, providerTypes, shiftCare]
  )

  const unitsWithDistances = useChainedQuery(
    filteredUnits.map((filteredUnits) =>
      selectedAddress && filteredUnits.length > 0
        ? unitsWithDistancesQuery(selectedAddress, filteredUnits)
        : constantQuery([])
    )
  )

  useTitle(t, t.map.title)

  const units = useMemo(
    () =>
      selectedAddress
        ? unitsWithDistances.getOrElse([])
        : filteredUnits.getOrElse([]),
    [filteredUnits, selectedAddress, unitsWithDistances]
  )

  const navigateBack = user ? '/applications' : '/login'

  return (
    <MapFullscreenContainer loggedIn={!!user}>
      <FlexContainer
        className={`mobile-mode-${mobileMode}`}
        $breakpoint={mapViewBreakpoint}
        $horizontalSpacing="zero"
        $verticalSpacing="zero"
      >
        {selectedUnit ? (
          <UnitDetailsPanel
            unit={selectedUnit}
            onClose={() => setSelectedUnit(null)}
            selectedAddress={selectedAddress}
          />
        ) : (
          <PanelWrapper>
            <SearchSection
              allUnits={allUnits}
              availableLanguages={availableLanguages}
              careType={careType}
              setCareType={setCareType}
              languages={languages}
              setLanguages={setLanguages}
              providerTypes={providerTypes}
              setProviderTypes={setProviderTypes}
              shiftCare={shiftCare}
              setShiftCare={setShiftCare}
              selectedAddress={selectedAddress}
              setSelectedAddress={setSelectedAddress}
              setSelectedUnit={setSelectedUnit}
              navigateBack={navigateBack}
            />
            <Gap $size="xs" />

            <MobileTabs mobileMode={mobileMode} setMobileMode={setMobileMode} />

            <UnitList
              selectedAddress={selectedAddress}
              filteredUnits={filteredUnits}
              unitsWithDistances={unitsWithDistances}
              setSelectedUnit={setSelectedUnit}
            />
          </PanelWrapper>
        )}
        <MapContainer>
          <MapBox
            units={units}
            selectedUnit={selectedUnit}
            selectedAddress={selectedAddress}
            setSelectedUnit={setSelectedUnit}
          />
        </MapContainer>
      </FlexContainer>
    </MapFullscreenContainer>
  )
})

const matchesCareType = (
  unit: PublicUnit,
  careType: CareTypeOption
): boolean => {
  switch (careType) {
    case 'DAYCARE':
      return (
        unit.type.includes('CENTRE') ||
        unit.type.includes('FAMILY') ||
        unit.type.includes('GROUP_FAMILY')
      )
    case 'CLUB':
      return unit.type.includes('CLUB')
    case 'PRESCHOOL':
      return (
        unit.type.includes('PRESCHOOL') ||
        unit.type.includes('PREPARATORY_EDUCATION')
      )
  }
}

const filterAndSortUnits = (
  units: PublicUnit[],
  languages: Language[],
  providerTypes: ProviderType[],
  shiftCare: boolean
): PublicUnit[] => {
  const filteredUnits = units
    .filter((u) => languages.length === 0 || languages.includes(u.language))
    .filter(
      (u) =>
        providerTypes.length === 0 ||
        (!(
          (u.providerType === 'MUNICIPAL' ||
            u.providerType === 'MUNICIPAL_SCHOOL') &&
          !providerTypes.includes('MUNICIPAL')
        ) &&
          !(
            u.providerType === 'PURCHASED' &&
            !providerTypes.includes('PURCHASED')
          ) &&
          !(
            u.providerType === 'PRIVATE' && !providerTypes.includes('PRIVATE')
          ) &&
          !(
            u.providerType === 'PRIVATE_SERVICE_VOUCHER' &&
            !providerTypes.includes('PRIVATE_SERVICE_VOUCHER')
          ))
    )
    .filter((u) => !shiftCare || u.providesShiftCare)

  return sortBy(filteredUnits, (u) => u.name)
}

const PanelWrapper = styled.div`
  width: 400px;
  min-width: 300px;
  flex-grow: 1;
  flex-shrink: 1;

  display: flex;
  flex-direction: column;

  overflow-y: auto;
  @media (max-width: ${mapViewBreakpoint}) {
    width: 100%;
    overflow-y: unset;
  }
`

const FullScreen = styled.div<{ $loggedIn: boolean }>`
  position: absolute;
  top: ${headerHeightDesktop}px;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  align-items: stretch;

  @media (max-width: ${mapViewBreakpoint}) {
    position: static;
  }
`

const FlexContainer = styled(AdaptiveFlex)`
  align-items: stretch;
  width: 100%;

  @media (max-width: ${mapViewBreakpoint}) {
    margin-top: ${defaultMargins.s};
    margin-bottom: 0;
    &.mobile-mode-map {
      .unit-list {
        display: none;
      }
    }

    &.mobile-mode-list {
      .map-box {
        display: none;
      }
    }
  }
`

const MapContainer = styled.div`
  min-width: 300px;
  flex-grow: 99;
`
