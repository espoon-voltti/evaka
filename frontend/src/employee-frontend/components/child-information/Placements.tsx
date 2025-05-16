// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { Fragment, useContext } from 'react'

import { combine } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import type { ChildId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import { H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import CreatePlacementModal from '../../components/child-information/placements/CreatePlacementModal'
import PlacementRow from '../../components/child-information/placements/PlacementRow'
import { serviceNeedOptionsQuery } from '../../queries'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'

import { placementsQuery } from './queries'

interface Props {
  childId: ChildId
}

export default React.memo(function Placements({ childId }: Props) {
  const { i18n } = useTranslation()
  const { user } = useContext(UserContext)
  const { uiMode, toggleUiMode } = useContext(UIContext)

  const placements = useQueryResult(placementsQuery({ childId }))
  const serviceNeedOptions = useQueryResult(serviceNeedOptionsQuery())

  return (
    <div>
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
      {uiMode === 'create-new-placement' && (
        <CreatePlacementModal childId={childId} />
      )}
    </div>
  )
})
