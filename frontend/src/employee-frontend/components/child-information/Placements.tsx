// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import _ from 'lodash'
import React, { Fragment, useContext, useState } from 'react'

import { combine } from 'lib-common/api'
import { DaycarePlacementWithDetails } from 'lib-common/generated/api-types/placement'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2, H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { getServiceNeedOptions } from '../../api/child/service-needs'
import CreatePlacementModal from '../../components/child-information/placements/CreatePlacementModal'
import PlacementRow from '../../components/child-information/placements/PlacementRow'
import { ChildContext, ChildState } from '../../state/child'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { DateRange, rangesOverlap } from '../../utils/date'
import { RequireRole } from '../../utils/roles'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'

interface Props {
  id: UUID
  startOpen: boolean
}

export default React.memo(function Placements({ id, startOpen }: Props) {
  const { i18n } = useTranslation()
  const { placements, loadPlacements } = useContext<ChildState>(ChildContext)
  const [serviceNeedOptions] = useApiState(getServiceNeedOptions, [])
  const { uiMode, toggleUiMode } = useContext(UIContext)

  const [open, setOpen] = useState(startOpen)

  const checkOverlaps = (
    range: DateRange,
    placement: DaycarePlacementWithDetails
  ): boolean =>
    placements
      .map(
        (ps) =>
          ps
            .filter((p) => p.id !== placement.id)
            .filter((p) => rangesOverlap(range, p)).length > 0
      )
      .getOrElse(false)

  return (
    <div>
      <CollapsibleContentArea
        title={<H2 noMargin>{i18n.childInformation.placements.title}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="child-placements-collapsible"
      >
        <Gap size="m" />
        <FlexRow justifyContent="space-between">
          <H3 noMargin>{i18n.childInformation.placements.placements}</H3>
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
              data-qa="create-new-placement-button"
            />
          </RequireRole>
        </FlexRow>
        {renderResult(
          combine(serviceNeedOptions, placements),
          ([serviceNeedOptions, placements]) => (
            <div>
              {_.orderBy(placements, ['startDate'], ['desc']).map((p, i) => (
                <Fragment key={p.id}>
                  <PlacementRow
                    placement={p}
                    onRefreshNeeded={loadPlacements}
                    checkOverlaps={checkOverlaps}
                    serviceNeedOptions={serviceNeedOptions}
                  />
                  {i < placements.length - 1 && (
                    <div className="separator large" />
                  )}
                </Fragment>
              ))}
            </div>
          )
        )}
      </CollapsibleContentArea>
      {uiMode === 'create-new-placement' && (
        <CreatePlacementModal childId={id} reload={loadPlacements} />
      )}
    </div>
  )
})
