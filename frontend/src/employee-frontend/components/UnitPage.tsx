// SPDX-FileCopyrightText: 2017-2022 City of Espoo
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
} from 'react-router-dom'
import styled from 'styled-components'

import { UnitResponse } from 'employee-frontend/api/unit'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Spinner from 'lib-components/atoms/state/Spinner'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { TabLinks } from 'lib-components/molecules/Tabs'
import { fontWeights, H1 } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import TabGroups from '../components/unit/TabGroups'
import TabUnitInformation from '../components/unit/TabUnitInformation'
import { useTranslation } from '../state/i18n'
import { TitleContext, TitleState } from '../state/title'
import { UnitContext, UnitContextProvider } from '../state/unit'

import TabApplicationProcess from './unit/TabApplicationProcess'
import TabCalendar from './unit/TabCalendar'
import { unitNotificationsQuery } from './unit/queries'

const defaultTab = (unit: UnitResponse) => {
  if (unit.permittedActions.has('READ_ATTENDANCES')) return 'calendar'
  if (unit.permittedActions.has('READ_OCCUPANCIES')) return 'calendar'
  if (unit.permittedActions.has('READ_GROUP_DETAILS')) return 'groups'
  return 'unit-info'
}

const UnitPage = React.memo(function UnitPage({ id }: { id: UUID }) {
  const { i18n } = useTranslation()
  const { setTitle } = useContext<TitleState>(TitleContext)
  const { unitInformation, filters, setFilters } = useContext(UnitContext)

  const unitNotifications = useQueryResult(unitNotificationsQuery(id))

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
          openGroups[cur] == true ? prev.concat(cur) : prev,
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
      (unitInformation.value.permittedActions.has('READ_OCCUPANCIES') ||
        unitInformation.value.permittedActions.has('READ_ATTENDANCES') ||
        unitInformation.value.permittedActions.has('READ_CALENDAR_EVENTS'))
        ? [
            {
              id: 'calendar',
              link: `/units/${id}/calendar`,
              label: i18n.unit.tabs.calendar
            }
          ]
        : []),
      ...(unitInformation.isSuccess &&
      unitInformation.value.permittedActions.has('READ_GROUP_DETAILS')
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
      unitInformation.value.permittedActions.has(
        'READ_APPLICATIONS_AND_PLACEMENT_PLANS'
      )
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

  if (unitInformation.isLoading) {
    return (
      <Container>
        <Spinner />
      </Container>
    )
  }
  if (unitInformation.isFailure) {
    return (
      <Container>
        <p>{i18n.common.loadingFailed}</p>
      </Container>
    )
  }

  return (
    <>
      <Container>
        <ContentArea opaque>
          <H1 noMargin>
            {unitInformation.map(({ daycare }) => daycare.name).getOrElse('')}
          </H1>
        </ContentArea>
      </Container>
      <Gap size="s" />
      <TabLinks tabs={tabs} />
      <Gap size="s" />
      <Container>
        <Routes>
          <Route path="unit-info" element={<TabUnitInformation />} />
          <Route
            path="groups"
            element={
              <TabGroups
                openGroups={openGroups}
                setOpenGroups={setOpenGroups}
              />
            }
          />
          <Route path="calendar" element={<TabCalendar />} />
          <Route
            path="calendar/events/:calendarEventId"
            element={<TabCalendar />}
          />
          <Route
            // redirect from old attendances page to the renamed calendar page
            path="attendances"
            element={<Navigate to="../calendar" replace />}
          />
          <Route
            path="application-process"
            element={<TabApplicationProcess unitId={id} />}
          />
          <Route
            index
            element={
              <Navigate replace to={defaultTab(unitInformation.value)} />
            }
          />
        </Routes>
      </Container>
    </>
  )
})

export default React.memo(function UnitPageWrapper() {
  const { id } = useNonNullableParams<{ id: UUID }>()
  return (
    <UnitContextProvider id={id}>
      <UnitPage id={id} />
    </UnitContextProvider>
  )
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
