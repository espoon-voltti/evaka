// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { Fragment, useContext, useState } from 'react'

import { combine } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2, H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import CreatePlacementModal from '../../components/child-information/placements/CreatePlacementModal'
import PlacementRow from '../../components/child-information/placements/PlacementRow'
import { serviceNeedsQuery } from '../../queries'
import { ChildContext, ChildState } from '../../state/child'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'

interface Props {
  childId: UUID
  startOpen: boolean
}

export default React.memo(function Placements({ childId, startOpen }: Props) {
  const { i18n } = useTranslation()
  const { user } = useContext(UserContext)
  const {
    placements,
    loadPlacements,
    reloadPermittedActions,
    loadBackupCares
  } = useContext<ChildState>(ChildContext)
  const serviceNeedOptions = useQueryResult(serviceNeedsQuery())
  const { uiMode, toggleUiMode } = useContext(UIContext)

  const [open, setOpen] = useState(startOpen)

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
          {user?.accessibleFeatures.createPlacements ? (
            <AddButtonRow
              text={i18n.childInformation.placements.createPlacement.btn}
              onClick={() => toggleUiMode('create-new-placement')}
              disabled={uiMode === 'create-new-placement'}
              data-qa="create-new-placement-button"
            />
          ) : null}
        </FlexRow>
        {renderResult(
          combine(serviceNeedOptions, placements),
          ([serviceNeedOptions, placements]) => (
            <div>
              {orderBy(placements.placements, ['startDate'], ['desc']).map(
                (p, i) => (
                  <Fragment key={p.id}>
                    <PlacementRow
                      placement={p}
                      permittedActions={
                        placements.permittedPlacementActions[p.id] ?? []
                      }
                      permittedServiceNeedActions={
                        placements.permittedServiceNeedActions
                      }
                      onRefreshNeeded={() => {
                        loadPlacements()
                        void loadBackupCares()
                        reloadPermittedActions()
                      }}
                      otherPlacementRanges={placements.placements
                        .filter((p2) => p2.id !== p.id)
                        .map(
                          (p2) => new FiniteDateRange(p2.startDate, p2.endDate)
                        )}
                      serviceNeedOptions={serviceNeedOptions}
                    />
                    {i < placements.placements.length - 1 && (
                      <div className="separator large" />
                    )}
                  </Fragment>
                )
              )}
            </div>
          )
        )}
      </CollapsibleContentArea>
      {uiMode === 'create-new-placement' && (
        <CreatePlacementModal
          childId={childId}
          reload={() => {
            loadPlacements()
            reloadPermittedActions()
          }}
        />
      )}
    </div>
  )
})
