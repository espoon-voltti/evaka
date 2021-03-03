import React from 'react'
import styled from 'styled-components'
import { Result } from '@evaka/lib-common/src/api'
import { PublicUnit } from '@evaka/lib-common/src/api-types/units/PublicUnit'
import { UnitLanguage } from '@evaka/lib-common/src/api-types/units/enums'
import { Gap } from '@evaka/lib-components/src/white-space'
import SearchSection from '../map/SearchSection'
import UnitList from '../map/UnitList'
import { mapViewBreakpoint, MobileMode } from '../map/const'
import { CareTypeOption, MapAddress, ProviderTypeOption } from '../map/MapView'
import { UnitWithDistance } from '../map/distances'

type Props = {
  allUnits: Result<PublicUnit[]>
  filteredUnits: Result<PublicUnit[]>
  unitsWithDistances: Result<UnitWithDistance[]>
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

export default React.memo(function UnitSearchPanel({
  allUnits,
  filteredUnits,
  unitsWithDistances,
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
  return (
    <Wrapper>
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
        mobileMode={mobileMode}
        setMobileMode={setMobileMode}
        selectedAddress={selectedAddress}
        setSelectedAddress={setSelectedAddress}
        setSelectedUnit={setSelectedUnit}
      />
      <Gap size="xs" />
      <UnitList
        selectedAddress={selectedAddress}
        filteredUnits={filteredUnits}
        unitsWithDistances={unitsWithDistances}
        setSelectedUnit={setSelectedUnit}
      />
    </Wrapper>
  )
})

const Wrapper = styled.div`
  width: 400px;
  min-width: 300px;
  flex-grow: 1;
  flex-shrink: 1;

  display: flex;
  flex-direction: column;

  .mobile-tabs {
    display: none;
  }

  @media (max-width: ${mapViewBreakpoint}) {
    width: 100%;
  }
`
