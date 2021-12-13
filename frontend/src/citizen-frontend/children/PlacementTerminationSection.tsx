// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { P } from 'lib-components/typography'
import { partition } from 'lodash'
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
      {renderResult(placementsResponse, (response) => {
        const { placements, terminationConstraints } = response
        const [terminatedPlacements, nonTerminatedPlacements] = partition(
          placements,
          (p) => !!p.terminationRequestedDate
        )
        return (
          <FixedSpaceColumn>
            <P>{t.children.placementTermination.description}</P>
            {nonTerminatedPlacements.length > 0 && (
              <PlacementTerminationForm
                placements={nonTerminatedPlacements}
                constraints={terminationConstraints}
                onSuccess={refreshPlacements}
              />
            )}
            {terminatedPlacements.length > 0 && (
              <TerminatedPlacements placements={terminatedPlacements} />
            )}
          </FixedSpaceColumn>
        )
      })}
    </CollapsibleSection>
  )
})
