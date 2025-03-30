// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Dispatch, SetStateAction } from 'react'
import styled from 'styled-components'

import { PublicUnit } from 'lib-common/generated/api-types/daycare'
import { PlacementPlanDraft } from 'lib-common/generated/api-types/placement'
import { ApplicationId } from 'lib-common/generated/api-types/shared'
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
  applicationId: ApplicationId
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
      {[
        ...(placementDraft.trialPlacementUnit &&
        !placementDraft.preferredUnits.some(
          (u) => u.id === placementDraft.trialPlacementUnit?.id
        )
          ? [placementDraft.trialPlacementUnit]
          : []),
        ...placementDraft.preferredUnits,
        ...additionalUnits
      ].map((unit) => {
        const isSelectedUnit = unitId === unit.id
        const preferenceIndex = placementDraft.preferredUnits.findIndex(
          (u) => u.id === unit.id
        )
        return (
          <UnitCard
            applicationId={applicationId}
            unitId={unit.id}
            unitName={unit.name}
            preferenceNumber={preferenceIndex < 0 ? null : preferenceIndex + 1}
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
