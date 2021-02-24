import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import _ from 'lodash'
import { Failure, Loading, Result, Success } from '@evaka/lib-common/src/api'
import {
  ProviderType,
  UnitLanguage
} from '@evaka/lib-common/src/api-types/units/enums'
import { PublicUnit } from '@evaka/lib-common/src/api-types/units/PublicUnit'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import { Coordinate } from '@evaka/lib-common/src/api-types/units/Coordinate'
import AdaptiveFlex from '@evaka/lib-components/src/layout/AdaptiveFlex'
import { useTranslation } from '~localization'
import useTitle from '~useTitle'
import { headerHeight } from '~header/const'
import UnitSearchPanel from '~map/UnitSearchPanel'
import MapBox from '~map/MapBox'
import { mapViewBreakpoint, MobileMode } from '~map/const'
import {
  calcStraightDistance,
  UnitWithDistance,
  UnitWithStraightDistance
} from '~map/distances'
import { fetchUnits, queryDistances } from '~map/api'
import UnitDetailsPanel from '~map/UnitDetailsPanel'
import { ApplicationType } from '@evaka/lib-common/src/api-types/application/enums'

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

export default React.memo(function MapView() {
  const t = useTranslation()
  const [mobileMode, setMobileMode] = useState<MobileMode>('map')

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
  useEffect(() => loadUnits(careType), [careType])

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
  }, [selectedAddress, filteredUnits])

  useTitle(t, t.map.title)

  return (
    <FullScreen data-qa="map-view">
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
          />
        </MapContainer>
      </FlexContainer>
    </FullScreen>
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

const FullScreen = styled.div`
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  align-items: stretch;
`

const FlexContainer = styled(AdaptiveFlex)`
  margin-top: 64px;
  align-items: stretch;

  width: 100%;

  @media (max-width: ${mapViewBreakpoint}) {
    margin-top: ${headerHeight};
    width: 100%;
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
