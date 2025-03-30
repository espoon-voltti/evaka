import sortBy from 'lodash/sortBy'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { DndProvider } from 'react-dnd'
import { HTML5Backend } from 'react-dnd-html5-backend'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import { combine, Result } from 'lib-common/api'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import { PlacementApplication } from 'lib-common/generated/api-types/placementdesktop'
import { ServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
import { ApplicationId, DaycareId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { H1, H2, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { serviceNeedOptionsQuery } from '../../queries'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { DaycareCard } from './DaycareCard'
import { UnplacedApplicationsList } from './UnplacedApplicationsList'
import { OnClickApplication, OnDropApplication } from './common'
import {
  placementApplicationsQuery,
  placementDaycaresQuery,
  setTrialPlacementUnitMutation,
  simpleApplicationActionMutation
} from './queries'

const WideContainer = styled.div`
  margin: 0 ${defaultMargins.L};
`

const DaycaresGrid = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-gap: 16px;

  @media (max-width: 1200px) {
    grid-template-columns: 1fr;
  }
`

export const PlacementDesktopPage = React.memo(function PlacementDesktopPage() {
  const { lang } = useTranslation()
  const navigate = useNavigate()

  const [occupancyDate, setOccupancyDate] = useState(
    LocalDate.todayInHelsinkiTz().addMonths(1)
  )

  const serviceNeedOptions = useQueryResult(serviceNeedOptionsQuery())
  const daycares = useQueryResult(placementDaycaresQuery({ occupancyDate }))
  const applications = useQueryResult(placementApplicationsQuery())
  const { mutateAsync: setTrialPlacementUnit } = useMutationResult(
    setTrialPlacementUnitMutation
  )
  const { mutateAsync: simpleApplicationAction } = useMutationResult(
    simpleApplicationActionMutation
  )

  const [selectedApplicationId, setSelectedApplicationId] =
    useState<ApplicationId | null>(null)
  const [trialPlacements, setTrialPlacements] = useState<
    Record<ApplicationId, DaycareId>
  >({})

  useEffect(() => {
    if (applications.isSuccess) {
      setTrialPlacements(
        applications.value.reduce(
          (acc, a) => ({
            ...acc,
            [a.id]: a.trialPlacementUnit
          }),
          {}
        )
      )
    }
  }, [applications])

  const selectedApplication = useMemo(
    () =>
      selectedApplicationId
        ? (applications
            .getOrElse([])
            .find((a) => a.id === selectedApplicationId) ?? null)
        : null,
    [selectedApplicationId, applications]
  )

  const onClickApplication: OnClickApplication = useCallback(
    (applicationId) => {
      if (applicationId === selectedApplicationId) {
        setSelectedApplicationId(null)
      } else {
        setSelectedApplicationId(applicationId)
      }
    },
    [selectedApplicationId]
  )

  const onDropApplication: OnDropApplication = (applicationId, daycareId) => {
    setTrialPlacements((prev) => ({
      ...prev,
      [applicationId]: daycareId
    }))
    void setTrialPlacementUnit({
      applicationId,
      body: { trialPlacementUnit: daycareId }
    })
  }

  const getPreferenceNumber = useCallback(
    (daycareId: DaycareId) => {
      if (selectedApplication === null) return null
      const i = selectedApplication.preferredUnits.findIndex(
        (id) => id === daycareId
      )
      return i < 0 ? null : i + 1
    },
    [selectedApplication]
  )

  const sortedDaycares = useMemo(
    () =>
      daycares.map((daycares) =>
        sortBy(
          daycares,
          (d) => getPreferenceNumber(d.id),
          (d) => d.name
        )
      ),
    [daycares, getPreferenceNumber]
  )

  const daycareNames = useMemo(
    () =>
      daycares.map((daycares) => new Map(daycares.map((d) => [d.id, d.name]))),
    [daycares]
  )

  const plannedApplications = useMemo(
    () =>
      applications.map((applications) =>
        applications.reduce<Record<DaycareId, PlacementApplication[]>>(
          (acc, a) =>
            a.plannedPlacementUnit
              ? {
                  ...acc,
                  [a.plannedPlacementUnit]: [
                    ...(acc[a.plannedPlacementUnit] || []),
                    a
                  ]
                }
              : acc,
          {}
        )
      ),
    [applications]
  )

  const trialApplications = useMemo(
    () =>
      applications.map((applications) =>
        applications.reduce<Record<DaycareId, PlacementApplication[]>>(
          (acc, a) =>
            a.plannedPlacementUnit === null && !!trialPlacements[a.id]
              ? {
                  ...acc,
                  [trialPlacements[a.id]]: [
                    ...(acc[trialPlacements[a.id]] || []),
                    a
                  ]
                }
              : acc,
          {}
        )
      ),
    [applications, trialPlacements]
  )

  const unplannedApplications = useMemo(
    () =>
      applications.map((applications) =>
        applications.filter(
          (a) => a.plannedPlacementUnit === null && !trialPlacements[a.id]
        )
      ),
    [applications, trialPlacements]
  )

  const serviceNeedDefaults: Result<Map<PlacementType, ServiceNeedOption>> =
    useMemo(() => {
      return serviceNeedOptions.map(
        (serviceNeedOptions) =>
          new Map(
            serviceNeedOptions
              .filter((sno) => sno.defaultOption)
              .map((sno) => [sno.validPlacementType, sno])
          )
      )
    }, [serviceNeedOptions])

  return (
    <DndProvider backend={HTML5Backend}>
      <WideContainer>
        <ContentArea opaque>
          <FixedSpaceRow justifyContent="space-between" alignItems="center">
            <H1 noMargin>Sijoittelutyöpöytä</H1>
            <FixedSpaceRow spacing="s" alignItems="center">
              <Label>Täyttöpäivä</Label>
              <DatePicker
                minDate={LocalDate.todayInHelsinkiTz()}
                date={occupancyDate}
                onChange={(date) => (date ? setOccupancyDate(date) : undefined)}
                locale={lang}
              />
            </FixedSpaceRow>
          </FixedSpaceRow>
        </ContentArea>
      </WideContainer>
      <Gap />
      <WideContainer>
        <FixedSpaceRow>
          <ContentArea
            opaque
            style={{
              flexGrow: 3,
              maxHeight: '70vh',
              overflowY: 'auto',
              overflowX: 'visible'
            }}
          >
            <H2>Yksiköt</H2>
            {renderResult(
              combine(sortedDaycares, daycareNames, serviceNeedDefaults),
              ([sortedDaycares, daycareNames, serviceNeedDefaults]) => (
                <DaycaresGrid>
                  {sortedDaycares.map((d) => (
                    <DaycareCard
                      key={d.id}
                      daycare={d}
                      daycareNames={daycareNames}
                      preferenceNumber={getPreferenceNumber(d.id)}
                      trialApplications={trialApplications.map(
                        (byDaycare) => byDaycare[d.id] ?? []
                      )}
                      plannedApplications={plannedApplications.map(
                        (byDaycare) => byDaycare[d.id] ?? []
                      )}
                      selectedApplicationId={selectedApplicationId}
                      occupancyDate={occupancyDate}
                      serviceNeedDefaults={serviceNeedDefaults}
                      onDropApplication={onDropApplication}
                      onClickApplication={onClickApplication}
                      onLockPlacement={(applicationId) => {
                        void navigate(
                          `/applications/${applicationId}/placement?placement-desktop=true`
                        )
                      }}
                      onUnlockPlacement={(applicationId) => {
                        void simpleApplicationAction({
                          applicationId,
                          action: 'CANCEL_PLACEMENT_PLAN'
                        })
                      }}
                    />
                  ))}
                </DaycaresGrid>
              )
            )}
          </ContentArea>
          <ContentArea
            opaque
            style={{
              flexGrow: 2,
              maxWidth: '600px',
              maxHeight: '70vh',
              overflowY: 'auto',
              overflowX: 'visible'
            }}
          >
            <FixedSpaceColumn
              alignItems="stretch"
              justifyContent="stretch"
              style={{ height: '100%' }}
            >
              <H2 noMargin>Hakemukset</H2>
              {renderResult(daycareNames, (daycareNames) => (
                <UnplacedApplicationsList
                  applications={unplannedApplications}
                  selectedApplicationId={selectedApplicationId}
                  daycareNames={daycareNames}
                  occupancyDate={occupancyDate}
                  onDropApplication={onDropApplication}
                  onClickApplication={onClickApplication}
                />
              ))}
            </FixedSpaceColumn>
          </ContentArea>
        </FixedSpaceRow>
      </WideContainer>
    </DndProvider>
  )
})
