// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useQueryClient } from '@tanstack/react-query'
import orderBy from 'lodash/orderBy'
import uniqBy from 'lodash/uniqBy'
import React, { useCallback, useEffect, useState } from 'react'
import styled from 'styled-components'

import type {
  ApplicationSummary,
  PagedApplicationSummaries,
  PreferredUnit
} from 'lib-common/generated/api-types/application'
import type {
  ApplicationId,
  DaycareId
} from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import Spinner from 'lib-components/atoms/state/Spinner'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { Gap } from 'lib-components/white-space'

import {
  getApplicationSummariesQuery,
  getPlacementDesktopDaycareQuery,
  getPlacementDesktopDaycaresQuery
} from '../queries'

import ApplicationCard from './ApplicationCard'
import DaycareCard from './DaycareCard'

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

    // optimistic cache to avoid refetching all applications when updating placements drafts
    const [placementDraftUnits, setPlacementDraftUnits] = useState<
      Record<ApplicationId, PreferredUnit | null>
    >({})

    const [shownDaycares, setShownDaycares] = useState<PreferredUnit[]>()

    useEffect(() => {
      setPlacementDraftUnits(
        applications.reduce(
          (acc, application) => ({
            ...acc,
            [application.id]: application.placementDraftUnit
          }),
          {}
        )
      )
      setShownDaycares(
        orderBy(
          uniqBy(
            applications.flatMap((a) => a.preferredUnits),
            (u) => u.id
          ),
          (u) => u.name
        )
      )
    }, [applications])

    const onUpdateApplicationPlacementSuccess = useCallback(
      (applicationId: ApplicationId, unit: PreferredUnit | null) => {
        setPlacementDraftUnits((prev) => ({
          ...prev,
          [applicationId]: unit
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
        <DaycaresColumn>
          <div>Yksiköitä: {shownDaycares?.length}</div>
          <Gap size="s" />
          {shownDaycares !== undefined && (
            <PrefetchedDaycares shownDaycares={shownDaycares} />
          )}
        </DaycaresColumn>

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
                  placementDraftUnit:
                    placementDraftUnits[application.id] ?? null
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

const PrefetchedDaycares = React.memo(function PrefetchedDaycares({
  shownDaycares
}: {
  shownDaycares: PreferredUnit[]
}) {
  const queryClient = useQueryClient()
  const initialData = useQueryResult(
    getPlacementDesktopDaycaresQuery({
      daycareIds: shownDaycares.map((d) => d.id)
    })
  )
  const [initialDataInserted, setInitialDataInserted] = useState(false)

  useEffect(() => {
    if (initialData.isSuccess) {
      initialData.value.forEach((daycare) => {
        void queryClient.setQueryData(
          getPlacementDesktopDaycareQuery({ daycareId: daycare.id }).queryKey,
          daycare
        )
      })
      setInitialDataInserted(true)
    }
  }, [initialData, queryClient])

  if (!initialDataInserted) return <Spinner />

  return (
    <FixedSpaceColumn>
      {shownDaycares.map((daycare) => (
        <DaycareCard key={daycare.id} daycare={daycare} />
      ))}
    </FixedSpaceColumn>
  )
})

const DaycaresColumn = styled.div`
  flex-grow: 1;
  width: 50%;
`

const ApplicationsColumn = styled.div`
  flex-grow: 1;
  width: 50%;
`
