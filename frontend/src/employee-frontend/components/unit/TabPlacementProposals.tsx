// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { ContentArea } from 'lib-components/layout/Container'
import { UnitContext } from '../../state/unit'
import { useParams } from 'react-router-dom'
import PlacementProposals from '../../components/unit/tab-placement-proposals/PlacementProposals'
import { UUID } from 'lib-common/types'
import { renderResult } from '../async-rendering'

interface Props {
  reloadUnitData: () => void
}

function TabPlacementProposals({ reloadUnitData }: Props) {
  const { id } = useParams<{ id: UUID }>()
  const { unitData } = useContext(UnitContext)

  return renderResult(unitData, (unitData) => (
    <ContentArea opaque>
      <PlacementProposals
        unitId={id}
        placementPlans={unitData.placementProposals ?? []}
        loadUnitData={reloadUnitData}
      />
    </ContentArea>
  ))
}

export default React.memo(TabPlacementProposals)
