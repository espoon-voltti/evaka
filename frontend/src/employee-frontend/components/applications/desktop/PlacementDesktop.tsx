// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useQueryClient } from '@tanstack/react-query'
import orderBy from 'lodash/orderBy'
import uniqBy from 'lodash/uniqBy'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'

import type {
  ApplicationSummary,
  PagedApplicationSummaries,
  PreferredUnit
} from 'lib-common/generated/api-types/application'
import type { UnitStub } from 'lib-common/generated/api-types/daycare'
import type {
  ApplicationId,
  DaycareId
} from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Spinner from 'lib-components/atoms/state/Spinner'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { Gap } from 'lib-components/white-space'

import { unitsQuery } from '../../../queries'
import { renderResult } from '../../async-rendering'
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
  const allUnits = useQueryResult(unitsQuery({}))

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

  return renderResult(allUnits, (allUnits) => (
    <PlacementDesktopValidated
      applications={applicationSummaries.data}
      allUnits={allUnits}
    />
  ))
})

const PlacementDesktopValidated = React.memo(
  function PlacementDesktopValidated({
    applications,
    allUnits
  }: {
    applications: ApplicationSummary[]
    allUnits: UnitStub[]
  }) {
    const queryClient = useQueryClient()

    // optimistic cache to avoid refetching all applications when updating placements drafts
    const [placementDraftUnits, setPlacementDraftUnits] = useState<
      Record<ApplicationId, PreferredUnit | null>
    >({})

    const [shownDaycares, setShownDaycares] = useState<PreferredUnit[]>()

    const otherAvailableUnits = useMemo(
      () =>
        allUnits.filter(
          (u) => !shownDaycares || !shownDaycares.some((u2) => u.id === u2.id)
        ),
      [allUnits, shownDaycares]
    )

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

      // by default, show daycares that are either a preferred unit or already have a placement draft
      setShownDaycares(
        orderBy(
          uniqBy(
            [
              ...applications.map((a) => a.preferredUnits[0]),
              ...applications.flatMap((a) =>
                a.placementDraftUnit ? [a.placementDraftUnit] : []
              )
            ],
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

    const onAddToShownDaycares = useCallback((unit: PreferredUnit) => {
      setShownDaycares((prev) =>
        prev
          ? orderBy(
              [...prev.filter((u) => u.id !== unit.id), unit],
              (u) => u.name
            )
          : prev
      )
    }, [])

    const onRemoveFromShownDaycares = useCallback((unitId: DaycareId) => {
      setShownDaycares((prev) =>
        prev ? [...prev.filter((u) => u.id !== unitId)] : prev
      )
    }, [])

    return (
      <FixedSpaceRow>
        <DaycaresColumn>
          <div>Näytettäviä ysiköitä: {shownDaycares?.length}</div>
          <Gap size="xs" />
          <Combobox
            items={otherAvailableUnits}
            selectedItem={null}
            onChange={(unit) => unit && onAddToShownDaycares(unit)}
            placeholder="Lisää yksikkö"
            getItemLabel={(unit) => unit.name}
          />
          <Gap size="s" />
          {shownDaycares !== undefined && (
            <PrefetchedDaycares
              shownDaycares={shownDaycares}
              onUpdateApplicationPlacementSuccess={
                onUpdateApplicationPlacementSuccess
              }
              onUpdateApplicationPlacementFailure={
                onUpdateApplicationPlacementFailure
              }
              onAddToShownDaycares={onAddToShownDaycares}
              onRemoveFromShownDaycares={onRemoveFromShownDaycares}
            />
          )}
        </DaycaresColumn>

        <ApplicationsColumn>
          <div style={{ textAlign: 'right' }}>
            Hakemuksia: {applications.length}
          </div>
          <Gap size="s" />
          <FixedSpaceColumn alignItems="flex-end">
            {shownDaycares !== undefined &&
              applications.map((application) => (
                <ApplicationCard
                  key={application.id}
                  application={{
                    ...application,
                    placementDraftUnit:
                      placementDraftUnits[application.id] ?? null
                  }}
                  shownDaycares={shownDaycares}
                  onUpdateApplicationPlacementSuccess={
                    onUpdateApplicationPlacementSuccess
                  }
                  onUpdateApplicationPlacementFailure={
                    onUpdateApplicationPlacementFailure
                  }
                  onAddToShownDaycares={onAddToShownDaycares}
                />
              ))}
          </FixedSpaceColumn>
        </ApplicationsColumn>
      </FixedSpaceRow>
    )
  }
)

const PrefetchedDaycares = React.memo(function PrefetchedDaycares({
  shownDaycares,
  onUpdateApplicationPlacementSuccess,
  onUpdateApplicationPlacementFailure,
  onRemoveFromShownDaycares
}: {
  shownDaycares: PreferredUnit[]
  onUpdateApplicationPlacementSuccess: (
    applicationId: ApplicationId,
    unit: PreferredUnit | null
  ) => void
  onUpdateApplicationPlacementFailure: () => void
  onAddToShownDaycares: (unit: PreferredUnit) => void
  onRemoveFromShownDaycares: (unitId: DaycareId) => void
}) {
  const queryClient = useQueryClient()
  const initialData = useQueryResult(
    getPlacementDesktopDaycaresQuery({
      unitIds: shownDaycares.map((d) => d.id)
    })
  )
  const [initialDataInserted, setInitialDataInserted] = useState(false)

  useEffect(() => {
    if (initialData.isSuccess) {
      initialData.value.forEach((unit) => {
        void queryClient.setQueryData(
          getPlacementDesktopDaycareQuery({ unitId: unit.id }).queryKey,
          unit
        )
      })
      setInitialDataInserted(true)
    }
  }, [initialData, queryClient])

  if (!initialDataInserted) return <Spinner />

  return (
    <FixedSpaceColumn>
      {shownDaycares.map((daycare) => (
        <DaycareCard
          key={daycare.id}
          daycare={daycare}
          onUpdateApplicationPlacementSuccess={
            onUpdateApplicationPlacementSuccess
          }
          onUpdateApplicationPlacementFailure={
            onUpdateApplicationPlacementFailure
          }
          onRemoveFromShownDaycares={() =>
            onRemoveFromShownDaycares(daycare.id)
          }
        />
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
