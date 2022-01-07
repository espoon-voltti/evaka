// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction } from 'react'
import styled from 'styled-components'
import { PublicUnit } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import {
  DaycarePlacementPlan,
  PlacementDraft
} from '../../types/placementdraft'
import UnitCard from './UnitCard'

const FlexContainer = styled.div`
  display: flex;
  overflow-x: scroll;
  flex-wrap: wrap;
`

interface Props {
  additionalUnits: PublicUnit[]
  setAdditionalUnits: Dispatch<SetStateAction<PublicUnit[]>>
  placement: DaycarePlacementPlan
  setPlacement: Dispatch<SetStateAction<DaycarePlacementPlan>>
  placementDraft: PlacementDraft
  selectedUnitIsGhostUnit: boolean
}

export default React.memo(function UnitCards({
  additionalUnits,
  setAdditionalUnits,
  placement,
  setPlacement,
  placementDraft,
  selectedUnitIsGhostUnit
}: Props) {
  if (!placement.period) return null

  const startDate = placement.period
    ? placement.period.start
    : LocalDate.today()
  const endDate = placement.period ? placement.period.end : LocalDate.today()

  return (
    <FlexContainer data-qa="placement-list">
      {placementDraft.preferredUnits.concat(additionalUnits).map((unit) => {
        const isSelectedUnit = placement.unitId === unit.id
        return (
          <UnitCard
            unitId={unit.id}
            unitName={unit.name}
            key={unit.id}
            startDate={startDate}
            endDate={endDate}
            additionalUnits={additionalUnits}
            setAdditionalUnits={setAdditionalUnits}
            setPlacement={setPlacement}
            isSelectedUnit={isSelectedUnit}
            displayGhostUnitWarning={isSelectedUnit && selectedUnitIsGhostUnit}
          />
        )
      })}
    </FlexContainer>
  )
})
