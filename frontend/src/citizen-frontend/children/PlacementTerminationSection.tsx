// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { P } from 'lib-components/typography'
import React from 'react'
import { renderResult } from '../async-rendering'
import { useTranslation } from '../localization'
import { getPlacements } from './api'
import PlacementTerminationForm from './PlacementTerminationForm'
import TerminatedPlacements from './TerminatedPlacements'

interface PlacementTerminationProps {
  childId: UUID
}

export default React.memo(function PlacementTerminationSection({
  childId
}: PlacementTerminationProps) {
  const t = useTranslation()
  const [placementsResponse, refreshPlacements] = useApiState(
    () => getPlacements(childId),
    [childId]
  )

  return (
    <CollapsibleSection
      title={t.children.placementTermination.title}
      startCollapsed
      fitted
    >
      {renderResult(placementsResponse, ({ placements }) => {
        const terminatedPlacements = placements.filter((p) =>
          p.placements
            .concat(p.additionalPlacements)
            .find((p2) => !!p2.terminationRequestedDate)
        )
        return (
          <FixedSpaceColumn>
            <P>{t.children.placementTermination.description}</P>
            {placements.map((grp) => (
              <PlacementTerminationForm
                key={`${grp.type}-${grp.unitId}`}
                childId={childId}
                placementGroup={grp}
                onSuccess={refreshPlacements}
              />
            ))}
            {terminatedPlacements.length > 0 && (
              <TerminatedPlacements placements={terminatedPlacements} />
            )}
          </FixedSpaceColumn>
        )
      })}
    </CollapsibleSection>
  )
})
