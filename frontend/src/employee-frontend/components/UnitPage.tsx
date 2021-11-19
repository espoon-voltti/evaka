// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import { Redirect, Route, Switch, useParams } from 'react-router-dom'
import { useTranslation } from '../state/i18n'
import { RouteWithTitle } from './RouteWithTitle'
import { Gap } from 'lib-components/white-space'
import Tabs from 'lib-components/molecules/Tabs'
import TabUnitInformation from '../components/unit/TabUnitInformation'
import TabGroups from '../components/unit/TabGroups'
import { TitleContext, TitleState } from '../state/title'
import Container from 'lib-components/layout/Container'
import { UnitContext, UnitContextProvider } from '../state/unit'
import TabPlacementProposals from '../components/unit/TabPlacementProposals'
import TabWaitingConfirmation from '../components/unit/TabWaitingConfirmation'
import TabApplications from '../components/unit/TabApplications'
import { useQuery } from 'lib-common/utils/useQuery'
import LocalDate from 'lib-common/local-date'
import TabCalendar from './unit/TabCalendar'
import { UUID } from 'lib-common/types'

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
              id: 'waiting-confirmation',
              link: `/units/${id}/waiting-confirmation`,
              label: i18n.unit.tabs.waitingConfirmation
            },
            {
              id: 'placement-proposals',
              link: `/units/${id}/placement-proposals`,
              label: i18n.unit.tabs.placementProposals,
              counter: unitData
                .map((data) => data.placementProposals?.length)
                .getOrElse(undefined)
            },
            {
              id: 'applications',
              link: `/units/${id}/applications`,
              label: i18n.unit.tabs.applications
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
      <Gap size="s" />
      {unitInformation.isSuccess && <Tabs tabs={tabs} />}
      <Gap size="s" />
      <Container>
        <Switch>
          <RouteWithTitle
            exact
            path="/units/:id/unit-info"
            component={TabUnitInformation}
          />
          <RouteWithTitle
            exact
            path="/units/:id/groups"
            component={() => (
              <TabGroups
                reloadUnitData={reloadUnitData}
                openGroups={openGroups}
                setOpenGroups={setOpenGroups}
              />
            )}
          />
          <RouteWithTitle
            exact
            path="/units/:id/calendar"
            component={TabCalendar}
          />
          <RouteWithTitle
            exact
            path="/units/:id/waiting-confirmation"
            component={TabWaitingConfirmation}
          />
          <RouteWithTitle
            exact
            path="/units/:id/placement-proposals"
            component={() => (
              <TabPlacementProposals reloadUnitData={reloadUnitData} />
            )}
          />
          <RouteWithTitle
            exact
            path="/units/:id/applications"
            component={TabApplications}
          />
          <Route path="/" component={RedirectToUnitInfo} />
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
