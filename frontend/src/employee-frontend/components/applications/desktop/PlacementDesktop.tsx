// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useQueryClient } from '@tanstack/react-query'
import React, { useCallback, useEffect, useState } from 'react'
import styled from 'styled-components'

import type {
  ApplicationSummary,
  PagedApplicationSummaries
} from 'lib-common/generated/api-types/application'
import type {
  ApplicationId,
  DaycareId
} from 'lib-common/generated/api-types/shared'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { Gap } from 'lib-components/white-space'

import { getApplicationSummariesQuery } from '../queries'

import ApplicationCard from './ApplicationCard'

export default React.memo(function PlacementDesktop({
  applicationSummaries
}: {
  applicationSummaries: PagedApplicationSummaries
}) {
  if (applicationSummaries.pages > 1 || applicationSummaries.total > 50) {
    return (
      <AlertBox
        title={`Liikaa hakemuksia (${applicationSummaries.total})`}
        message="Tarkenna hakuehtoja niin, että hakemuksia on enintään 50 kpl."
      />
    )
  }

  const primaryUnits = [
    ...new Set(applicationSummaries.data.map((a) => a.preferredUnits[0].id))
  ]

  if (primaryUnits.length > 10) {
    return (
      <AlertBox
        title={`Liikaa ensisijaisia hakuyksiköitä (${primaryUnits.length})`}
        message="Tarkenna hakuehtoja niin, että hakemuksien ykköstoiveissa on enintään 10 eri yksikköä."
      />
    )
  }

  return (
    <PlacementDesktopValidated
      applications={applicationSummaries.data}
      primaryUnits={primaryUnits}
    />
  )
})

const PlacementDesktopValidated = React.memo(
  function PlacementDesktopValidated({
    applications,
    primaryUnits
  }: {
    applications: ApplicationSummary[]
    primaryUnits: DaycareId[]
  }) {
    const queryClient = useQueryClient()

    // optimistic cache to avoid refetching all applications when updating trial placements
    const [trialUnits, setTrialUnits] = useState<
      Record<ApplicationId, DaycareId | null>
    >({})
    useEffect(() => {
      setTrialUnits(
        applications.reduce(
          (acc, application) => ({
            ...acc,
            [application.id]: application.trialPlacementUnit
          }),
          {}
        )
      )
    }, [applications])

    const onUpdateApplicationPlacementSuccess = useCallback(
      (applicationId: ApplicationId, unitId: DaycareId | null) => {
        setTrialUnits((prev) => ({
          ...prev,
          [applicationId]: unitId
        }))
      },
      []
    )

    const onUpdateApplicationPlacementFailure = useCallback(() => {
      void queryClient.invalidateQueries({
        queryKey: getApplicationSummariesQuery.prefix,
        type: 'all'
      })
    }, [queryClient])

    return (
      <FixedSpaceRow>
        <DaycaresColumn>Yksiköitä: {primaryUnits.length}</DaycaresColumn>
        <ApplicationsColumn>
          <div style={{ textAlign: 'right' }}>
            Hakemuksia: {applications.length}
          </div>
          <Gap size="s" />
          <FixedSpaceColumn alignItems="flex-end">
            {applications.map((application) => (
              <ApplicationCard
                key={application.id}
                application={{
                  ...application,
                  trialPlacementUnit: trialUnits[application.id]
                }}
                onUpdateApplicationPlacementSuccess={
                  onUpdateApplicationPlacementSuccess
                }
                onUpdateApplicationPlacementFailure={
                  onUpdateApplicationPlacementFailure
                }
              />
            ))}
          </FixedSpaceColumn>
        </ApplicationsColumn>
      </FixedSpaceRow>
    )
  }
)

const DaycaresColumn = styled.div`
  flex-grow: 1;
  width: 50%;
`

const ApplicationsColumn = styled.div`
  flex-grow: 1;
  width: 50%;
`
