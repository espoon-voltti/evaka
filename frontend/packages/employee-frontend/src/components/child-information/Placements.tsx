// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect } from 'react'
import { UUID } from '~types'
import { useTranslation } from '~state/i18n'
import { ChildContext } from '~state'
import { ChildState } from '~state/child'
import { isFailure, isLoading, isSuccess, Loading } from '~api'
import { Loader } from '~components/shared/alpha'
import { faMapMarkerAlt } from 'icon-set'
import PlacementRow from '~components/child-information/placements/PlacementRow'
import { UIContext } from '~state/ui'
import CreatePlacementModal from '~components/child-information/placements/CreatePlacementModal'
import { AddButtonRow } from 'components/shared/atoms/buttons/AddButton'
import { getPlacements } from 'api/child/placements'
import CollapsibleSection from 'components/shared/molecules/CollapsibleSection'
import { RequireRole } from 'utils/roles'
import { DateRange, rangesOverlap } from '~utils/date'
import { Placement } from '~types/child'

interface Props {
  id: UUID
  open: boolean
}

const Placements = React.memo(function Placements({ id, open }: Props) {
  const { i18n } = useTranslation()
  const { placements, setPlacements } = useContext<ChildState>(ChildContext)
  const { uiMode, toggleUiMode } = useContext(UIContext)

  function loadPlacements() {
    setPlacements(Loading())
    void getPlacements(id).then((placements) => setPlacements(placements))
  }

  useEffect(loadPlacements, [id, setPlacements])

  const checkOverlaps = (range: DateRange, placement: Placement): boolean =>
    isSuccess(placements)
      ? !!placements.data
          .filter((p) => p.id !== placement.id)
          .filter((p) => rangesOverlap(range, p)).length
      : false

  function renderContents() {
    if (isLoading(placements)) {
      return <Loader />
    } else if (isFailure(placements)) {
      return <div>{i18n.common.loadingFailed}</div>
    }
    return (
      <div>
        {placements.data
          .sort((p1, p2) =>
            p1.startDate.isEqual(p2.startDate)
              ? 0
              : p1.startDate.isBefore(p2.startDate)
              ? 1
              : -1
          )
          .map((p) => (
            <PlacementRow
              key={p.id}
              placement={p}
              onRefreshNeeded={loadPlacements}
              checkOverlaps={checkOverlaps}
            />
          ))}
      </div>
    )
  }

  return (
    <div className="placements-section">
      <CollapsibleSection
        icon={faMapMarkerAlt}
        title={i18n.childInformation.placements.title}
        startCollapsed={!open}
        dataQa="child-placements-collapsible"
      >
        <RequireRole
          oneOf={[
            'SERVICE_WORKER',
            'UNIT_SUPERVISOR',
            'FINANCE_ADMIN',
            'ADMIN'
          ]}
        >
          <AddButtonRow
            text={i18n.childInformation.placements.createPlacement.btn}
            onClick={() => toggleUiMode('create-new-placement')}
            disabled={uiMode === 'create-new-placement'}
          />
        </RequireRole>
        {renderContents()}
      </CollapsibleSection>
      {uiMode === 'create-new-placement' && (
        <CreatePlacementModal childId={id} reload={loadPlacements} />
      )}
    </div>
  )
})

export default Placements
