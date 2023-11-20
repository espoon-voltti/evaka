// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { ReactNode, useMemo, useState } from 'react'
import styled from 'styled-components'

import { ApplicationType } from 'lib-common/generated/api-types/application'
import {
  Language,
  ProviderType,
  PublicUnit
} from 'lib-common/generated/api-types/daycare'
import { Coordinate } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
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
import { mapViewBreakpoint, MobileMode } from './const'
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
    <FullScreen data-qa="map-view" loggedIn={loggedIn}>
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

  const allUnits = useQueryResult(unitsQuery(careType))

  const filteredUnits = useMemo(
    () =>
      allUnits.map((units) =>
        filterAndSortUnits(units, careType, languages, providerTypes, shiftCare)
      ),
    [allUnits, careType, languages, providerTypes, shiftCare]
  )

  const unitsWithDistances = useQueryResult(
    unitsWithDistancesQuery(
      selectedAddress,
      filteredUnits.isSuccess ? filteredUnits.value : []
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
        breakpoint={mapViewBreakpoint}
        horizontalSpacing="zero"
        verticalSpacing="zero"
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
            <Gap size="xs" />

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

const filterAndSortUnits = (
  units: PublicUnit[],
  careType: CareTypeOption,
  languages: Language[],
  providerTypes: ProviderType[],
  shiftCare: boolean
): PublicUnit[] => {
  const filteredUnits = units
    .filter((u) =>
      careType === 'DAYCARE'
        ? u.type.includes('CENTRE') ||
          u.type.includes('FAMILY') ||
          u.type.includes('GROUP_FAMILY')
        : careType === 'CLUB'
          ? u.type.includes('CLUB')
          : careType === 'PRESCHOOL'
            ? u.type.includes('PRESCHOOL') ||
              u.type.includes('PREPARATORY_EDUCATION')
            : false
    )
    .filter(
      (u) =>
        languages.length == 0 ||
        (!(u.language === 'fi' && !languages.includes('fi')) &&
          !(u.language === 'sv' && !languages.includes('sv')))
    )
    .filter(
      (u) =>
        providerTypes.length == 0 ||
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
    .filter((u) => !shiftCare || u.roundTheClock)

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

const FullScreen = styled.div<{ loggedIn: boolean }>`
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
