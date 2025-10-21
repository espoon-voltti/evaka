// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useQueryClient } from '@tanstack/react-query'
import isEqual from 'lodash/isEqual'
import orderBy from 'lodash/orderBy'
import uniqBy from 'lodash/uniqBy'
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  useRef
} from 'react'
import styled from 'styled-components'

import type {
  ApplicationSummary,
  ApplicationSummaryPlacementDraft,
  PagedApplicationSummaries,
  PreferredUnit
} from 'lib-common/generated/api-types/application'
import type { UnitStub } from 'lib-common/generated/api-types/daycare'
import type {
  ApplicationId,
  DaycareId
} from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import type { Arg0 } from 'lib-common/types'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Spinner from 'lib-components/atoms/state/Spinner'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { defaultMargins, Gap } from 'lib-components/white-space'

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
    const { i18n, lang } = useTranslation()
    const queryClient = useQueryClient()

    const {
      confirmedSearchFilters,
      placementDesktopDaycares: shownDaycares,
      setPlacementDesktopDaycares: _setShownDaycares,
      occupancyPeriodStart,
      setOccupancyPeriodStart
    } = useContext(ApplicationUIContext)
    const searchedUnits = useMemo(
      () =>
        allUnits.filter((u1) =>
          (confirmedSearchFilters?.units ?? []).some((u2) => u1.id === u2)
        ),
      [allUnits, confirmedSearchFilters?.units]
    )

    // optimistic cache to avoid refetching all applications when updating placements drafts
    const [placementDraftCache, setPlacementDraftCache] = useState<
      Record<ApplicationId, ApplicationSummaryPlacementDraft | undefined>
    >({})

    const daycareListRef = useRef<HTMLDivElement>(null)
    const [daycareRefs, setDaycareRefs] = useState<
      Record<DaycareId, React.RefObject<HTMLDivElement>>
    >({})

    // set this to scroll to a specific daycare and trigger an animation
    // as soon as it's visible and a ref is available
    const [highlightedDaycare, setHighlightedDaycareDaycare] =
      useState<DaycareId | null>(null)
    const resetHighlightRef = useRef<ReturnType<typeof setTimeout> | null>(null)
    useEffect(() => {
      // useEffect is needed to wait for the DOM to update
      if (
        highlightedDaycare &&
        daycareRefs[highlightedDaycare]?.current &&
        daycareListRef.current
      ) {
        const daycareElement = daycareRefs[highlightedDaycare].current
        const daycareListElement = daycareListRef.current
        daycareListElement.scrollTo({
          top: daycareElement.offsetTop - daycareListElement.offsetTop,
          behavior: 'smooth'
        })
      }

      // reset the highlight after the animation has run, so that it can be retriggered
      if (resetHighlightRef.current) {
        clearTimeout(resetHighlightRef.current)
      }
      if (highlightedDaycare) {
        resetHighlightRef.current = setTimeout(() => {
          setHighlightedDaycareDaycare(null)
        }, 1000)
      }

      return () => {
        if (resetHighlightRef.current) {
          clearTimeout(resetHighlightRef.current)
        }
      }
    }, [highlightedDaycare, daycareRefs])

    const setShownDaycares = useCallback(
      (units: PreferredUnit[]) => {
        _setShownDaycares(units)

        setDaycareRefs((prev) =>
          units.reduce(
            (acc, daycare) => ({
              ...acc,
              [daycare.id]:
                prev[daycare.id] ?? React.createRef<HTMLDivElement>()
            }),
            {}
          )
        )
      },
      [_setShownDaycares]
    )

    const otherAvailableUnits = useMemo(
      () =>
        allUnits.filter(
          (u) => !shownDaycares || !shownDaycares.some((u2) => u.id === u2.id)
        ),
      [allUnits, shownDaycares]
    )

    useEffect(() => {
      setPlacementDraftCache(
        applications.reduce(
          (acc, application) => ({
            ...acc,
            [application.id]: application.placementDraft
          }),
          {}
        )
      )
    }, [applications])

    useEffect(() => {
      if (shownDaycares !== undefined) return

      // By default, show daycares that are
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
                a.placementDraft ? [a.placementDraft.unit] : []
              )
            ],
            (u) => u.id
          ),
          (u) => u.name
        )
      )
    }, [applications, searchedUnits, shownDaycares, setShownDaycares])

    const onUpsertApplicationPlacementSuccess = useCallback(
      (
        applicationId: ApplicationId,
        unit: PreferredUnit,
        startDate: LocalDate
      ) => {
        setPlacementDraftCache((prev) => ({
          ...prev,
          [applicationId]: { unit, startDate }
        }))
      },
      []
    )

    const onDeleteApplicationPlacementSuccess = useCallback(
      (applicationId: ApplicationId) => {
        setPlacementDraftCache((prev) => ({
          ...prev,
          [applicationId]: undefined
        }))
      },
      []
    )

    const onMutateApplicationPlacementFailure = useCallback(() => {
      void queryClient.invalidateQueries({
        queryKey: getApplicationSummariesQuery.prefix,
        type: 'all'
      })
    }, [queryClient])

    const onAddToShownDaycares = useCallback(
      (unit: PreferredUnit) => {
        if (shownDaycares) {
          setShownDaycares(
            orderBy(
              [...shownDaycares.filter((u) => u.id !== unit.id), unit],
              (u) => u.name
            )
          )
        }
      },
      [shownDaycares, setShownDaycares]
    )

    const onRemoveFromShownDaycares = useCallback(
      (unitId: DaycareId) => {
        if (shownDaycares) {
          setShownDaycares(shownDaycares.filter((u) => u.id !== unitId))
        }
      },
      [shownDaycares, setShownDaycares]
    )

    return (
      <FixedSpaceColumn spacing="L">
        <FixedSpaceRow alignItems="center">
          <div>{i18n.applications.placementDesktop.occupancyPeriod}:</div>
          <DatePicker
            date={occupancyPeriodStart}
            onChange={(date) =>
              setOccupancyPeriodStart(date ?? LocalDate.todayInHelsinkiTz())
            }
            locale={lang}
          />
          <div>—</div>
          <div>{occupancyPeriodStart.addMonths(3).format()}</div>
        </FixedSpaceRow>
        <FixedSpaceRow>
          <DaycaresColumn ref={daycareListRef}>
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
                daycareRefs={daycareRefs}
                highlightedDaycare={highlightedDaycare}
                applications={applications}
                onDeleteApplicationPlacementSuccess={
                  onDeleteApplicationPlacementSuccess
                }
                onMutateApplicationPlacementFailure={
                  onMutateApplicationPlacementFailure
                }
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
            <FixedSpaceColumn alignItems="flex-end" style={{ flexGrow: 1 }}>
              {shownDaycares !== undefined &&
                applications.map((application) => (
                  <ApplicationCard
                    key={application.id}
                    application={{
                      ...application,
                      placementDraft:
                        placementDraftCache[application.id] ?? null
                    }}
                    shownDaycares={shownDaycares}
                    allUnits={allUnits}
                    onUpsertApplicationPlacementSuccess={
                      onUpsertApplicationPlacementSuccess
                    }
                    onDeleteApplicationPlacementSuccess={
                      onDeleteApplicationPlacementSuccess
                    }
                    onMutateApplicationPlacementFailure={
                      onMutateApplicationPlacementFailure
                    }
                    onAddOrHighlightDaycare={(unit) => {
                      onAddToShownDaycares(unit)
                      setHighlightedDaycareDaycare(unit.id)
                    }}
                  />
                ))}
            </FixedSpaceColumn>
          </ApplicationsColumn>
        </FixedSpaceRow>
      </FixedSpaceColumn>
    )
  }
)

const PrefetchedDaycares = React.memo(function PrefetchedDaycares({
  shownDaycares,
  daycareRefs,
  highlightedDaycare,
  applications,
  onDeleteApplicationPlacementSuccess,
  onMutateApplicationPlacementFailure,
  onRemoveFromShownDaycares
}: {
  shownDaycares: PreferredUnit[]
  daycareRefs: Record<DaycareId, React.RefObject<HTMLDivElement | null>>
  highlightedDaycare: DaycareId | null
  applications: ApplicationSummary[]
  onDeleteApplicationPlacementSuccess: (applicationId: ApplicationId) => void
  onMutateApplicationPlacementFailure: () => void
  onRemoveFromShownDaycares: (unitId: DaycareId) => void
}) {
  const queryClient = useQueryClient()
  const { occupancyPeriodStart } = useContext(ApplicationUIContext)

  // do not refetch when shownDaycares change, unless applications also change
  // to avoid refetching everything when adding/removing a single shown daycare
  const queryArg: Arg0<typeof getPlacementDesktopDaycaresQuery> = useMemo(
    () => ({
      unitIds: shownDaycares.map((d) => d.id),
      occupancyStart: occupancyPeriodStart
    }),
    [occupancyPeriodStart, applications] // eslint-disable-line react-hooks/exhaustive-deps
  )

  const initialData = useQueryResult(getPlacementDesktopDaycaresQuery(queryArg))

  const [initialDataInsertedForArgs, setInitialDataInsertedForArgs] =
    useState<Arg0<typeof getPlacementDesktopDaycaresQuery> | null>(null)

  useEffect(() => {
    if (initialData.isSuccess && !initialData.isReloading) {
      initialData.value.forEach((unit) => {
        void queryClient.setQueryData(
          getPlacementDesktopDaycareQuery({
            unitId: unit.id,
            occupancyStart: queryArg.occupancyStart
          }).queryKey,
          unit
        )
      })
      setInitialDataInsertedForArgs(queryArg)
    }
  }, [initialData, queryArg, queryClient])

  const initialDataUpToDate = isEqual(queryArg, initialDataInsertedForArgs)

  if (!initialDataUpToDate || !initialData.isSuccess || initialData.isReloading)
    return <Spinner />

  return (
    <FixedSpaceColumn>
      {shownDaycares.map((daycare) => (
        <DaycareCard
          key={daycare.id}
          daycare={daycare}
          highlighted={daycare.id === highlightedDaycare}
          applications={applications}
          onDeleteApplicationPlacementSuccess={
            onDeleteApplicationPlacementSuccess
          }
          onMutateApplicationPlacementFailure={
            onMutateApplicationPlacementFailure
          }
          onRemoveFromShownDaycares={() =>
            onRemoveFromShownDaycares(daycare.id)
          }
          ref={daycareRefs[daycare.id]}
        />
      ))}
    </FixedSpaceColumn>
  )
})

const DaycaresColumn = styled.div`
  flex-grow: 1;
  max-width: 40%;
  padding-left: ${defaultMargins.xs};
  padding-right: ${defaultMargins.m};
  max-height: 90vh;
  overflow-y: auto;
`

const ApplicationsColumn = styled.div`
  flex-grow: 1;
  padding-right: ${defaultMargins.m};
  max-height: 90vh;
  overflow-y: auto;
`
