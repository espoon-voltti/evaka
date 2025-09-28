// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Dispatch, SetStateAction } from 'react'
import React from 'react'
import styled from 'styled-components'

import type { PublicUnit } from 'lib-common/generated/api-types/daycare'
import type { PlacementPlanDraft } from 'lib-common/generated/api-types/placement'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import { defaultMargins } from 'lib-components/white-space'

import type { DaycarePlacementPlanForm } from './PlacementPlanDraft'
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
  formState: DaycarePlacementPlanForm
  setFormState: Dispatch<SetStateAction<DaycarePlacementPlanForm>>
  placementPlanDraft: PlacementPlanDraft
  selectedUnitIsGhostUnit: boolean
}

export default React.memo(function UnitCards({
  additionalUnits,
  setAdditionalUnits,
  applicationId,
  formState,
  setFormState,
  placementPlanDraft,
  selectedUnitIsGhostUnit
}: Props) {
  const { unitId, period, hasPreschoolDaycarePeriod } = formState

  // null => preschoolDaycarePeriod is invalid or empty
  // undefined => not a preschool application
  const preschoolDaycarePeriod = hasPreschoolDaycarePeriod
    ? formState.preschoolDaycarePeriod
    : undefined

  if (period === null || preschoolDaycarePeriod === null) {
    return null
  }

  return (
    <FlexContainer data-qa="placement-list">
      {placementPlanDraft.preferredUnits.concat(additionalUnits).map((unit) => {
        const isSelectedUnit = unitId === unit.id
        return (
          <UnitCard
            applicationId={applicationId}
            unitId={unit.id}
            unitName={unit.name}
            key={unit.id}
            period={period}
            preschoolDaycarePeriod={preschoolDaycarePeriod}
            additionalUnits={additionalUnits}
            setAdditionalUnits={setAdditionalUnits}
            setPlacement={setFormState}
            isSelectedUnit={isSelectedUnit}
            displayGhostUnitWarning={isSelectedUnit && selectedUnitIsGhostUnit}
          />
        )
      })}
    </FlexContainer>
  )
})
