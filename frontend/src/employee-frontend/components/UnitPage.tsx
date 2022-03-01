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
  Redirect,
  Route,
  Switch,
  useHistory,
  useParams
} from 'react-router-dom'
import styled from 'styled-components'

import { isLoading } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useQuery } from 'lib-common/utils/useQuery'
import Container from 'lib-components/layout/Container'
import Tabs from 'lib-components/molecules/Tabs'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'

import TabGroups from '../components/unit/TabGroups'
import TabUnitInformation from '../components/unit/TabUnitInformation'
import { useTranslation } from '../state/i18n'
import { TitleContext, TitleState } from '../state/title'
import { UnitContext, UnitContextProvider } from '../state/unit'

import TabApplicationProcess from './unit/TabApplicationProcess'
import TabCalendar from './unit/TabCalendar'

const UnitPage = React.memo(function UnitPage({ id }: { id: UUID }) {
  const { i18n } = useTranslation()
  const { setTitle } = useContext<TitleState>(TitleContext)
  const { unitInformation, unitData, reloadUnitData, filters, setFilters } =
    useContext(UnitContext)

  const query = useQuery()
  useEffect(() => {
    if (query.has('start')) {
      const queryStart = LocalDate.parseIso(query.get('start') ?? '')
      setFilters(filters.withStartDate(queryStart))
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (unitInformation.isSuccess) {
      setTitle(unitInformation.value.daycare.name)
    }
  }, [setTitle, unitInformation])

  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({})

  const history = useHistory()
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
  ): string => {
    return Object.keys(openGroups)
      .reduce(
        (prev: string[], cur: string) =>
          openGroups[cur] == true ? prev.concat(cur) : prev,
        []
      )
      .join(',')
  }

  useEffect(() => {
    const openList = openGroupsToStringList(openGroups)
    openList.length > 0
      ? history.replace(`?open_groups=${openList}`)
      : history.replace('?')
  }, [openGroups, history])

  const tabs = useMemo(
    () => [
      {
        id: 'unit-info',
        link: `/units/${id}/unit-info`,
        label: i18n.unit.tabs.unitInfo
      },
      {
        id: 'groups',
        link: `/units/${id}/groups`,
        label: i18n.unit.tabs.groups,
        counter: unitData
          .map((data) => data.missingGroupPlacements.length)
          .getOrElse(undefined)
      },
      {
        id: 'calendar',
        link: `/units/${id}/calendar`,
        label: i18n.unit.tabs.calendar
      },
      ...(unitInformation.isSuccess &&
      unitInformation.value.permittedActions.has('READ_DETAILED')
        ? [
            {
              id: 'application-process',
              link: `/units/${id}/application-process`,
              label: i18n.unit.tabs.applicationProcess,
              counter: unitData
                .map(
                  (data) =>
                    (data.placementPlans?.filter((p) => !p.rejectedByCitizen)
                      ?.length ?? 0) + (data.placementProposals?.length ?? 0)
                )
                .getOrElse(0)
            }
          ]
        : [])
    ],
    [id, i18n, unitData, unitInformation]
  )

  const RedirectToUnitInfo = useCallback(
    () => <Redirect to={`/units/${id}/unit-info`} />,
    [id]
  )

  return (
    <>
      {unitInformation.isSuccess && <Tabs tabs={tabs} />}
      <Gap size="s" />
      <Container>
        <Switch>
          <Route
            exact
            path="/units/:id/unit-info"
            render={() => <TabUnitInformation />}
          />
          <Route
            exact
            path="/units/:id/groups"
            render={() => (
              <TabGroups
                reloadUnitData={reloadUnitData}
                openGroups={openGroups}
                setOpenGroups={setOpenGroups}
              />
            )}
          />
          <Route
            exact
            path="/units/:id/calendar"
            render={() => <TabCalendar />}
          />
          <Route
            exact
            path="/units/:id/application-process"
            render={() => (
              <TabApplicationProcess
                isLoading={isLoading(unitData)}
                reloadUnitData={reloadUnitData}
              />
            )}
          />
          <Route path="/" render={() => <RedirectToUnitInfo />} />
        </Switch>
      </Container>
    </>
  )
})

export default React.memo(function UnitPageWrapper() {
  const { id } = useParams<{ id: UUID }>()
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
