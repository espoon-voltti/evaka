// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction, memo } from 'react'
import MemoizedCard from './UnitCard'
import styled from 'styled-components'
import { Unit } from '../../state/placementdraft'
import {
  DaycarePlacementPlan,
  PlacementDraft
} from '../../types/placementdraft'
import LocalDate from 'lib-common/local-date'

const FlexContainer = styled.div`
  display: flex;
  overflow-x: scroll;
  flex-wrap: wrap;
`

interface Props {
  additionalUnits: Unit[]
  setAdditionalUnits: Dispatch<SetStateAction<Unit[]>>
  placement: DaycarePlacementPlan
  setPlacement: Dispatch<SetStateAction<DaycarePlacementPlan>>
  placementDraft: PlacementDraft
  selectedUnitIsGhostUnit: boolean
}

const MemoizedCards = memo(function UnitCards({
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
          <MemoizedCard
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

export default MemoizedCards
