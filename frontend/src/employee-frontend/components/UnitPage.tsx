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
import styled from 'styled-components'
import { Redirect, Route, Switch, useSearchParams, useLocation } from 'wouter'

import type { Action } from 'lib-common/generated/action'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { TabLinks } from 'lib-components/molecules/Tabs'
import { fontWeights, H1, H3 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import { useTranslation } from '../state/i18n'
import { UnitContext } from '../state/unit'
import { useTitle } from '../utils/useTitle'

import { renderResult } from './async-rendering'
import TabApplicationProcess from './unit/TabApplicationProcess'
import TabCalendar from './unit/TabCalendar'
import TabGroups from './unit/TabGroups'
import TabUnitInformation from './unit/TabUnitInformation'
import UnitServiceWorkerNote from './unit/UnitServiceWorkerNote'
import { unitNotificationsQuery, daycareQuery } from './unit/queries'

const defaultTab = (permittedActions: Action.Unit[]) => {
  if (permittedActions.includes('READ_ATTENDANCES')) return 'calendar'
  if (permittedActions.includes('READ_OCCUPANCIES')) return 'calendar'
  if (permittedActions.includes('READ_GROUP_DETAILS')) return 'groups'
  return 'unit-info'
}

export default React.memo(function UnitPage() {
  const id = useIdRouteParam<DaycareId>('id')
  const { i18n } = useTranslation()
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

  useTitle(unitInformation.map((value) => value.daycare.name))

  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({})

  const [, navigate] = useLocation()
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
        ) ||
        unitInformation.value.permittedActions.includes(
          'READ_ABSENCE_APPLICATIONS'
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
        <Switch>
          <Route path="/units/:id/unit-info">
            <TabUnitInformation unitInformation={unitInformation} />
          </Route>
          <Route path="/units/:id/groups">
            <TabGroups
              unitInformation={unitInformation}
              openGroups={openGroups}
              setOpenGroups={setOpenGroups}
            />
          </Route>
          <Route path="/units/:id/calendar">
            <TabCalendar unitInformation={unitInformation} />
          </Route>
          <Route path="/units/:id/calendar/events/:calendarEventId">
            <TabCalendar unitInformation={unitInformation} />
          </Route>
          <Route
            // redirect from old attendances page to the renamed calendar page
            path="/units/:id/attendances"
          >
            <Redirect to={`/units/${id}/calendar`} replace />
          </Route>
          <Route path="/units/:id/application-process">
            <TabApplicationProcess unitInformation={unitInformation} />
          </Route>
          <Route>
            <Redirect
              replace
              to={`/units/${id}/${defaultTab(unitInformation.permittedActions)}`}
            />
          </Route>
        </Switch>
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
