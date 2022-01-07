// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useTranslation } from 'employee-frontend/state/i18n'
import { UUID } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import PlacementProposals from '../../components/unit/tab-placement-proposals/PlacementProposals'
import { UnitContext } from '../../state/unit'
import { NotificationCounter } from '../UnitPage'
import { renderResult } from '../async-rendering'

interface Props {
  reloadUnitData: () => void
}

export default React.memo(function TabPlacementProposals({
  reloadUnitData
}: Props) {
  const { id } = useParams<{ id: UUID }>()
  const { unitData } = useContext(UnitContext)
  const { i18n } = useTranslation()
  const [open, setOpen] = useState<boolean>(true)

  return renderResult(unitData, (unitData) => (
    <CollapsibleContentArea
      opaque
      title={
        <Title size={2}>
          {i18n.unit.placementProposals.title}
          {unitData?.placementProposals?.length ?? 0 > 0 ? (
            <NotificationCounter>
              {unitData?.placementProposals?.length}
            </NotificationCounter>
          ) : null}
        </Title>
      }
      open={open}
      toggleOpen={() => setOpen(!open)}
    >
      <PlacementProposals
        unitId={id}
        placementPlans={unitData.placementProposals ?? []}
        loadUnitData={reloadUnitData}
      />
    </CollapsibleContentArea>
  ))
})
