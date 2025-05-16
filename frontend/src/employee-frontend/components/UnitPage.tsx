// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import {
  Navigate,
  Route,
  Routes,
  useNavigate,
  useSearchParams
} from 'react-router'
import styled from 'styled-components'

import type { DaycareResponse } from 'lib-common/generated/api-types/daycare'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { TabLinks } from 'lib-components/molecules/Tabs'
import { fontWeights, H1, H3 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import TabGroups from '../components/unit/TabGroups'
import TabUnitInformation from '../components/unit/TabUnitInformation'
import { useTranslation } from '../state/i18n'
import type { TitleState } from '../state/title'
import { TitleContext } from '../state/title'
import { UnitContext } from '../state/unit'

import { renderResult } from './async-rendering'
import TabApplicationProcess from './unit/TabApplicationProcess'
import TabCalendar from './unit/TabCalendar'
import UnitServiceWorkerNote from './unit/UnitServiceWorkerNote'
import { unitNotificationsQuery, daycareQuery } from './unit/queries'

const defaultTab = (unit: DaycareResponse) => {
  if (unit.permittedActions.includes('READ_ATTENDANCES')) return 'calendar'
  if (unit.permittedActions.includes('READ_OCCUPANCIES')) return 'calendar'
  if (unit.permittedActions.includes('READ_GROUP_DETAILS')) return 'groups'
  return 'unit-info'
}

export default React.memo(function UnitPage() {
  const id = useIdRouteParam<DaycareId>('id')
  const { i18n } = useTranslation()
  const { setTitle } = useContext<TitleState>(TitleContext)
  const { filters, setFilters } = useContext(UnitContext)

  const unitInformation = useQueryResult(daycareQuery({ daycareId: id }))

  const unitNotifications = useQueryResult(
    unitNotificationsQuery({ daycareId: id })
  )

  const [searchParams, setSearchParams] = useSearchParams()
  useEffect(() => {
    if (searchParams.has('start')) {
      const queryStart = LocalDate.parseIso(searchParams.get('start') ?? '')
      setFilters(filters.withStartDate(queryStart))
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (unitInformation.isSuccess) {
      setTitle(unitInformation.value.daycare.name)
    }
  }, [setTitle, unitInformation])

  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({})

  const navigate = useNavigate()
  const queryParams = useCallback(
    () => new URLSearchParams(location.search),
    []
  )

  const getOpenGroupsFromQueryParams = useCallback((): Record<
    string,
    boolean
  > => {
    const qp = queryParams().get('open_groups')
    if (qp != null) {
      return qp
        .split(',')
        .reduce(
          (prev, cur) => (cur ? Object.assign(prev, { [cur]: true }) : prev),
          {}
        )
    } else {
      return {}
    }
  }, [queryParams])

  useEffect(() => {
    setOpenGroups(getOpenGroupsFromQueryParams())
  }, [getOpenGroupsFromQueryParams])

  const openGroupsToStringList = (
    openGroups: Record<string, boolean>
  ): string =>
    Object.keys(openGroups)
      .reduce(
        (prev: string[], cur: string) =>
          openGroups[cur] ? prev.concat(cur) : prev,
        []
      )
      .join(',')

  useEffect(() => {
    const openList = openGroupsToStringList(openGroups)
    if (openList.length > 0) {
      searchParams.set('open_groups', openList)
      setSearchParams(searchParams, {
        replace: true
      })
    }
  }, [openGroups, navigate, setSearchParams, searchParams])

  const tabs = useMemo(
    () => [
      ...(unitInformation.isSuccess &&
      (unitInformation.value.permittedActions.includes('READ_OCCUPANCIES') ||
        unitInformation.value.permittedActions.includes('READ_ATTENDANCES') ||
        unitInformation.value.permittedActions.includes('READ_CALENDAR_EVENTS'))
        ? [
            {
              id: 'calendar',
              link: `/units/${id}/calendar`,
              label: i18n.unit.tabs.calendar
            }
          ]
        : []),
      ...(unitInformation.isSuccess &&
      unitInformation.value.permittedActions.includes('READ_GROUP_DETAILS')
        ? [
            {
              id: 'groups',
              link: `/units/${id}/groups`,
              label: i18n.unit.tabs.groups,
              counter: unitNotifications
                .map(({ groups }) => groups)
                .getOrElse(0)
            }
          ]
        : []),
      ...(unitInformation.isSuccess &&
      (unitInformation.value.permittedActions.includes(
        'READ_APPLICATIONS_AND_PLACEMENT_PLANS'
      ) ||
        unitInformation.value.permittedActions.includes(
          'READ_SERVICE_APPLICATIONS'
        ))
        ? [
            {
              id: 'application-process',
              link: `/units/${id}/application-process`,
              label: i18n.unit.tabs.applicationProcess,
              counter: unitNotifications
                .map(({ applications }) => applications)
                .getOrElse(0)
            }
          ]
        : []),
      {
        id: 'unit-info',
        link: `/units/${id}/unit-info`,
        label: i18n.unit.tabs.unitInfo
      }
    ],
    [id, i18n, unitInformation, unitNotifications]
  )

  return renderResult(unitInformation, (unitInformation) => (
    <>
      <Container>
        <ContentArea opaque>
          <H1 noMargin>{unitInformation.daycare.name}</H1>
        </ContentArea>
      </Container>
      {unitInformation.permittedActions.includes(
        'READ_SERVICE_WORKER_NOTE'
      ) && (
        <>
          <Gap size="s" />
          <Container>
            <ContentArea opaque>
              <H3 noMargin>{i18n.unit.serviceWorkerNote.title}</H3>
              <Gap size="s" />
              <UnitServiceWorkerNote
                unitId={id}
                canEdit={unitInformation.permittedActions.includes(
                  'SET_SERVICE_WORKER_NOTE'
                )}
              />
            </ContentArea>
          </Container>
        </>
      )}
      <Gap size="s" />
      <TabLinks tabs={tabs} />
      <Gap size="s" />
      <Container>
        <Routes>
          <Route
            path="unit-info"
            element={<TabUnitInformation unitInformation={unitInformation} />}
          />
          <Route
            path="groups"
            element={
              <TabGroups
                unitInformation={unitInformation}
                openGroups={openGroups}
                setOpenGroups={setOpenGroups}
              />
            }
          />
          <Route
            path="calendar"
            element={<TabCalendar unitInformation={unitInformation} />}
          />
          <Route
            path="calendar/events/:calendarEventId"
            element={<TabCalendar unitInformation={unitInformation} />}
          />
          <Route
            // redirect from old attendances page to the renamed calendar page
            path="attendances"
            element={<Navigate to="../calendar" replace />}
          />
          <Route
            path="application-process"
            element={
              <TabApplicationProcess unitInformation={unitInformation} />
            }
          />
          <Route
            index
            element={<Navigate replace to={defaultTab(unitInformation)} />}
          />
        </Routes>
      </Container>
    </>
  ))
})

export const NotificationCounter = styled.div`
  width: 1.25em;
  height: 1.25em;
  display: inline-flex;
  justify-content: center;
  align-items: center;

  border-radius: 50%;
  background-color: ${(p) => p.theme.colors.status.warning};
  color: ${(p) => p.theme.colors.grayscale.g0};
  margin-left: ${defaultMargins.xs};
  font-weight: ${fontWeights.bold};
  font-size: 0.75em;
`
