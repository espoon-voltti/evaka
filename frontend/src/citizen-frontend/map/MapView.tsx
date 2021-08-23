// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { ReactNode, useEffect, useState } from 'react'
import styled from 'styled-components'
import _ from 'lodash'
import { Failure, Loading, Result, Success } from 'lib-common/api'
import { UnitLanguage } from 'lib-common/api-types/units/enums'
import { PublicUnit } from 'lib-common/api-types/units/PublicUnit'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { Coordinate } from 'lib-common/api-types/units/Coordinate'
import { defaultMargins } from 'lib-components/white-space'
import AdaptiveFlex from 'lib-components/layout/AdaptiveFlex'
import { useTranslation } from '../localization'
import useTitle from '../useTitle'
import { headerHeightDesktop } from '../header/const'
import UnitSearchPanel from '../map/UnitSearchPanel'
import MapBox from '../map/MapBox'
import { mapViewBreakpoint, MobileMode } from '../map/const'
import {
  calcStraightDistance,
  UnitWithDistance,
  UnitWithStraightDistance
} from '../map/distances'
import { fetchUnits, queryDistances } from '../map/api'
import UnitDetailsPanel from '../map/UnitDetailsPanel'
import { ApplicationType, ProviderType } from 'lib-common/generated/enums'
import { useUser } from 'citizen-frontend/auth'

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

export type ProviderTypeOption = Exclude<ProviderType, 'MUNICIPAL_SCHOOL'>

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
  const loggedIn = !!useUser()

  const [selectedUnit, setSelectedUnit] = useState<PublicUnit | null>(null)

  const [selectedAddress, setSelectedAddress] = useState<MapAddress | null>(
    null
  )
  const [careType, setCareType] = useState<CareTypeOption>('DAYCARE')
  const [languages, setLanguages] = useState<UnitLanguage[]>([])
  const [providerTypes, setProviderTypes] = useState<ProviderTypeOption[]>([])
  const [shiftCare, setShiftCare] = useState<boolean>(false)

  const [unitsResult, setUnitsResult] = useState<Result<PublicUnit[]>>(
    Loading.of()
  )
  const loadUnits = useRestApi(fetchUnits, setUnitsResult)
  useEffect(() => loadUnits(careType), [careType, loadUnits])

  const [filteredUnits, setFilteredUnits] = useState<Result<PublicUnit[]>>(
    filterUnits(unitsResult, careType, languages, providerTypes, shiftCare)
  )
  useEffect(() => {
    setFilteredUnits(
      filterUnits(unitsResult, careType, languages, providerTypes, shiftCare)
    )
  }, [unitsResult, careType, languages, providerTypes, shiftCare])

  const [unitsWithDistances, setUnitsWithDistances] = useState<
    Result<UnitWithDistance[]>
  >(Success.of([]))
  const loadDistance = useRestApi(queryDistances, setUnitsWithDistances)
  useEffect(() => {
    if (selectedAddress && filteredUnits.isSuccess) {
      const units = filteredUnits.value

      const unitsWithStraightDistance = units.map<UnitWithStraightDistance>(
        (unit) => ({
          ...unit,
          straightDistance: unit.location
            ? calcStraightDistance(unit.location, selectedAddress.coordinates)
            : null
        })
      )

      loadDistance(selectedAddress.coordinates, unitsWithStraightDistance)
    } else {
      setUnitsWithDistances(Success.of([]))
    }
  }, [loadDistance, selectedAddress, filteredUnits])

  useTitle(t, t.map.title)

  return (
    <MapFullscreenContainer loggedIn={loggedIn}>
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
          <UnitSearchPanel
            allUnits={unitsResult}
            filteredUnits={filteredUnits}
            unitsWithDistances={unitsWithDistances}
            careType={careType}
            setCareType={setCareType}
            languages={languages}
            setLanguages={setLanguages}
            providerTypes={providerTypes}
            setProviderTypes={setProviderTypes}
            shiftCare={shiftCare}
            setShiftCare={setShiftCare}
            mobileMode={mobileMode}
            setMobileMode={setMobileMode}
            selectedAddress={selectedAddress}
            setSelectedAddress={setSelectedAddress}
            setSelectedUnit={setSelectedUnit}
          />
        )}
        <MapContainer>
          <MapBox
            units={
              selectedAddress
                ? unitsWithDistances.getOrElse([])
                : filteredUnits.getOrElse([])
            }
            selectedUnit={selectedUnit}
            selectedAddress={selectedAddress}
            setSelectedUnit={setSelectedUnit}
          />
        </MapContainer>
      </FlexContainer>
    </MapFullscreenContainer>
  )
})

const filterUnits = (
  unitsResult: Result<PublicUnit[]>,
  careType: CareTypeOption,
  languages: UnitLanguage[],
  providerTypes: ProviderTypeOption[],
  shiftCare: boolean
): Result<PublicUnit[]> => {
  if (unitsResult.isLoading) return Loading.of()
  if (unitsResult.isFailure)
    return Failure.of({
      message: unitsResult.message,
      statusCode: unitsResult.statusCode
    })

  const filteredUnits = unitsResult.value
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
            u.providerType === 'EXTERNAL_PURCHASED' &&
            !providerTypes.includes('EXTERNAL_PURCHASED')
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

  const sortedUnits = _.sortBy(filteredUnits, (u) => u.name)
  return Success.of(sortedUnits)
}

const FullScreen = styled.div<{ loggedIn: boolean }>`
  position: absolute;
  top: calc(
    ${headerHeightDesktop}px + ${({ loggedIn }) => (loggedIn ? '76px' : '0px')}
  );
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
