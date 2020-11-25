{/*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/}

import React, { useContext } from 'react'
import { ContentArea } from '~components/shared/layout/Container'
import { UnitContext } from '~state/unit'
import { isFailure, isLoading } from '~api'
import { SpinnerSegment } from '~components/shared/atoms/state/Spinner'
import ErrorSegment from '~components/shared/atoms/state/ErrorSegment'
import { useParams } from 'react-router-dom'
import { UUID } from '~types'
import PlacementProposals from '~components/unit/tab-placement-proposals/PlacementProposals'

interface Props {
  reloadUnitData: () => void
}

function TabPlacementProposals({ reloadUnitData }: Props) {
  const { id } = useParams<{ id: UUID }>()
  const { unitData } = useContext(UnitContext)

  if (isFailure(unitData)) {
    return <ErrorSegment />
  }

  if (isLoading(unitData)) {
    return <SpinnerSegment />
  }

  return (
    <ContentArea opaque>
      <PlacementProposals
        unitId={id}
        placementPlans={unitData.data.placementProposals ?? []}
        loadUnitData={reloadUnitData}
      />
    </ContentArea>
  )
}

export default React.memo(TabPlacementProposals)
