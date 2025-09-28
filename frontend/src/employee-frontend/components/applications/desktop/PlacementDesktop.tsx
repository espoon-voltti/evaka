// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useQueryClient } from '@tanstack/react-query'
import orderBy from 'lodash/orderBy'
import uniqBy from 'lodash/uniqBy'
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
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
import { ApplicationUIContext } from '../../../state/application-ui'
import { useTranslation } from '../../../state/i18n'
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
  const { i18n } = useTranslation()

  const allUnits = useQueryResult(unitsQuery({}))

  if (applicationSummaries.pages > 1 || applicationSummaries.total > 50) {
    return (
      <AlertBox
        title={i18n.applications.placementDesktop.warnings.tooManyApplicationsTitle(
          applicationSummaries.total
        )}
        message={
          i18n.applications.placementDesktop.warnings.tooManyApplicationsMessage
        }
      />
    )
  }

  const primaryUnits = [
    ...new Set(applicationSummaries.data.map((a) => a.preferredUnits[0].id))
  ]

  if (primaryUnits.length > 10) {
    return (
      <AlertBox
        title={i18n.applications.placementDesktop.warnings.tooManyPrimaryUnitsTitle(
          primaryUnits.length
        )}
        message={
          i18n.applications.placementDesktop.warnings.tooManyPrimaryUnitsMessage
        }
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
    const { i18n } = useTranslation()
    const queryClient = useQueryClient()

    const { confirmedSearchFilters } = useContext(ApplicationUIContext)
    const searchedUnits = useMemo(
      () =>
        allUnits.filter((u1) =>
          (confirmedSearchFilters?.units ?? []).some((u2) => u1.id === u2)
        ),
      [allUnits, confirmedSearchFilters?.units]
    )

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

      // by default, show daycares that are
      // - one of the searched units, or
      // - a preferred unit of some result application, or
      // - already have a placement draft from some result application
      setShownDaycares(
        orderBy(
          uniqBy(
            [
              ...searchedUnits,
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
    }, [applications, searchedUnits])

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
          <div>
            {i18n.applications.placementDesktop.shownUnitsCount}:{' '}
            {shownDaycares?.length}
          </div>
          <Gap size="xs" />
          <Combobox
            items={otherAvailableUnits}
            selectedItem={null}
            onChange={(unit) => unit && onAddToShownDaycares(unit)}
            placeholder={i18n.applications.placementDesktop.addShownUnit}
            getItemLabel={(unit) => unit.name}
            fullWidth
          />
          <Gap size="s" />
          {shownDaycares !== undefined && (
            <PrefetchedDaycares
              shownDaycares={shownDaycares}
              applications={applications}
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
            {i18n.applications.placementDesktop.applicationsCount}:{' '}
            {applications.length}
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
                  allUnits={allUnits}
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
  applications,
  onUpdateApplicationPlacementSuccess,
  onUpdateApplicationPlacementFailure,
  onRemoveFromShownDaycares
}: {
  shownDaycares: PreferredUnit[]
  applications: ApplicationSummary[]
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
          applications={applications}
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
  max-width: 40%;
`

const ApplicationsColumn = styled.div`
  flex-grow: 2;
`
