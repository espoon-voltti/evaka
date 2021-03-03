{
  /*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React, { useContext } from 'react'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import { UnitContext } from '../../state/unit'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'
import ErrorSegment from '@evaka/lib-components/src/atoms/state/ErrorSegment'
import { useParams } from 'react-router-dom'
import { UUID } from '../../types'
import PlacementProposals from '../../components/unit/tab-placement-proposals/PlacementProposals'

interface Props {
  reloadUnitData: () => void
}

function TabPlacementProposals({ reloadUnitData }: Props) {
  const { id } = useParams<{ id: UUID }>()
  const { unitData } = useContext(UnitContext)

  if (unitData.isFailure) {
    return <ErrorSegment />
  }

  if (unitData.isLoading) {
    return <SpinnerSegment />
  }

  return (
    <ContentArea opaque>
      <PlacementProposals
        unitId={id}
        placementPlans={unitData.value.placementProposals ?? []}
        loadUnitData={reloadUnitData}
      />
    </ContentArea>
  )
}

export default React.memo(TabPlacementProposals)
