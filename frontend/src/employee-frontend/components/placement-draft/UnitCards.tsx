// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction } from 'react'
import styled from 'styled-components'

import { PublicUnit } from 'lib-common/generated/api-types/daycare'
import { PlacementPlanDraft } from 'lib-common/generated/api-types/placement'
import { UUID } from 'lib-common/types'
import { defaultMargins } from 'lib-components/white-space'

import { DaycarePlacementPlanForm } from './PlacementDraft'
import UnitCard from './UnitCard'

const FlexContainer = styled.div`
  display: flex;
  gap: ${defaultMargins.L};
  flex-wrap: wrap;
`

interface Props {
  additionalUnits: PublicUnit[]
  setAdditionalUnits: Dispatch<SetStateAction<PublicUnit[]>>
  applicationId: UUID
  placement: DaycarePlacementPlanForm
  setPlacement: Dispatch<SetStateAction<DaycarePlacementPlanForm>>
  placementDraft: PlacementPlanDraft
  selectedUnitIsGhostUnit: boolean
}

export default React.memo(function UnitCards({
  additionalUnits,
  setAdditionalUnits,
  applicationId,
  placement: { period, preschoolDaycarePeriod, unitId },
  setPlacement,
  placementDraft,
  selectedUnitIsGhostUnit
}: Props) {
  if (!period) {
    return null
  }

  return (
    <FlexContainer data-qa="placement-list">
      {placementDraft.preferredUnits.concat(additionalUnits).map((unit) => {
        const isSelectedUnit = unitId === unit.id
        return (
          <UnitCard
            applicationId={applicationId}
            unitId={unit.id}
            unitName={unit.name}
            key={unit.id}
            period={period}
            preschoolDaycarePeriod={preschoolDaycarePeriod ?? undefined}
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
